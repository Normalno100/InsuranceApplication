package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.assembler.ResponseAssembler;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.v3.*;
import org.javaguru.travel.insurance.application.validation.TravelCalculatePremiumRequestValidatorV3;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Сервис расчёта страховой премии для V3 API (multi-person).
 *
 * task_135: Создан для нового REST-эндпоинта /insurance/travel/v3/calculate.
 *
 * АРХИТЕКТУРА:
 *   Оркестрирует полный цикл обработки V3 запроса:
 *   1. Валидация запроса (TravelCalculatePremiumRequestValidatorV3)
 *   2. Расчёт групповой премии (PremiumCalculationService.calculateForGroup)
 *   3. Применение скидок (DiscountApplicationService)
 *   4. Сборка ответа V3 (TravelCalculatePremiumResponseV3)
 *
 * ОБРАТНАЯ СОВМЕСТИМОСТЬ:
 *   V2 API (TravelCalculatePremiumService) не изменяется.
 *   Оба сервиса используют общий сервисный слой через разные адаптеры.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumServiceV3 {

    private final TravelCalculatePremiumRequestValidatorV3 validator;
    private final PremiumCalculationService premiumCalculationService;
    private final DiscountApplicationService discountApplicationService;

    /**
     * Рассчитывает страховую премию для группы застрахованных (V3 API).
     *
     * @param request V3 запрос с несколькими персонами
     * @return V3 ответ с индивидуальными премиями по каждой персоне
     */
    public TravelCalculatePremiumResponseV3 calculatePremium(TravelCalculatePremiumRequestV3 request) {
        log.info("V3 premium calculation for {} persons, country: {}",
                request.getPersons() != null ? request.getPersons().size() : 0,
                request.getCountryIsoCode());

        // ── 1. Валидация ──────────────────────────────────────────────────
        List<ValidationError> validationErrors = validator.validate(request);
        if (!validationErrors.isEmpty()) {
            log.warn("V3 validation failed: {} errors", validationErrors.size());
            return buildValidationErrorResponse(validationErrors);
        }

        // ── 2. Расчёт групповой премии ────────────────────────────────────
        GroupPremiumResult groupResult;
        try {
            groupResult = premiumCalculationService.calculateForGroup(request);
        } catch (Exception e) {
            log.error("V3 premium calculation failed", e);
            return buildSystemErrorResponse(e.getMessage());
        }

        // ── 3. Проверка решения андеррайтинга ─────────────────────────────
        if (groupResult.isDeclined()) {
            log.warn("V3 application declined: {}", groupResult.groupUnderwriting().getDeclineReason());
            return buildDeclinedResponse(groupResult);
        }

        if (groupResult.requiresReview()) {
            log.info("V3 manual review required: {}", groupResult.groupUnderwriting().getDeclineReason());
            return buildReviewRequiredResponse(groupResult);
        }

        // ── 4. Применение скидок ──────────────────────────────────────────
        try {
            // Адаптируем V3 запрос в V2 для DiscountApplicationService
            TravelCalculatePremiumRequest discountRequest = adaptForDiscount(request);
            DiscountApplicationService.DiscountApplicationResult discountResult =
                    discountApplicationService.applyDiscounts(discountRequest, groupResult.totalPremium());

            // ── 5. Сборка успешного ответа ────────────────────────────────
            return buildSuccessResponse(request, groupResult, discountResult);

        } catch (Exception e) {
            log.error("V3 discount application failed", e);
            return buildSystemErrorResponse(e.getMessage());
        }
    }

    // ── Методы сборки ответов ─────────────────────────────────────────────────

    private TravelCalculatePremiumResponseV3 buildValidationErrorResponse(
            List<ValidationError> errors) {

        List<TravelCalculatePremiumResponseV3.ValidationError> responseErrors = errors.stream()
                .map(e -> TravelCalculatePremiumResponseV3.ValidationError.builder()
                        .field(e.getField())
                        .message(e.getMessage())
                        .code(e.getErrorCode())
                        .build())
                .collect(Collectors.toList());

        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(responseErrors)
                .build();
    }

    private TravelCalculatePremiumResponseV3 buildDeclinedResponse(GroupPremiumResult groupResult) {
        var error = TravelCalculatePremiumResponseV3.ValidationError.builder()
                .field("underwriting")
                .message("Application declined: " + groupResult.groupUnderwriting().getDeclineReason())
                .build();

        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.DECLINED)
                .success(false)
                .underwriting(buildUnderwritingInfo(groupResult))
                .errors(List.of(error))
                .build();
    }

    private TravelCalculatePremiumResponseV3 buildReviewRequiredResponse(GroupPremiumResult groupResult) {
        var error = TravelCalculatePremiumResponseV3.ValidationError.builder()
                .field("underwriting")
                .message("Manual review required: " + groupResult.groupUnderwriting().getDeclineReason())
                .build();

        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .underwriting(buildUnderwritingInfo(groupResult))
                .errors(List.of(error))
                .build();
    }

    private TravelCalculatePremiumResponseV3 buildSuccessResponse(
            TravelCalculatePremiumRequestV3 request,
            GroupPremiumResult groupResult,
            DiscountApplicationService.DiscountApplicationResult discountResult) {

        String currency = resolveCurrency(request);

        // Сборка PricingSummaryV3 с totalPersonsPremium
        PricingSummaryV3 pricing = PricingSummaryV3.builder()
                .totalPremium(discountResult.finalPremium())
                .totalPersonsPremium(groupResult.totalPremium())
                .baseAmount(groupResult.totalPremium())
                .totalDiscount(discountResult.totalDiscount())
                .currency(currency)
                .includedRisks(request.getSelectedRisks() != null
                        ? request.getSelectedRisks()
                        : List.of())
                .build();

        // Сборка TripSummary
        TravelCalculatePremiumResponseV3.TripSummary trip = buildTripSummary(request, groupResult);

        // Сборка применённых скидок
        List<TravelCalculatePremiumResponseV3.AppliedDiscount> appliedDiscounts =
                discountResult.appliedDiscounts().stream()
                        .map(d -> TravelCalculatePremiumResponseV3.AppliedDiscount.builder()
                                .type(d.type())
                                .code(d.code())
                                .description(d.description())
                                .amount(d.amount())
                                .percentage(d.percentage())
                                .build())
                        .collect(Collectors.toList());

        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                .success(true)
                .errors(List.of())
                .pricing(pricing)
                .personPremiums(groupResult.personPremiums())
                .trip(trip)
                .appliedDiscounts(appliedDiscounts.isEmpty() ? null : appliedDiscounts)
                .underwriting(buildUnderwritingInfo(groupResult))
                .build();
    }

    private TravelCalculatePremiumResponseV3 buildSystemErrorResponse(String errorMessage) {
        var error = TravelCalculatePremiumResponseV3.ValidationError.builder()
                .field("system")
                .message("Calculation error: " + errorMessage)
                .build();

        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(error))
                .build();
    }

    private TravelCalculatePremiumResponseV3.UnderwritingInfo buildUnderwritingInfo(
            GroupPremiumResult groupResult) {

        List<TravelCalculatePremiumResponseV3.RuleEvaluation> evaluatedRules =
                groupResult.groupUnderwriting().getRuleResults().stream()
                        .map(r -> new TravelCalculatePremiumResponseV3.RuleEvaluation(
                                r.getRuleName(),
                                r.getSeverity().name(),
                                r.getMessage()))
                        .collect(Collectors.toList());

        return TravelCalculatePremiumResponseV3.UnderwritingInfo.builder()
                .decision(groupResult.groupUnderwriting().getDecision().name())
                .reason(groupResult.groupUnderwriting().getDeclineReason())
                .evaluatedRules(evaluatedRules)
                .build();
    }

    private TravelCalculatePremiumResponseV3.TripSummary buildTripSummary(
            TravelCalculatePremiumRequestV3 request,
            GroupPremiumResult groupResult) {

        var firstPersonDetails = groupResult.firstPersonDetails();

        return TravelCalculatePremiumResponseV3.TripSummary.builder()
                .dateFrom(request.getAgreementDateFrom())
                .dateTo(request.getAgreementDateTo())
                .days(firstPersonDetails != null ? firstPersonDetails.tripDetails().days() : null)
                .countryCode(request.getCountryIsoCode())
                .countryName(firstPersonDetails != null
                        ? firstPersonDetails.countryDetails().countryName() : null)
                .medicalCoverageLevel(request.getMedicalRiskLimitLevel())
                .coverageAmount(firstPersonDetails != null
                        ? firstPersonDetails.tripDetails().coverageAmount() : null)
                .medicalPayoutLimit(firstPersonDetails != null
                        ? firstPersonDetails.payoutLimitDetails().medicalPayoutLimit() : null)
                .calculationMode(firstPersonDetails != null
                        ? firstPersonDetails.calculationMode().name() : null)
                .build();
    }

    /**
     * Адаптирует V3 запрос в V2 для передачи в DiscountApplicationService.
     * DiscountApplicationService ожидает TravelCalculatePremiumRequest с promoCode,
     * personsCount и isCorporate.
     */
    private TravelCalculatePremiumRequest adaptForDiscount(TravelCalculatePremiumRequestV3 request) {
        // Для группового расчёта используем persons.size() как personsCount если не задан явно
        int personsCount = (request.getPersonsCount() != null && request.getPersonsCount() > 0)
                ? request.getPersonsCount()
                : (request.getPersons() != null ? request.getPersons().size() : 1);

        return TravelCalculatePremiumRequest.builder()
                .promoCode(request.getPromoCode())
                .personsCount(personsCount)
                .isCorporate(request.getIsCorporate())
                .agreementDateFrom(request.getAgreementDateFrom())
                .currency(request.getCurrency())
                .build();
    }

    private String resolveCurrency(TravelCalculatePremiumRequestV3 request) {
        return (request.getCurrency() != null && !request.getCurrency().trim().isEmpty())
                ? request.getCurrency()
                : "EUR";
    }
}