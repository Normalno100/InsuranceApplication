package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.strategy.CountryDefaultPremiumStrategy;
import org.javaguru.travel.insurance.core.calculators.strategy.MedicalLevelPremiumStrategy;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Фасад расчёта медицинской страховой премии.
 *
 * РЕФАКТОРИНГ task_115 — паттерн Strategy:
 *
 * До рефакторинга:
 *   - ~400 строк в одном классе (нарушение SRP)
 *   - Два режима в одном классе (сложная логика)
 *   - Дублирование кода между calculateWithMedicalLevel() и calculateWithCountryDefault()
 *   - Прямые зависимости от множества репозиториев
 *
 * После рефакторинга:
 *   MedicalRiskPremiumCalculator  ← фасад (этот класс, ~60 строк)
 *   ├── MedicalLevelPremiumStrategy   ← логика MEDICAL_LEVEL
 *   ├── CountryDefaultPremiumStrategy ← логика COUNTRY_DEFAULT
 *   ├── SharedCalculationComponents   ← общие компоненты (возраст, риски, скидки, детали)
 *   └── CalculationStepsBuilder       ← построение шагов расчёта
 *
 * task_114:
 *   - ageCoefficient явно отображается как отдельный шаг с промежуточным значением
 *   - referenceDate (agreementDateFrom) передаётся в AgeCalculator для выбора
 *     актуальной версии коэффициента из таблицы age_coefficients
 *   - Формула в шагах: "DailyRate × ageCoeff = X" вместо просто "× ageCoeff"
 *
 * ПОДДЕРЖИВАЕМЫЕ РЕЖИМЫ:
 *   MEDICAL_LEVEL  — стандартный, через медицинский уровень покрытия
 *   COUNTRY_DEFAULT — через дефолтную дневную ставку страны
 *
 * ВЫБОР РЕЖИМА:
 *   useCountryDefaultPremium=true + существует запись в country_default_day_premiums
 *     → COUNTRY_DEFAULT
 *   иначе → MEDICAL_LEVEL (в том числе как fallback)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRiskPremiumCalculator {

    private final MedicalLevelPremiumStrategy medicalLevelStrategy;
    private final CountryDefaultPremiumStrategy countryDefaultStrategy;
    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;

    // ========================================
    // ПУБЛИЧНОЕ API
    // ========================================

    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        return calculatePremiumWithDetails(request).premium();
    }

    /**
     * Рассчитывает премию с полной детальной разбивкой.
     * Автоматически выбирает стратегию (режим) расчёта.
     */
    public PremiumCalculationResult calculatePremiumWithDetails(TravelCalculatePremiumRequest request) {
        log.info("Premium calculation: country={}, useCountryDefault={}",
                request.getCountryIsoCode(), request.getUseCountryDefaultPremium());

        if (shouldUseCountryDefaultMode(request)) {
            return countryDefaultStrategy.calculate(request);
        } else {
            return medicalLevelStrategy.calculate(request);
        }
    }

    // ========================================
    // ВЫБОР СТРАТЕГИИ
    // ========================================

    /**
     * Определяет, нужно ли использовать стратегию COUNTRY_DEFAULT.
     *
     * Условия:
     * 1. request.useCountryDefaultPremium == true
     * 2. Для страны есть активная запись в country_default_day_premiums
     *
     * Если запись отсутствует — fallback на MEDICAL_LEVEL с предупреждением в логах.
     */
    private boolean shouldUseCountryDefaultMode(TravelCalculatePremiumRequest request) {
        if (!Boolean.TRUE.equals(request.getUseCountryDefaultPremium())) {
            return false;
        }

        boolean hasDefaultPremium = countryDefaultDayPremiumService.hasDefaultDayPremium(
                request.getCountryIsoCode(),
                request.getAgreementDateFrom());

        if (!hasDefaultPremium) {
            log.warn("useCountryDefaultPremium=true for country '{}' but no default day premium found. " +
                            "Falling back to MEDICAL_LEVEL.",
                    request.getCountryIsoCode());
        }

        return hasDefaultPremium;
    }

    // ========================================
    // ВЛОЖЕННЫЕ ТИПЫ (публичные — используются в ResponseAssembler и др.)
    // ========================================

    /** Режим расчёта премии */
    public enum CalculationMode {
        MEDICAL_LEVEL,
        COUNTRY_DEFAULT
    }

    /**
     * Результат расчёта пакетной скидки.
     *
     * Остаётся публичным т.к. используется в ResponseAssembler.
     */
    public record BundleDiscountResult(
            RiskBundleService.ApplicableBundleResult bundle,
            BigDecimal discountAmount
    ) {}

    /**
     * Полный результат расчёта премии.
     */
    public record PremiumCalculationResult(
            BigDecimal premium,
            BigDecimal baseRate,
            int age,
            BigDecimal ageCoefficient,
            String ageGroupDescription,
            BigDecimal countryCoefficient,
            String countryName,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            BigDecimal totalCoefficient,
            int days,
            BigDecimal coverageAmount,
            List<RiskPremiumDetail> riskDetails,
            BundleDiscountResult bundleDiscount,
            List<CalculationStep> calculationSteps,
            // Поля режима расчёта
            CalculationMode calculationMode,
            BigDecimal countryDefaultDayPremium,
            BigDecimal countryDefaultDayPremiumForInfo,
            String countryDefaultCurrency
    ) {}

    public record RiskPremiumDetail(
            String riskCode,
            String riskName,
            BigDecimal premium,
            BigDecimal coefficient,
            BigDecimal ageModifier
    ) {}

    public record CalculationStep(
            String description,
            String formula,
            BigDecimal result
    ) {}
}