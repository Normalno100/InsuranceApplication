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
 * ИЗМЕНЕНИЯ task_117:
 * - PremiumCalculationResult дополнен полями:
 *     medicalPayoutLimit      — лимит выплат для отображения в TripSummary
 *     appliedPayoutLimit      — фактически применённый лимит
 *     payoutLimitApplied      — флаг: была ли произведена корректировка премии
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
    // ВЛОЖЕННЫЕ ТИПЫ
    // ========================================

    /** Режим расчёта премии */
    public enum CalculationMode {
        MEDICAL_LEVEL,
        COUNTRY_DEFAULT
    }

    public record BundleDiscountResult(
            RiskBundleService.ApplicableBundleResult bundle,
            BigDecimal discountAmount
    ) {}

    /**
     * Полный результат расчёта премии.
     *
     * ИЗМЕНЕНИЯ task_117:
     *   medicalPayoutLimit   — лимит выплат (для отображения в TripSummary)
     *   appliedPayoutLimit   — фактически применённый лимит
     *   payoutLimitApplied   — true если премия была скорректирована из-за лимита
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
            // Режим расчёта
            CalculationMode calculationMode,
            BigDecimal countryDefaultDayPremium,
            BigDecimal countryDefaultDayPremiumForInfo,
            String countryDefaultCurrency,
            // task_117: лимит выплат
            BigDecimal medicalPayoutLimit,
            BigDecimal appliedPayoutLimit,
            Boolean payoutLimitApplied
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