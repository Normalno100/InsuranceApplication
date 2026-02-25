package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;

/**
 * Стратегия расчёта страховой премии.
 * Выбор стратегии выполняет {@link org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator}.
 */
public interface PremiumCalculationStrategy {

    /**
     * Выполняет расчёт премии согласно конкретной стратегии.
     *
     * @param request запрос с параметрами страхования
     * @return детальный результат расчёта
     */
    PremiumCalculationResult calculate(TravelCalculatePremiumRequest request);
}
