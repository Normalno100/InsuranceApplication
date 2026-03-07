package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Компонент расчёта длительности поездки и коэффициента длительности.
 *
 * ОТВЕТСТВЕННОСТЬ (SRP):
 *   Расчёт количества дней между датами и получение соответствующего
 *   коэффициента из таблицы trip_duration_coefficients.
 *
 * АРХИТЕКТУРА:
 *   core слой → зависит от TripDurationPricingService (тоже core).
 *   Нет зависимостей на infrastructure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripDurationCalculator {

    private final TripDurationPricingService durationPricingService;

    /**
     * Рассчитывает количество дней поездки (dateFrom включительно, dateTo исключительно).
     *
     * @param dateFrom дата начала поездки
     * @param dateTo   дата окончания поездки
     * @return количество дней (>= 0)
     */
    public long calculateDays(LocalDate dateFrom, LocalDate dateTo) {
        long days = ChronoUnit.DAYS.between(dateFrom, dateTo);
        log.debug("Trip duration: {} → {} = {} days", dateFrom, dateTo, days);
        return days;
    }

    /**
     * Получает коэффициент длительности для заданного количества дней на указанную дату.
     *
     * @param days количество дней поездки
     * @param date дата, на которую нужен коэффициент (обычно agreementDateFrom)
     * @return коэффициент (1.0 = без скидки, < 1.0 = скидка за длительность)
     */
    public BigDecimal getDurationCoefficient(long days, LocalDate date) {
        BigDecimal coeff = durationPricingService.getDurationCoefficient((int) days, date);
        log.debug("Duration coefficient: {} days on {} → {}", days, date, coeff);
        return coeff;
    }
}