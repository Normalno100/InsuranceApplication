package org.javaguru.travel.insurance.application.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.service.DiscountApplicationService;
import org.javaguru.travel.insurance.application.service.PremiumCalculationService;
import org.javaguru.travel.insurance.application.service.UnderwritingApplicationService;
import org.javaguru.travel.insurance.application.assembler.ResponseAssembler;
import org.javaguru.travel.insurance.core.validation.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.core.validation.ValidationError;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Оркестратор расчета премии
 *
 * ЦЕЛЬ: Координация процесса расчета без знания деталей реализации
 *
 * ОБЯЗАННОСТИ:
 * 1. Валидация запроса
 * 2. Проверка андеррайтинга
 * 3. Координация расчета премии
 * 4. Применение скидок
 * 5. Сборка финального ответа
 *
 * МЕТРИКИ:
 * - Complexity: 5 (было 25 в God Service)
 * - LOC: ~90 (было 350)
 * - Dependencies: 5 специализированных сервисов
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
     * Главный метод оркестрации расчета премии
     *
     * Поток:
     * 1. Validate → 2. Underwrite → 3. Calculate → 4. Apply Discounts → 5. Assemble Response
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

        // STEP 2: Андеррайтинг
        var underwritingResult = underwritingService.evaluate(request);

        if (underwritingResult.isDeclined()) {
            log.warn("Application declined: {}", underwritingResult.getDeclineReason());
            return responseAssembler.buildDeclinedResponse(request, underwritingResult);
        }

        if (underwritingResult.requiresManualReview()) {
            log.info("Manual review required: {}", underwritingResult.getDeclineReason());
            return responseAssembler.buildReviewRequiredResponse(request, underwritingResult);
        }

        // STEP 3: Расчет премии
        try {
            var calculationResult = premiumCalculationService.calculate(request);

            // STEP 4: Применение скидок
            var discountResult = discountApplicationService.applyDiscounts(
                    request,
                    calculationResult.premium()
            );

            // STEP 5: Сборка ответа
            return responseAssembler.buildSuccessResponse(
                    request,
                    calculationResult,
                    discountResult,
                    underwritingResult,
                    includeDetails
            );

        } catch (Exception e) {
            log.error("Premium calculation failed", e);
            return responseAssembler.buildSystemErrorResponse(e.getMessage());
        }
    }
}