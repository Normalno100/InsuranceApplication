package org.javaguru.travel.insurance.core.underwriting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;

/**
 * Сервис андеррайтинга - главный входная точка для оценки заявок
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingService {

    private final UnderwritingEngine underwritingEngine;

    /**
     * Оценивает заявку на соответствие правилам андеррайтинга
     *
     * @param request заявка на страхование
     * @return результат андеррайтинга
     */
    public UnderwritingResult evaluateApplication(TravelCalculatePremiumRequest request) {
        log.info("Evaluating underwriting for application: {} {} to {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode()
        );

        // Делегируем оценку движку
        UnderwritingResult result = underwritingEngine.evaluate(request);

        log.info("Underwriting decision: {} for {} {}",
                result.getDecision(),
                request.getPersonFirstName(),
                request.getPersonLastName()
        );

        return result;
    }
}