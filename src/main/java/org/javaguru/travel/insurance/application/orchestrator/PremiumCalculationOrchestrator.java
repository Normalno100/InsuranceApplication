package org.javaguru.travel.insurance.application.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.service.DiscountApplicationService;
import org.javaguru.travel.insurance.application.service.GroupPremiumResult;
import org.javaguru.travel.insurance.application.service.PremiumCalculationService;
import org.javaguru.travel.insurance.application.service.UnderwritingApplicationService;
import org.javaguru.travel.insurance.application.assembler.ResponseAssembler;
import org.javaguru.travel.insurance.application.validation.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Оркестратор расчета премии.
 *
 * ЦЕЛЬ: Координация процесса расчета без знания деталей реализации.
 *
 * task_134: Оркестратор обновлён для поддержки GroupPremiumResult.
 * V2 API использует MultiPersonPremiumCalculationService через адаптер
 * (одна персона → группа из одной персоны).
 *
 * ОБЯЗАННОСТИ:
 * 1. Валидация запроса
 * 2. Расчёт групповой премии (включая андеррайтинг каждой персоны)
 * 3. Применение скидок к итоговой сумме
 * 4. Сборка финального ответа
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumCalculationOrchestrator {

    private final TravelCalculatePremiumRequestValidator validator;
    private final UnderwritingApplicationService underwritingService;
    private final PremiumCalculationService premiumCalculationService;
    private final DiscountApplicationService discountApplicationService;
    private final ResponseAssembler responseAssembler;

    /**
     * Главный метод оркестрации расчета премии.
     *
     * task_134: Поток изменён — андеррайтинг теперь выполняется внутри
     * GroupPremiumResult (MultiPersonPremiumCalculationService обходит
     * каждую персону и агрегирует наиболее строгое решение).
     *
     * Поток:
     * 1. Validate → 2. Calculate (includes underwriting per person) → 3. Check group decision
     * → 4. Apply Discounts → 5. Assemble Response
     */
    public TravelCalculatePremiumResponse process(TravelCalculatePremiumRequest request, boolean includeDetails) {
        log.info("Starting premium calculation orchestration for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        // STEP 1: Валидация
        List<ValidationError> validationErrors = validator.validate(request);
        if (!validationErrors.isEmpty()) {
            log.warn("Validation failed: {} errors", validationErrors.size());
            return responseAssembler.buildValidationErrorResponse(validationErrors);
        }

        // STEP 2: Расчёт премии с андеррайтингом через GroupPremiumResult
        GroupPremiumResult groupResult;
        try {
            groupResult = premiumCalculationService.calculateSinglePersonAsGroup(request);
        } catch (Exception e) {
            log.error("Premium calculation failed", e);
            return responseAssembler.buildSystemErrorResponse(e.getMessage());
        }

        // STEP 3: Проверка решения андеррайтинга группы
        if (groupResult.isDeclined()) {
            log.warn("Application declined: {}", groupResult.groupUnderwriting().getDeclineReason());
            return responseAssembler.buildDeclinedResponse(request, groupResult.groupUnderwriting());
        }

        if (groupResult.requiresReview()) {
            log.info("Manual review required: {}", groupResult.groupUnderwriting().getDeclineReason());
            return responseAssembler.buildReviewRequiredResponse(request, groupResult.groupUnderwriting());
        }

        // STEP 4: Применение скидок к итоговой сумме полиса
        try {
            var discountResult = discountApplicationService.applyDiscounts(
                    request,
                    groupResult.totalPremium()
            );

            // STEP 5: Сборка ответа с данными GroupPremiumResult
            return responseAssembler.buildSuccessResponse(
                    request,
                    // Оборачиваем в PremiumCalculationResult для совместимости с ResponseAssembler
                    new PremiumCalculationService.PremiumCalculationResult(
                            groupResult.totalPremium(),
                            groupResult.firstPersonDetails()
                    ),
                    discountResult,
                    groupResult.groupUnderwriting(),
                    includeDetails
            );

        } catch (Exception e) {
            log.error("Discount application or response assembly failed", e);
            return responseAssembler.buildSystemErrorResponse(e.getMessage());
        }
    }
}