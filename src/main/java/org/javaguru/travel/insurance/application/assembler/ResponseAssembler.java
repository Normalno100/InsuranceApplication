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

import java.util.List;
import java.util.stream.Collectors;

/**
 * Assembler для сборки Response DTO.
 *
 * РЕФАКТОРИНГ (п. 4.2 плана):
 *   Метод buildFormula() вынесен в FormulaBuilder.
 *   ResponseAssembler делегирует построение формулы через FormulaBuilder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseAssembler {

    private final CountryRepository countryRepository;
    private final FormulaBuilder formulaBuilder;

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

        builder.pricing(TravelCalculatePremiumResponse.PricingSummary.builder()
                .totalPremium(discountResult.finalPremium())
                .baseAmount(discountResult.basePremium())
                .totalDiscount(discountResult.totalDiscount())
                .currency(currency)
                .includedRisks(request.getSelectedRisks() != null
                        ? request.getSelectedRisks()
                        : List.of())
                .build());

        builder.person(buildPersonSummary(request, details));

        builder.trip(buildTripSummary(request, details));

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

        builder.underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                .decision(underwritingResult.getDecision().name())
                .evaluatedRules(buildRuleEvaluations(underwritingResult))
                .build());

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
     * Делегирует построение формулы в FormulaBuilder.
     */
    private TravelCalculatePremiumResponse.PricingDetails buildPricingDetails(
            MedicalRiskPremiumCalculator.PremiumCalculationResult details) {

        if (details == null) {
            log.warn("buildPricingDetails: details is null, returning empty PricingDetails");
            return TravelCalculatePremiumResponse.PricingDetails.builder().build();
        }

        boolean isCountryDefault = details.calculationMode()
                == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        var builder = TravelCalculatePremiumResponse.PricingDetails.builder()
                .baseRate(details.baseRate())
                .ageCoefficient(details.ageCoefficient())
                .countryCoefficient(details.countryCoefficient())
                .durationCoefficient(details.durationCoefficient())
                .countryDefaultDayPremium(isCountryDefault ? details.countryDefaultDayPremium() : null)
                // РЕФАКТОРИНГ (п. 4.2): делегируем построение формулы в FormulaBuilder
                .calculationFormula(formulaBuilder.build(details));

        builder.countryInfo(buildCountryInfo(details));

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

    private String resolveCurrency(TravelCalculatePremiumRequest request) {
        return request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                ? request.getCurrency()
                : "EUR";
    }
}