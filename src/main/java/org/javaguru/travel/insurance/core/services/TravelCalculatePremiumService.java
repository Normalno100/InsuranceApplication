package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.validation.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.dto.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse.*;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumService {

    private final TravelCalculatePremiumRequestValidator validator;
    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final PromoCodeService promoCodeService;
    private final DiscountService discountService;
    private final UnderwritingService underwritingService;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    /**
     * Расчет премии с полными деталями
     */
    public TravelCalculatePremiumResponse calculatePremium(
            TravelCalculatePremiumRequest request) {
        return calculatePremium(request, true);
    }

    /**
     * Расчет премии с опциональными деталями
     *
     * @param request запрос на расчет
     * @param includeDetails включать ли детальную разбивку расчета
     */
    public TravelCalculatePremiumResponse calculatePremium(
            TravelCalculatePremiumRequest request,
            boolean includeDetails) {

        var responseBuilder = TravelCalculatePremiumResponse.builder();

        // 1. Валидация
        List<org.javaguru.travel.insurance.core.validation.ValidationError> validationErrors =
                validator.validate(request);

        if (!validationErrors.isEmpty()) {
            return buildValidationErrorResponse(validationErrors);
        }

        // 2. Андеррайтинг
        UnderwritingResult underwritingResult = underwritingService.evaluateApplication(request);

        // 2a. Если заявка отклонена
        if (underwritingResult.isDeclined()) {
            return buildDeclinedResponse(request, underwritingResult);
        }

        // 2b. Если требуется ручная проверка
        if (underwritingResult.requiresManualReview()) {
            return buildReviewRequiredResponse(request, underwritingResult);
        }

        // 3. Расчет премии (только если одобрено)
        try {
            var calculationResult = medicalRiskCalculator.calculatePremiumWithDetails(request);
            BigDecimal basePremium = calculationResult.premium();

            // 4. Применение промо-кода и скидок
            List<AppliedDiscount> appliedDiscounts = new ArrayList<>();
            BigDecimal totalDiscount = BigDecimal.ZERO;

            // Промо-код
            if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
                var promoResult = promoCodeService.applyPromoCode(
                        request.getPromoCode(),
                        request.getAgreementDateFrom(),
                        basePremium
                );

                if (promoResult.isValid()) {
                    totalDiscount = totalDiscount.add(promoResult.actualDiscountAmount());
                    appliedDiscounts.add(AppliedDiscount.builder()
                            .type("PROMO_CODE")
                            .code(promoResult.code())
                            .description(promoResult.description())
                            .amount(promoResult.actualDiscountAmount())
                            .percentage(promoResult.discountValue())
                            .build());
                }
            }

            // Другие скидки (групповые, корпоративные)
            int personsCount = request.getPersonsCount() != null && request.getPersonsCount() > 0
                    ? request.getPersonsCount() : 1;
            boolean isCorporate = Boolean.TRUE.equals(request.getIsCorporate());

            var bestDiscount = discountService.calculateBestDiscount(
                    basePremium,
                    personsCount,
                    isCorporate,
                    request.getAgreementDateFrom()
            );

            if (bestDiscount.isPresent()) {
                var discount = bestDiscount.get();
                totalDiscount = totalDiscount.add(discount.amount());
                appliedDiscounts.add(AppliedDiscount.builder()
                        .type(discount.discountType().name())
                        .code(discount.code())
                        .description(discount.name())
                        .amount(discount.amount())
                        .percentage(discount.percentage())
                        .build());
            }

            // Пакетная скидка
            var bundleDiscount = calculationResult.bundleDiscount();
            if (bundleDiscount != null && bundleDiscount.bundle() != null) {
                var bundle = bundleDiscount.bundle();
                appliedDiscounts.add(AppliedDiscount.builder()
                        .type("BUNDLE")
                        .code(bundle.code())
                        .description(bundle.name())
                        .amount(bundleDiscount.discountAmount())
                        .percentage(bundle.discountPercentage())
                        .build());
            }

            BigDecimal finalPremium = applyMinimumPremium(
                    basePremium.subtract(totalDiscount)
            );

            String currency = request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                    ? request.getCurrency() : "EUR";

            // 5. Строим успешный ответ
            return buildSuccessResponse(
                    request,
                    calculationResult,
                    basePremium,
                    totalDiscount,
                    finalPremium,
                    currency,
                    appliedDiscounts,
                    underwritingResult,
                    includeDetails
            );

        } catch (Exception e) {
            return buildSystemErrorResponse(e.getMessage());
        }
    }

    /**
     * Строит ответ с ошибками валидации
     */
    private TravelCalculatePremiumResponse buildValidationErrorResponse(
            List<org.javaguru.travel.insurance.core.validation.ValidationError> errors) {

        var validationErrors = errors.stream()
                .map(e -> TravelCalculatePremiumResponse.ValidationError.builder()
                        .field(e.getField())
                        .message(e.getMessage())
                        .build())
                .collect(Collectors.toList());

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(validationErrors)
                .build();
    }

    /**
     * Строит ответ для отклоненной заявки
     */
    private TravelCalculatePremiumResponse buildDeclinedResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.DECLINED)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(ValidationError.builder()
                        .field("underwriting")
                        .message("Application declined: " + underwritingResult.getDeclineReason())
                        .build()))
                .build();
    }

    /**
     * Строит ответ для заявки, требующей проверки
     */
    private TravelCalculatePremiumResponse buildReviewRequiredResponse(
            TravelCalculatePremiumRequest request,
            UnderwritingResult underwritingResult) {

        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .person(buildPersonSummary(request, null))
                .underwriting(UnderwritingInfo.builder()
                        .decision(underwritingResult.getDecision().name())
                        .reason(underwritingResult.getDeclineReason())
                        .evaluatedRules(buildRuleEvaluations(underwritingResult))
                        .build())
                .errors(List.of(TravelCalculatePremiumResponse.ValidationError.builder()
                        .field("underwriting")
                        .message("Manual review required: " + underwritingResult.getDeclineReason())
                        .build()))
                .build();
    }

    /**
     * Строит успешный ответ
     */
    private TravelCalculatePremiumResponse buildSuccessResponse(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult,
            BigDecimal basePremium,
            BigDecimal totalDiscount,
            BigDecimal finalPremium,
            String currency,
            List<AppliedDiscount> appliedDiscounts,
            UnderwritingResult underwritingResult,
            boolean includeDetails) {

        var builder = TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.SUCCESS)
                .success(true)
                .errors(List.of());

        // Pricing Summary
        builder.pricing(PricingSummary.builder()
                .totalPremium(finalPremium)
                .baseAmount(basePremium)
                .totalDiscount(totalDiscount)
                .currency(currency)
                .includedRisks(request.getSelectedRisks() != null
                        ? request.getSelectedRisks()
                        : List.of())
                .build());

        // Person Summary
        builder.person(buildPersonSummary(request, calculationResult));

        // Trip Summary
        builder.trip(buildTripSummary(request, calculationResult));

        // Applied Discounts
        if (!appliedDiscounts.isEmpty()) {
            builder.appliedDiscounts(appliedDiscounts);
        }

        // Underwriting Info
        builder.underwriting(UnderwritingInfo.builder()
                .decision(underwritingResult.getDecision().name())
                .evaluatedRules(buildRuleEvaluations(underwritingResult))
                .build());

        // Pricing Details (опционально)
        if (includeDetails) {
            builder.pricingDetails(buildPricingDetails(calculationResult));
        }

        return builder.build();
    }

    /**
     * Строит ответ с системной ошибкой
     */
    private TravelCalculatePremiumResponse buildSystemErrorResponse(String errorMessage) {
        return TravelCalculatePremiumResponse.builder()
                .status(ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(ValidationError.builder()
                        .field("system")
                        .message("Calculation error: " + errorMessage)
                        .build()))
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

    /**
     * Применяет минимальную премию
     */
    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0) {
            return MIN_PREMIUM;
        }
        return premium;
    }
}