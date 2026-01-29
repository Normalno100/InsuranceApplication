package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * Сервис расчета премии
 *
 * ЦЕЛЬ: Координация расчета базовой премии
 *
 * ОБЯЗАННОСТИ:
 * 1. Делегирование расчета калькулятору
 * 2. Валидация результатов
 * 3. Применение минимальной премии
 *
 * МЕТРИКИ:
 * - Complexity: 3
 * - LOC: ~60
 * - Single Responsibility: только расчет премии
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumCalculationService {

    private final MedicalRiskPremiumCalculator medicalRiskCalculator;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    /**
     * Рассчитывает премию с детальной разбивкой
     */
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.debug("Calculating premium for country: {}, medical level: {}",
                request.getCountryIsoCode(), request.getMedicalRiskLimitLevel());

        // Делегируем расчет специализированному калькулятору
        var calculatorResult = medicalRiskCalculator.calculatePremiumWithDetails(request);

        // Применяем минимальную премию
        BigDecimal finalPremium = applyMinimumPremium(calculatorResult.premium());

        log.info("Premium calculated: {} EUR (before discounts)", finalPremium);

        return new PremiumCalculationResult(
                finalPremium,
                calculatorResult
        );
    }

    /**
     * Применяет минимальную премию
     */
    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0) {
            log.debug("Applying minimum premium: {} -> {}", premium, MIN_PREMIUM);
            return MIN_PREMIUM;
        }
        return premium;
    }

    /**
     * Результат расчета премии
     */
    public record PremiumCalculationResult(
            BigDecimal premium,
            MedicalRiskPremiumCalculator.PremiumCalculationResult details
    ) {}
}