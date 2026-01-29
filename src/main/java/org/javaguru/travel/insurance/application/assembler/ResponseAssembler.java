package org.javaguru.travel.insurance.application.assembler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.service.DiscountApplicationService;
import org.javaguru.travel.insurance.application.service.PremiumCalculationService;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.validation.ValidationError;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Assembler для сборки Response DTO
 *
 * ЦЕЛЬ: Централизованная сборка всех типов ответов
 *
 * ОБЯЗАННОСТИ:
 * 1. Сборка успешных ответов
 * 2. Сборка ответов с ошибками валидации
 * 3. Сборка ответов об отклонении
 * 4. Сборка ответов о необходимости проверки
 * 5. Маппинг domain → DTO
 *
 * МЕТРИКИ:
 * - Complexity: 2
 * - LOC: ~270
 * - Single Responsibility: только сборка ответов
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResponseAssembler {

    /**
     * Собирает ответ с ошибками валидации
     */
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
                .status(ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(validationErrors)
                .build();
    }

    /**
     * Собирает ответ для отклоненной заявки
     */
    public TravelCalculatePremiumResponse buildDeclinedResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("underwriting");
        error.setMessage("Application declined: " + underwritingResult.getDeclineReason());

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.DECLINED)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(error))
                .build();
    }

    /**
     * Собирает ответ для заявки, требующей проверки
     */
    public TravelCalculatePremiumResponse buildReviewRequiredResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("underwriting");
        error.setMessage("Manual review required: " + underwritingResult.getDeclineReason());

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(error))
                .build();
    }

    /**
     * Собирает успешный ответ
     */
    public TravelCalculatePremiumResponse buildSuccessResponse(
            TravelCalculatePremiumRequest request,
            PremiumCalculationService.PremiumCalculationResult calculationResult,
            DiscountApplicationService.DiscountApplicationResult discountResult,
            UnderwritingResult underwritingResult,
            boolean includeDetails) {

        String currency = request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                ? request.getCurrency() : "EUR";

        var builder = TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.SUCCESS)
                .success(true)
                .errors(List.of());

        // Pricing Summary
        builder.pricing(PricingSummary.builder()
                .totalPremium(discountResult.finalPremium())
                .baseAmount(discountResult.basePremium())
                .totalDiscount(discountResult.totalDiscount())
                .currency(currency)
                .includedRisks(request.getSelectedRisks() != null
                        ? request.getSelectedRisks()
                        : List.of())
                .build());

        // Person Summary
        builder.person(buildPersonSummary(request, calculationResult.details()));

        // Trip Summary
        builder.trip(buildTripSummary(request, calculationResult.details()));

        // Applied Discounts
        if (!discountResult.appliedDiscounts().isEmpty()) {
            var appliedDiscounts = discountResult.appliedDiscounts().stream()
                    .map(d -> AppliedDiscount.builder()
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
        builder.underwriting(UnderwritingInfo.builder()
                .decision(underwritingResult.getDecision().name())
                .evaluatedRules(buildRuleEvaluations(underwritingResult))
                .build());

        // Pricing Details (если запрошено)
        if (includeDetails) {
            builder.pricingDetails(buildPricingDetails(calculationResult.details()));
        }

        return builder.build();
    }

    /**
     * Собирает ответ с системной ошибкой
     */
    public TravelCalculatePremiumResponse buildSystemErrorResponse(String errorMessage) {
        var error = new TravelCalculatePremiumResponse.ValidationError();
        error.setField("system");
        error.setMessage("Calculation error: " + errorMessage);

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(error))
                .build();
    }

    /**
     * Строит PersonSummary
     */
    private PersonSummary buildPersonSummary(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult) {

        Integer age = calculationResult != null ? calculationResult.age() : null;
        String ageGroup = calculationResult != null ? calculationResult.ageGroupDescription() : null;

        return PersonSummary.builder()
                .firstName(request.getPersonFirstName())
                .lastName(request.getPersonLastName())
                .birthDate(request.getPersonBirthDate())
                .age(age)
                .ageGroup(ageGroup)
                .build();
    }

    /**
     * Строит TripSummary
     */
    private TripSummary buildTripSummary(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult) {

        return TripSummary.builder()
                .dateFrom(request.getAgreementDateFrom())
                .dateTo(request.getAgreementDateTo())
                .days(calculationResult != null ? calculationResult.days() : null)
                .countryCode(request.getCountryIsoCode())
                .countryName(calculationResult != null ? calculationResult.countryName() : null)
                .medicalCoverageLevel(request.getMedicalRiskLimitLevel())
                .coverageAmount(calculationResult != null ? calculationResult.coverageAmount() : null)
                .build();
    }

    /**
     * Строит PricingDetails
     */
    private PricingDetails buildPricingDetails(
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult) {

        var builder = PricingDetails.builder()
                .baseRate(calculationResult.baseRate())
                .ageCoefficient(calculationResult.ageCoefficient())
                .countryCoefficient(calculationResult.countryCoefficient())
                .durationCoefficient(calculationResult.durationCoefficient())
                .calculationFormula(buildFormula(calculationResult));

        // Risk Breakdown
        var riskBreakdown = calculationResult.riskDetails().stream()
                .map(r -> RiskBreakdown.builder()
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
        var steps = calculationResult.calculationSteps().stream()
                .map(s -> {
                    var step = new CalculationStep();
                    step.setStepNumber(calculationResult.calculationSteps().indexOf(s) + 1);
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
     * Строит список оценок правил
     */
    private List<RuleEvaluation> buildRuleEvaluations(UnderwritingResult underwritingResult) {
        return underwritingResult.getRuleResults().stream()
                .map(r -> new RuleEvaluation(
                        r.getRuleName(),
                        r.getSeverity().name(),
                        r.getMessage()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Строит формулу расчета
     */
    private String buildFormula(MedicalRiskPremiumCalculator.PremiumCalculationResult result) {
        var formula = new StringBuilder("Premium = ")
                .append(String.format("%.2f", result.baseRate()))
                .append(" × ")
                .append(String.format("%.2f", result.ageCoefficient()))
                .append(" × ")
                .append(String.format("%.2f", result.countryCoefficient()))
                .append(" × ")
                .append(String.format("%.2f", result.durationCoefficient()));

        if (result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.2f", result.additionalRisksCoefficient()))
                    .append(")");
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
}