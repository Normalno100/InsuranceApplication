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
 * РЕФАКТОРИНГ (п. 4.3 плана):
 * - PremiumCalculationResult декомпозирован с 22 параметров на вложенные records:
 *     AgeDetails       — возраст и возрастной коэффициент
 *     CountryDetails   — страна, коэффициент, дефолтные ставки
 *     TripDetails      — дни, коэффициенты длительности, покрытие
 *     RiskDetails      — детали рисков, пакетная скидка
 *     PayoutLimitDetails — лимит выплат (task_117)
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

    // ── Вложенные records (декомпозиция 22 полей) ──────────────────────────

    /**
     * Возраст застрахованного и возрастной коэффициент.
     */
    public record AgeDetails(
            int age,
            BigDecimal ageCoefficient,
            String ageGroupDescription
    ) {}

    /**
     * Информация о стране назначения.
     *
     * countryDefaultDayPremium       — дефолтная ставка, применённая в расчёте (только COUNTRY_DEFAULT)
     * countryDefaultDayPremiumForInfo — дефолтная ставка для информации в ответе (оба режима)
     * countryDefaultCurrency          — валюта дефолтной ставки
     */
    public record CountryDetails(
            String countryName,
            BigDecimal countryCoefficient,
            BigDecimal countryDefaultDayPremium,
            BigDecimal countryDefaultDayPremiumForInfo,
            String countryDefaultCurrency
    ) {}

    /**
     * Параметры поездки и расчётные коэффициенты.
     */
    public record TripDetails(
            int days,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            BigDecimal totalCoefficient,
            BigDecimal coverageAmount
    ) {}

    /**
     * Детализация по рискам и пакетная скидка.
     */
    public record RiskDetails(
            List<RiskPremiumDetail> riskPremiumDetails,
            BundleDiscountResult bundleDiscount
    ) {}

    /**
     * Лимит страховых выплат (task_117).
     *
     * medicalPayoutLimit   — лимит выплат для отображения в TripSummary
     * appliedPayoutLimit   — фактически применённый лимит
     * payoutLimitApplied   — true если премия была скорректирована из-за лимита
     */
    public record PayoutLimitDetails(
            BigDecimal medicalPayoutLimit,
            BigDecimal appliedPayoutLimit,
            boolean payoutLimitApplied
    ) {}

    /**
     * Полный результат расчёта премии.
     *
     * РЕФАКТОРИНГ (п. 4.3): вместо 22 плоских параметров — 5 вложенных records.
     *
     * Для обратной совместимости добавлены convenience-методы,
     * делегирующие к вложенным records.
     */
    public record PremiumCalculationResult(
            BigDecimal premium,
            BigDecimal baseRate,
            AgeDetails ageDetails,
            CountryDetails countryDetails,
            TripDetails tripDetails,
            RiskDetails riskDetails,
            CalculationMode calculationMode,
            List<CalculationStep> calculationSteps,
            PayoutLimitDetails payoutLimitDetails
    ) { }

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