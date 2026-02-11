package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;

/**
 * Application-layer сервис для андеррайтинга
 *
 * ЦЕЛЬ: Адаптер между application и core layers
 *
 * ОБЯЗАННОСТИ:
 * 1. Делегирование андеррайтинга core сервису
 * 2. Логирование на application уровне
 *
 * МЕТРИКИ:
 * - Complexity: 1
 * - LOC: ~20
 * - Pure delegation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingApplicationService {

    private final UnderwritingService underwritingService;

    /**
     * Оценивает заявку через андеррайтинг
     */
    public UnderwritingResult evaluate(TravelCalculatePremiumRequest request) {
        log.debug("Evaluating underwriting for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        return underwritingService.evaluateApplication(request);
    }
}