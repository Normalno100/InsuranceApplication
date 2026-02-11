package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.TripDurationCoefficientRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сервис для расчета коэффициента длительности поездки
 * Прогрессивная шкала — длительные поездки дешевле
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TripDurationPricingService {

    private final TripDurationCoefficientRepository durationRepository;

    /**
     * Получает коэффициент для указанного количества дней
     *
     * @param days количество дней поездки
     * @param date дата на которую применяется коэффициент
     * @return коэффициент (1.0 = без скидки, 0.85 = -15% скидка)
     */
    public BigDecimal getDurationCoefficient(int days, LocalDate date) {
        log.debug("Getting duration coefficient for {} days on {}", days, date);

        var coefficientOpt = durationRepository.findCoefficientForDays(days, date);

        if (coefficientOpt.isEmpty()) {
            log.warn("No duration coefficient found for {} days, using default 1.0", days);
            return BigDecimal.ONE;
        }

        var coefficient = coefficientOpt.get();
        log.debug("Found duration coefficient: {} ({})",
                coefficient.getCoefficient(),
                coefficient.getDescription());

        return coefficient.getCoefficient();
    }

    /**
     * Получает коэффициент на текущую дату
     */
    public BigDecimal getDurationCoefficient(int days) {
        return getDurationCoefficient(days, LocalDate.now());
    }

    /**
     * Результат с детальной информацией
     */
    public DurationPricingResult getDurationPricingDetails(int days, LocalDate date) {
        var coefficientOpt = durationRepository.findCoefficientForDays(days, date);

        if (coefficientOpt.isEmpty()) {
            return new DurationPricingResult(
                    days,
                    BigDecimal.ONE,
                    "No special discount",
                    false
            );
        }

        var coefficient = coefficientOpt.get();
        boolean hasDiscount = coefficient.getCoefficient()
                .compareTo(BigDecimal.ONE) < 0;

        return new DurationPricingResult(
                days,
                coefficient.getCoefficient(),
                coefficient.getDescription(),
                hasDiscount
        );
    }

    /**
     * Результат расчета коэффициента длительности
     */
    public record DurationPricingResult(
            int days,
            BigDecimal coefficient,
            String description,
            boolean hasDiscount
    ) {
        /**
         * Рассчитывает процент скидки
         */
        public BigDecimal getDiscountPercentage() {
            if (!hasDiscount) {
                return BigDecimal.ZERO;
            }
            return BigDecimal.ONE.subtract(coefficient)
                    .multiply(BigDecimal.valueOf(100));
        }
    }
}