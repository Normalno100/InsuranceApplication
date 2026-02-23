package org.javaguru.travel.insurance.application.assembler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.service.DiscountApplicationService;
import org.javaguru.travel.insurance.application.service.PremiumCalculationService;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Assembler для сборки Response DTO.
 *
 * ИЗМЕНЕНИЯ v2.1 (этап 4–5):
 * - buildSuccessResponse теперь заполняет CountryInfo в PricingDetails
 * - TripSummary дополнен полями countryDefaultDayPremium и calculationMode
 * - buildPricingDetails учитывает режим расчёта (MEDICAL_LEVEL / COUNTRY_DEFAULT)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseAssembler {

    private final CountryRepository countryRepository;

    // ========================================
    // ПУБЛИЧНЫЕ МЕТОДЫ
    // ========================================

    public TravelCalculatePremiumResponse buildValidationErrorResponse(
            List<ValidationError> errors) {

        var validationErrors = errors.stream()
                .map(e -> {
                    var error = new TravelCalculatePremiumResponse.ValidationError();
                    error.setField(e.getField());
                    error.setMessage(e.getMessage());
                    error.setCode(e.getErrorCode());
                    return error;
                })
                .collect(Collectors.toList());

        return TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(validationErrors)
                .build();
    }

    public TravelCalculatePremiumResponse buildDeclinedResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("underwriting");
        error.setMessage("Application declined: " + underwritingResult.getDeclineReason());

        return TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.DECLINED)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(error))
                .build();
    }

    public TravelCalculatePremiumResponse buildReviewRequiredResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("underwriting");
        error.setMessage("Manual review required: " + underwritingResult.getDeclineReason());

        return TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(error))
                .build();
    }

    /**
     * Собирает успешный ответ.
     *
     * ИЗМЕНЕНИЯ v2.1: передаёт информацию о режиме расчёта и дефолтной ставке страны.
     */
    public TravelCalculatePremiumResponse buildSuccessResponse(
            TravelCalculatePremiumRequest request,
            PremiumCalculationService.PremiumCalculationResult calculationResult,
            DiscountApplicationService.DiscountApplicationResult discountResult,
            UnderwritingResult underwritingResult,
            boolean includeDetails) {

        String currency = resolveCurrency(request);
        MedicalRiskPremiumCalculator.PremiumCalculationResult details = calculationResult.details();

        var builder = TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.SUCCESS)
                .success(true)
                .errors(List.of());

        // Pricing Summary
        builder.pricing(TravelCalculatePremiumResponse.PricingSummary.builder()
                .totalPremium(discountResult.finalPremium())
                .baseAmount(discountResult.basePremium())
                .totalDiscount(discountResult.totalDiscount())
                .currency(currency)
                .includedRisks(request.getSelectedRisks() != null
                        ? request.getSelectedRisks()
                        : List.of())
                .build());

        // Person Summary
        builder.person(buildPersonSummary(request, details));

        // Trip Summary — теперь с режимом расчёта и дефолтной ставкой
        builder.trip(buildTripSummary(request, details));

        // Applied Discounts
        if (!discountResult.appliedDiscounts().isEmpty()) {
            var appliedDiscounts = discountResult.appliedDiscounts().stream()
                    .map(d -> TravelCalculatePremiumResponse.AppliedDiscount.builder()
                            .type(d.type())
                            .code(d.code())
                            .description(d.description())
                            .amount(d.amount())
                            .percentage(d.percentage())
                            .build())
                    .collect(Collectors.toList());
            builder.appliedDiscounts(appliedDiscounts);
        }

        // Underwriting Info
        builder.underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                .decision(underwritingResult.getDecision().name())
                .evaluatedRules(buildRuleEvaluations(underwritingResult))
                .build());

        // Pricing Details (если запрошено)
        if (includeDetails) {
            builder.pricingDetails(buildPricingDetails(details));
        }

        return builder.build();
    }

    public TravelCalculatePremiumResponse buildSystemErrorResponse(String errorMessage) {
        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("system");
        error.setMessage("Calculation error: " + errorMessage);

        return TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(error))
                .build();
    }

    // ========================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    private TravelCalculatePremiumResponse.PersonSummary buildPersonSummary(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult details) {

        Integer age = details != null ? details.age() : null;
        String ageGroup = details != null ? details.ageGroupDescription() : null;

        return TravelCalculatePremiumResponse.PersonSummary.builder()
                .firstName(request.getPersonFirstName())
                .lastName(request.getPersonLastName())
                .birthDate(request.getPersonBirthDate())
                .age(age)
                .ageGroup(ageGroup)
                .build();
    }

    /**
     * Строит TripSummary с поддержкой двух режимов расчёта.
     *
     * ИЗМЕНЕНИЯ v2.1:
     * - calculationMode отражает, какой режим был использован
     * - countryDefaultDayPremium заполняется в COUNTRY_DEFAULT режиме
     * - medicalCoverageLevel и coverageAmount — в MEDICAL_LEVEL режиме
     */
    private TravelCalculatePremiumResponse.TripSummary buildTripSummary(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult details) {

        boolean isCountryDefaultMode = details != null
                && details.calculationMode() == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        return TravelCalculatePremiumResponse.TripSummary.builder()
                .dateFrom(request.getAgreementDateFrom())
                .dateTo(request.getAgreementDateTo())
                .days(details != null ? details.days() : null)
                .countryCode(request.getCountryIsoCode())
                .countryName(details != null ? details.countryName() : null)
                // Поля зависят от режима расчёта
                .medicalCoverageLevel(isCountryDefaultMode ? null : request.getMedicalRiskLimitLevel())
                .coverageAmount(isCountryDefaultMode ? null
                        : (details != null ? details.coverageAmount() : null))
                .countryDefaultDayPremium(isCountryDefaultMode && details != null
                        ? details.countryDefaultDayPremium()
                        : null)
                .calculationMode(details != null ? details.calculationMode().name() : null)
                .build();
    }

    /**
     * Строит PricingDetails с поддержкой двух режимов.
     *
     * ИЗМЕНЕНИЯ v2.1:
     * - countryDefaultDayPremium заполняется в COUNTRY_DEFAULT режиме
     * - countryInfo содержит информацию о стране (в обоих режимах)
     */
    private TravelCalculatePremiumResponse.PricingDetails buildPricingDetails(
            MedicalRiskPremiumCalculator.PremiumCalculationResult details) {

        boolean isCountryDefault = details.calculationMode()
                == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        var builder = TravelCalculatePremiumResponse.PricingDetails.builder()
                .baseRate(details.baseRate())
                .ageCoefficient(details.ageCoefficient())
                .countryCoefficient(details.countryCoefficient())
                .durationCoefficient(details.durationCoefficient())
                .countryDefaultDayPremium(isCountryDefault ? details.countryDefaultDayPremium() : null)
                .calculationFormula(buildFormula(details));

        // CountryInfo — всегда присутствует в деталях при успешном ответе
        builder.countryInfo(buildCountryInfo(details));

        // Risk Breakdown
        var riskBreakdown = details.riskDetails().stream()
                .map(r -> TravelCalculatePremiumResponse.RiskBreakdown.builder()
                        .riskCode(r.riskCode())
                        .riskName(r.riskName())
                        .premium(r.premium())
                        .baseCoefficient(r.coefficient())
                        .ageModifier(r.ageModifier())
                        .isMandatory("TRAVEL_MEDICAL".equals(r.riskCode()))
                        .build())
                .collect(Collectors.toList());
        builder.riskBreakdown(riskBreakdown);

        // Calculation Steps
        var steps = details.calculationSteps().stream()
                .map(s -> {
                    var step = new TravelCalculatePremiumResponse.CalculationStep();
                    step.setStepNumber(details.calculationSteps().indexOf(s) + 1);
                    step.setDescription(s.description());
                    step.setFormula(s.formula());
                    step.setResult(s.result());
                    return step;
                })
                .collect(Collectors.toList());
        builder.steps(steps);

        return builder.build();
    }

    /**
     * Строит CountryInfo — детальная информация о стране.
     */
    private TravelCalculatePremiumResponse.CountryInfo buildCountryInfo(
            MedicalRiskPremiumCalculator.PremiumCalculationResult details) {

        boolean hasDefaultPremium = details.countryDefaultDayPremiumForInfo() != null;

        return TravelCalculatePremiumResponse.CountryInfo.builder()
                .name(details.countryName())
                .riskCoefficient(details.countryCoefficient())
                .defaultDayPremium(details.countryDefaultDayPremiumForInfo())
                .defaultDayPremiumCurrency(details.countryDefaultCurrency())
                .hasDefaultDayPremium(hasDefaultPremium)
                .build();
    }

    private List<TravelCalculatePremiumResponse.RuleEvaluation> buildRuleEvaluations(
            UnderwritingResult underwritingResult) {

        return underwritingResult.getRuleResults().stream()
                .map(r -> new TravelCalculatePremiumResponse.RuleEvaluation(
                        r.getRuleName(),
                        r.getSeverity().name(),
                        r.getMessage()))
                .collect(Collectors.toList());
    }

    /**
     * Строит формулу расчёта в зависимости от режима.
     */
    private String buildFormula(MedicalRiskPremiumCalculator.PremiumCalculationResult result) {
        boolean isCountryDefault = result.calculationMode()
                == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        var formula = new StringBuilder("Premium = ")
                .append(String.format("%.2f", result.baseRate()));

        if (isCountryDefault) {
            formula.append(" (country default rate)");
        } else {
            formula.append(" (daily rate)");
        }

        formula.append(" × ").append(String.format("%.4f", result.ageCoefficient())).append(" (age)");

        // Коэффициент страны применяется только в MEDICAL_LEVEL
        if (!isCountryDefault) {
            formula.append(" × ").append(String.format("%.4f", result.countryCoefficient()))
                    .append(" (country)");
        }

        formula.append(" × ").append(String.format("%.4f", result.durationCoefficient()))
                .append(" (duration)");

        if (result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.4f", result.additionalRisksCoefficient()))
                    .append(") (risks)");
        }

        formula.append(" × ").append(result.days()).append(" days");

        if (result.bundleDiscount() != null
                && result.bundleDiscount().discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" - ")
                    .append(String.format("%.2f", result.bundleDiscount().discountAmount()))
                    .append(" (bundle discount)");
        }

        return formula.toString();
    }

    private String resolveCurrency(TravelCalculatePremiumRequest request) {
        return request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                ? request.getCurrency()
                : "EUR";
    }
}