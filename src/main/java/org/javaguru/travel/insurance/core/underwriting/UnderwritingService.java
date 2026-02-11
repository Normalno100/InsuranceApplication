package org.javaguru.travel.insurance.core.underwriting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.underwriting.persistence.UnderwritingPersistenceService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;

/**
 * Сервис андеррайтинга - главный входная точка для оценки заявок
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingService {

    private final UnderwritingEngine underwritingEngine;
    private final UnderwritingPersistenceService persistenceService;  // 👈 НОВОЕ

    public UnderwritingResult evaluateApplication(TravelCalculatePremiumRequest request) {
        log.info("Evaluating underwriting for application: {} {} to {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode()
        );

        // Засекаем время
        long startTime = System.currentTimeMillis();

        // Делегируем оценку движку
        UnderwritingResult result = underwritingEngine.evaluate(request);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Underwriting decision: {} for {} {} ({}ms)",
                result.getDecision(),
                request.getPersonFirstName(),
                request.getPersonLastName(),
                duration
        );

        // 👇 НОВОЕ: Сохраняем решение в БД
        try {
            persistenceService.saveDecision(request, result, duration);
        } catch (Exception e) {
            log.error("Error saving underwriting decision to database", e);
            // Не прерываем процесс, если сохранение не удалось
        }

        return result;
    }
}