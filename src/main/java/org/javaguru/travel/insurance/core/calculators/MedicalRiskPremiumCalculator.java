package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный калькулятор медицинской страховой премии.
 *
 * ПОДДЕРЖИВАЕТ ДВА РЕЖИМА РАСЧЁТА (этап 5):
 *
 * 1. MEDICAL_LEVEL (стандартный):
 *    ПРЕМИЯ = DAILY_RATE × AGE_COEFF × COUNTRY_COEFF × DURATION_COEFF
 *             × (1 + RISK_COEFFS) × DAYS - BUNDLE_DISCOUNT
 *    Источник базовой ставки: medical_risk_limit_levels.daily_rate
 *
 * 2. COUNTRY_DEFAULT (новый):
 *    ПРЕМИЯ = DEFAULT_DAY_PREMIUM × AGE_COEFF × DURATION_COEFF
 *             × (1 + RISK_COEFFS) × DAYS - BUNDLE_DISCOUNT
 *    Источник базовой ставки: country_default_day_premiums.default_day_premium
 *    COUNTRY_COEFF не применяется (уже "запечён" в DEFAULT_DAY_PREMIUM)
 *
 * ВЫБОР РЕЖИМА:
 * - Если request.useCountryDefaultPremium == true И для страны есть запись → COUNTRY_DEFAULT
 * - В остальных случаях (включая fallback) → MEDICAL_LEVEL
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;
    private final TripDurationPricingService durationPricingService;
    private final RiskBundleService riskBundleService;
    private final AgeRiskPricingService ageRiskPricingService;

    /** Новая зависимость — сервис дефолтных дневных премий (этап 5) */
    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;

    // ========================================
    // ПУБЛИЧНЫЕ МЕТОДЫ
    // ========================================

    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        return calculatePremiumWithDetails(request).premium();
    }

    /**
     * Рассчитывает премию с полной детальной разбивкой.
     * Автоматически выбирает режим расчёта (MEDICAL_LEVEL или COUNTRY_DEFAULT).
     */
    public PremiumCalculationResult calculatePremiumWithDetails(TravelCalculatePremiumRequest request) {
        log.info("Starting premium calculation. Country: {}, useCountryDefault: {}",
                request.getCountryIsoCode(),
                request.getUseCountryDefaultPremium());

        // Определяем режим расчёта
        boolean useCountryDefault = shouldUseCountryDefaultMode(request);

        if (useCountryDefault) {
            return calculateWithCountryDefault(request);
        } else {
            return calculateWithMedicalLevel(request);
        }
    }

    // ========================================
    // РЕЖИМ 1: MEDICAL_LEVEL (стандартный)
    // ========================================

    /**
     * Стандартный расчёт через уровень медицинского покрытия.
     * Логика полностью сохранена из исходной реализации.
     */
    private PremiumCalculationResult calculateWithMedicalLevel(TravelCalculatePremiumRequest request) {
        log.info("Using MEDICAL_LEVEL calculation mode");

        // 1. Справочные данные
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()));

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 2. Возраст и коэффициент
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom());

        // 3. Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo());

        // 4. Коэффициент длительности
        BigDecimal durationCoefficient = durationPricingService.getDurationCoefficient(
                (int) days, request.getAgreementDateFrom());

        // 5. Дополнительные риски с возрастными модификаторами
        AdditionalRisksCalculation additionalRisksCalc = calculateAdditionalRisksWithAgeModifiers(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 6. Итоговый коэффициент (включая country coefficient — особенность MEDICAL_LEVEL)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(country.getRiskCoefficient())
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisksCalc.totalCoefficient()));

        // 7. Базовая премия
        BigDecimal basePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 8. Пакетная скидка
        BundleDiscountResult bundleDiscount = calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 10. Детали по рискам
        List<RiskPremiumDetail> riskDetails = calculateRiskDetails(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 11. Дефолтная дневная премия страны (для информации в ответе)
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremiumInfo =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElse(null);

        log.info("MEDICAL_LEVEL final premium: {} (bundle discount: {})",
                finalPremium, bundleDiscount.discountAmount());

        return new PremiumCalculationResult(
                finalPremium,
                medicalLevel.getDailyRate(),
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),
                country.getNameEn(),
                durationCoefficient,
                additionalRisksCalc.totalCoefficient(),
                totalCoeff,
                (int) days,
                medicalLevel.getCoverageAmount(),
                riskDetails,
                bundleDiscount,
                buildCalculationSteps(
                        medicalLevel.getDailyRate(),
                        ageResult.coefficient(),
                        country.getRiskCoefficient(),
                        durationCoefficient,
                        additionalRisksCalc.totalCoefficient(),
                        days,
                        basePremium,
                        bundleDiscount.discountAmount(),
                        finalPremium),
                // Поля этапа 5
                CalculationMode.MEDICAL_LEVEL,
                null,    // countryDefaultDayPremium — не используется в этом режиме
                defaultPremiumInfo != null ? defaultPremiumInfo.defaultDayPremium() : null,
                defaultPremiumInfo != null ? defaultPremiumInfo.currency() : null
        );
    }

    // ========================================
    // РЕЖИМ 2: COUNTRY_DEFAULT (новый, этап 5)
    // ========================================

    /**
     * Расчёт через дефолтную дневную премию страны.
     *
     * ОТЛИЧИЕ от MEDICAL_LEVEL:
     * - Базовая ставка: country_default_day_premiums.default_day_premium
     * - Коэффициент страны НЕ применяется (уже учтён в ставке)
     * - medicalRiskLimitLevel и coverageAmount в ответе будут null
     */
    private PremiumCalculationResult calculateWithCountryDefault(TravelCalculatePremiumRequest request) {
        log.info("Using COUNTRY_DEFAULT calculation mode for country: {}", request.getCountryIsoCode());

        // 1. Получаем дефолтную дневную премию страны
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremium =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElseThrow(() -> new IllegalStateException(
                                "Country default day premium not found for: " + request.getCountryIsoCode()
                                        + ". This should not happen — shouldUseCountryDefaultMode() returned true."));

        // 2. Получаем информацию о стране (для коэффициента в деталях)
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 3. Возраст и коэффициент
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom());

        // 4. Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo());

        // 5. Коэффициент длительности
        BigDecimal durationCoefficient = durationPricingService.getDurationCoefficient(
                (int) days, request.getAgreementDateFrom());

        // 6. Дополнительные риски с возрастными модификаторами
        AdditionalRisksCalculation additionalRisksCalc = calculateAdditionalRisksWithAgeModifiers(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 7. Базовая премия — БЕЗ country coefficient!
        //    totalCoeff не включает country.getRiskCoefficient()
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisksCalc.totalCoefficient()));

        BigDecimal basePremium = countryDefaultDayPremiumService.calculateBasePremium(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                (int) days);

        // Применяем коэффициент доп. рисков к базовой премии
        if (additionalRisksCalc.totalCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            basePremium = basePremium
                    .multiply(BigDecimal.ONE.add(additionalRisksCalc.totalCoefficient()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 8. Пакетная скидка
        BundleDiscountResult bundleDiscount = calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 10. Детали по рискам (передаём country.getRiskCoefficient() как BigDecimal.ONE,
        //     т.к. он не участвует в формуле в этом режиме)
        List<RiskPremiumDetail> riskDetails = calculateRiskDetails(
                request.getSelectedRisks(),
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                BigDecimal.ONE,   // country coefficient не применяется
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 11. Шаги расчёта (с пометкой о режиме)
        List<CalculationStep> steps = buildCalculationStepsForCountryDefault(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                additionalRisksCalc.totalCoefficient(),
                days,
                basePremium,
                bundleDiscount.discountAmount(),
                finalPremium);

        log.info("COUNTRY_DEFAULT final premium: {} (base rate: {}, bundle discount: {})",
                finalPremium, defaultPremium.defaultDayPremium(), bundleDiscount.discountAmount());

        return new PremiumCalculationResult(
                finalPremium,
                defaultPremium.defaultDayPremium(),  // baseRate = defaultDayPremium
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),         // присутствует для информации, но не используется в формуле
                country.getNameEn(),
                durationCoefficient,
                additionalRisksCalc.totalCoefficient(),
                totalCoeff,
                (int) days,
                null,  // coverageAmount — не применимо в COUNTRY_DEFAULT режиме
                riskDetails,
                bundleDiscount,
                steps,
                // Поля этапа 5
                CalculationMode.COUNTRY_DEFAULT,
                defaultPremium.defaultDayPremium(),
                defaultPremium.defaultDayPremium(),
                defaultPremium.currency()
        );
    }

    // ========================================
    // ОПРЕДЕЛЕНИЕ РЕЖИМА РАСЧЁТА
    // ========================================

    /**
     * Определяет, нужно ли использовать режим COUNTRY_DEFAULT.
     *
     * Условия для COUNTRY_DEFAULT:
     * 1. request.useCountryDefaultPremium == true
     * 2. Для страны есть активная запись в country_default_day_premiums
     *
     * Если запись отсутствует — автоматический fallback на MEDICAL_LEVEL.
     */
    private boolean shouldUseCountryDefaultMode(TravelCalculatePremiumRequest request) {
        if (!Boolean.TRUE.equals(request.getUseCountryDefaultPremium())) {
            return false;
        }

        boolean hasDefaultPremium = countryDefaultDayPremiumService.hasDefaultDayPremium(
                request.getCountryIsoCode(),
                request.getAgreementDateFrom());

        if (!hasDefaultPremium) {
            log.warn("useCountryDefaultPremium=true requested for country '{}', " +
                            "but no default day premium found. Falling back to MEDICAL_LEVEL.",
                    request.getCountryIsoCode());
        }

        return hasDefaultPremium;
    }

    // ========================================
    // ОБЩИЕ ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    private AdditionalRisksCalculation calculateAdditionalRisksWithAgeModifiers(
            List<String> selectedRiskCodes,
            int age,
            java.time.LocalDate agreementDate) {

        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return new AdditionalRisksCalculation(BigDecimal.ZERO, new ArrayList<>());
        }

        List<ModifiedRiskDetail> modifiedRisks = new ArrayList<>();
        BigDecimal totalCoefficient = BigDecimal.ZERO;

        for (String riskCode : selectedRiskCodes) {
            var riskOpt = riskTypeRepository.findActiveByCode(riskCode, agreementDate);

            if (riskOpt.isPresent() && !riskOpt.get().getIsMandatory()) {
                var risk = riskOpt.get();
                BigDecimal baseCoefficient = risk.getCoefficient();

                BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                        riskCode, age, agreementDate);

                BigDecimal modifiedCoefficient = baseCoefficient.multiply(ageModifier);

                modifiedRisks.add(new ModifiedRiskDetail(
                        riskCode, baseCoefficient, ageModifier, modifiedCoefficient));

                totalCoefficient = totalCoefficient.add(modifiedCoefficient);

                log.debug("Risk '{}': base={}, age_modifier={}, modified={}",
                        riskCode, baseCoefficient, ageModifier, modifiedCoefficient);
            }
        }

        return new AdditionalRisksCalculation(totalCoefficient, modifiedRisks);
    }

    private BundleDiscountResult calculateBundleDiscount(
            List<String> selectedRisks,
            BigDecimal premiumAmount,
            java.time.LocalDate agreementDate) {

        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        var bestBundleOpt = riskBundleService.getBestApplicableBundle(selectedRisks, agreementDate);

        if (bestBundleOpt.isEmpty()) {
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        var bundle = bestBundleOpt.get();
        BigDecimal discountAmount = riskBundleService.calculateBundleDiscount(premiumAmount, bundle);

        log.info("Applied bundle '{}' with {}% discount = {} EUR",
                bundle.code(), bundle.discountPercentage(), discountAmount);

        return new BundleDiscountResult(bundle, discountAmount);
    }

    private List<RiskPremiumDetail> calculateRiskDetails(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            int days,
            int age,
            java.time.LocalDate agreementDate) {

        List<RiskPremiumDetail> details = new ArrayList<>();

        BigDecimal basePremium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        var medicalRisk = riskTypeRepository.findActiveByCode("TRAVEL_MEDICAL", agreementDate)
                .orElseThrow();

        details.add(new RiskPremiumDetail(
                medicalRisk.getCode(),
                medicalRisk.getNameEn(),
                basePremium,
                BigDecimal.ZERO,
                BigDecimal.ONE));

        if (selectedRiskCodes != null) {
            for (String riskCode : selectedRiskCodes) {
                var riskOpt = riskTypeRepository.findActiveByCode(riskCode, agreementDate);
                if (riskOpt.isPresent() && !riskOpt.get().getIsMandatory()) {
                    var risk = riskOpt.get();

                    BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                            riskCode, age, agreementDate);

                    BigDecimal modifiedCoefficient = risk.getCoefficient().multiply(ageModifier);

                    BigDecimal riskPremium = basePremium
                            .multiply(modifiedCoefficient)
                            .setScale(2, RoundingMode.HALF_UP);

                    details.add(new RiskPremiumDetail(
                            risk.getCode(),
                            risk.getNameEn(),
                            riskPremium,
                            risk.getCoefficient(),
                            ageModifier));
                }
            }
        }

        return details;
    }

    /**
     * Шаги расчёта для режима MEDICAL_LEVEL (исходная логика).
     */
    private List<CalculationStep> buildCalculationSteps(
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal basePremium,
            BigDecimal bundleDiscount,
            BigDecimal finalPremium) {

        List<CalculationStep> steps = new ArrayList<>();

        steps.add(new CalculationStep("Base daily rate (medical level)", "Base Rate", baseRate));

        steps.add(new CalculationStep(
                "Age coefficient",
                String.format("%.2f × %.4f (age coeff)", baseRate, ageCoefficient),
                baseRate.multiply(ageCoefficient)));

        steps.add(new CalculationStep(
                "Country risk coefficient",
                String.format("× %.4f (country coeff)", countryCoefficient),
                baseRate.multiply(ageCoefficient).multiply(countryCoefficient)));

        steps.add(new CalculationStep(
                "Duration coefficient",
                String.format("× %.4f (duration coeff)", durationCoefficient),
                baseRate.multiply(ageCoefficient)
                        .multiply(countryCoefficient)
                        .multiply(durationCoefficient)));

        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Additional risks (age-modified)",
                    String.format("× (1 + %.4f)", additionalRisksCoefficient),
                    baseRate.multiply(ageCoefficient)
                            .multiply(countryCoefficient)
                            .multiply(durationCoefficient)
                            .multiply(BigDecimal.ONE.add(additionalRisksCoefficient))));
        }

        steps.add(new CalculationStep(
                "Multiply by trip days",
                String.format("× %d days", days),
                basePremium));

        if (bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Bundle discount",
                    String.format("- %.2f (bundle discount)", bundleDiscount),
                    finalPremium));
        }

        return steps;
    }

    /**
     * Шаги расчёта для режима COUNTRY_DEFAULT (новый, этап 5).
     * Отличается отсутствием шага "Country coefficient".
     */
    private List<CalculationStep> buildCalculationStepsForCountryDefault(
            BigDecimal defaultDayPremium,
            BigDecimal ageCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal basePremium,
            BigDecimal bundleDiscount,
            BigDecimal finalPremium) {

        List<CalculationStep> steps = new ArrayList<>();

        steps.add(new CalculationStep(
                "Country default day premium",
                "Default Day Rate (country-specific base rate)",
                defaultDayPremium));

        steps.add(new CalculationStep(
                "Age coefficient",
                String.format("%.2f × %.4f (age coeff)", defaultDayPremium, ageCoefficient),
                defaultDayPremium.multiply(ageCoefficient)));

        steps.add(new CalculationStep(
                "Duration coefficient",
                String.format("× %.4f (duration coeff) [country coeff already in base rate]",
                        durationCoefficient),
                defaultDayPremium.multiply(ageCoefficient).multiply(durationCoefficient)));

        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Additional risks (age-modified)",
                    String.format("× (1 + %.4f)", additionalRisksCoefficient),
                    defaultDayPremium.multiply(ageCoefficient)
                            .multiply(durationCoefficient)
                            .multiply(BigDecimal.ONE.add(additionalRisksCoefficient))));
        }

        steps.add(new CalculationStep(
                "Multiply by trip days",
                String.format("× %d days", days),
                basePremium));

        if (bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Bundle discount",
                    String.format("- %.2f (bundle discount)", bundleDiscount),
                    finalPremium));
        }

        return steps;
    }

    // ========================================
    // ВЛОЖЕННЫЕ ТИПЫ
    // ========================================

    /** Режим расчёта премии */
    public enum CalculationMode {
        MEDICAL_LEVEL,
        COUNTRY_DEFAULT
    }

    private record AdditionalRisksCalculation(
            BigDecimal totalCoefficient,
            List<ModifiedRiskDetail> modifiedRisks
    ) {}

    private record ModifiedRiskDetail(
            String riskCode,
            BigDecimal baseCoefficient,
            BigDecimal ageModifier,
            BigDecimal modifiedCoefficient
    ) {}

    public record BundleDiscountResult(
            RiskBundleService.ApplicableBundleResult bundle,
            BigDecimal discountAmount
    ) {}

    /**
     * Результат расчёта премии.
     *
     * ИЗМЕНЕНИЯ v2 (этап 5):
     * - calculationMode      — режим расчёта (MEDICAL_LEVEL / COUNTRY_DEFAULT)
     * - countryDefaultDayPremium — дефолтная дневная ставка страны (null в MEDICAL_LEVEL)
     * - countryDefaultDayPremiumForInfo — ставка страны для информации (всегда, если есть)
     * - countryDefaultCurrency — валюта дефолтной ставки
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
            // --- новые поля этапа 5 ---
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