package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Компонент расчёта возраста и возрастного коэффициента.
 *
 * ОТВЕТСТВЕННОСТЬ (SRP):
 *   Единственная ответственность — делегировать расчёт возраста
 *   и коэффициента в {@link AgeCalculator}, предоставляя удобный API
 *   для стратегий расчёта премии.
 *
 * АРХИТЕКТУРА:
 *   core слой → зависит только от AgeCalculator (тоже core).
 *   Нет зависимостей на infrastructure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PersonAgeCalculator {

    private final AgeCalculator ageCalculator;

    /**
     * Рассчитывает возраст и коэффициент. Коэффициент всегда применяется.
     *
     * @param birthDate     дата рождения застрахованного
     * @param referenceDate дата начала поездки (agreementDateFrom)
     * @return результат с возрастом, коэффициентом и описанием группы
     */
    public AgeCalculator.AgeCalculationResult calculate(
            LocalDate birthDate,
            LocalDate referenceDate) {
        return calculate(birthDate, referenceDate, true);
    }

    /**
     * Рассчитывает возраст и коэффициент с учётом флага включения.
     *
     * task_116: если ageCoefficientEnabled=false, коэффициент возвращается равным 1.0.
     *
     * @param birthDate               дата рождения
     * @param referenceDate           дата начала поездки
     * @param ageCoefficientEnabled   true = применять коэффициент, false = 1.0
     * @return результат с возрастом, коэффициентом и описанием группы
     */
    public AgeCalculator.AgeCalculationResult calculate(
            LocalDate birthDate,
            LocalDate referenceDate,
            boolean ageCoefficientEnabled) {

        AgeCalculator.AgeCalculationResult result =
                ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate, ageCoefficientEnabled);

        log.debug("Age calculation: birthDate={}, referenceDate={}, age={}, coeff={}, enabled={}",
                birthDate, referenceDate,
                result.age(), result.coefficient(), ageCoefficientEnabled);

        return result;
    }
}