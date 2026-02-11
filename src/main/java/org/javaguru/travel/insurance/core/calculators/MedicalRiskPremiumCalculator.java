package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
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
 * Главный калькулятор медицинской страховой премии
 *
 * ФОРМУЛА:
 * ПРЕМИЯ = БАЗОВАЯ_СТАВКА × КОЭФФ_ВОЗРАСТА × КОЭФФ_СТРАНЫ × КОЭФФ_ДЛИТЕЛЬНОСТИ
 *          × (1 + СУММА_МОДИФИЦИРОВАННЫХ_РИСКОВ) × ДНИ - СКИДКА_ПАКЕТА
 *
 * ГДЕ:
 * - КОЭФФ_ДЛИТЕЛЬНОСТИ: прогрессивная скидка за длительные поездки (ИДЕЯ #3)
 * - МОДИФИЦИРОВАННЫЕ_РИСКИ: риски с учетом возраста (ИДЕЯ #5)
 * - СКИДКА_ПАКЕТА: скидка за покупку пакета рисков (ИДЕЯ #2)
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

    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        var details = calculatePremiumWithDetails(request);
        return details.premium();
    }

    public PremiumCalculationResult calculatePremiumWithDetails(TravelCalculatePremiumRequest request) {
        log.info("Starting premium calculation with advanced pricing features");

        // 1. Получаем данные из БД
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));

        // 2. Расчёт возраста и коэффициента
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        // 3. Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        //  4. Коэффициент длительности (прогрессивная скидка)
        BigDecimal durationCoefficient = durationPricingService.getDurationCoefficient(
                (int) days,
                request.getAgreementDateFrom()
        );

        log.debug("Duration coefficient for {} days: {}", days, durationCoefficient);

        // 5. Коэффициент дополнительных рисков с возрастными модификаторами
        AdditionalRisksCalculation additionalRisksCalc = calculateAdditionalRisksWithAgeModifiers(
                request.getSelectedRisks(),
                ageResult.age(),
                request.getAgreementDateFrom()
        );

        log.debug("Additional risks coefficient (age-modified): {}",
                additionalRisksCalc.totalCoefficient());

        // 6. Итоговый коэффициент (БЕЗ пакетной скидки - она применяется отдельно)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(country.getRiskCoefficient())
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisksCalc.totalCoefficient()));

        // 7. Базовая премия (ДО пакетной скидки)
        BigDecimal basePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Base premium (before bundle discount): {}", basePremium);

        // 8. Пакетная скидка
        BundleDiscountResult bundleDiscount = calculateBundleDiscount(
                request.getSelectedRisks(),
                basePremium,
                request.getAgreementDateFrom()
        );

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("Final premium: {} (bundle discount: {})",
                finalPremium, bundleDiscount.discountAmount());

        // 10. Детали по рискам
        List<RiskPremiumDetail> riskDetails = calculateRiskDetails(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom()
        );

        // 11. Формируем результат
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
                        finalPremium
                )
        );
    }

    /**
     * Расчет дополнительных рисков с возрастными модификаторами
     */
    private AdditionalRisksCalculation calculateAdditionalRisksWithAgeModifiers(
            List<String> selectedRiskCodes,
            int age,
            java.time.LocalDate agreementDate) {

        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return new AdditionalRisksCalculation(
                    BigDecimal.ZERO,
                    new ArrayList<>()
            );
        }

        List<ModifiedRiskDetail> modifiedRisks = new ArrayList<>();
        BigDecimal totalCoefficient = BigDecimal.ZERO;

        for (String riskCode : selectedRiskCodes) {
            var riskOpt = riskTypeRepository.findActiveByCode(riskCode, agreementDate);

            if (riskOpt.isPresent() && !riskOpt.get().getIsMandatory()) {
                var risk = riskOpt.get();
                BigDecimal baseCoefficient = risk.getCoefficient();

                // Получаем возрастной модификатор
                BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                        riskCode, age, agreementDate
                );

                // Модифицированный коэффициент
                BigDecimal modifiedCoefficient = baseCoefficient.multiply(ageModifier);

                modifiedRisks.add(new ModifiedRiskDetail(
                        riskCode,
                        baseCoefficient,
                        ageModifier,
                        modifiedCoefficient
                ));

                totalCoefficient = totalCoefficient.add(modifiedCoefficient);

                log.debug("Risk '{}': base={}, age_modifier={}, modified={}",
                        riskCode, baseCoefficient, ageModifier, modifiedCoefficient);
            }
        }

        return new AdditionalRisksCalculation(totalCoefficient, modifiedRisks);
    }

    /**
     * Расчет пакетной скидки
     */
    private BundleDiscountResult calculateBundleDiscount(
            List<String> selectedRisks,
            BigDecimal premiumAmount,
            java.time.LocalDate agreementDate) {

        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        var bestBundleOpt = riskBundleService.getBestApplicableBundle(
                selectedRisks,
                agreementDate
        );

        if (bestBundleOpt.isEmpty()) {
            log.debug("No applicable bundle found");
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        var bundle = bestBundleOpt.get();
        BigDecimal discountAmount = riskBundleService.calculateBundleDiscount(
                premiumAmount,
                bundle
        );

        log.info("Applied bundle '{}' with {}% discount = {} EUR",
                bundle.code(), bundle.discountPercentage(), discountAmount);

        return new BundleDiscountResult(bundle, discountAmount);
    }

    /**
     * Детали расчета рисков (ОБНОВЛЕНО для возрастных модификаторов)
     */
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

        // Базовый медицинский риск (всегда включен)
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
                BigDecimal.ONE  // без возрастного модификатора
        ));

        // Дополнительные риски (с возрастными модификаторами)
        if (selectedRiskCodes != null) {
            for (String riskCode : selectedRiskCodes) {
                var riskOpt = riskTypeRepository.findActiveByCode(riskCode, agreementDate);
                if (riskOpt.isPresent() && !riskOpt.get().getIsMandatory()) {
                    var risk = riskOpt.get();

                    // Получаем возрастной модификатор
                    BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                            riskCode, age, agreementDate
                    );

                    // Модифицированный коэффициент
                    BigDecimal modifiedCoefficient = risk.getCoefficient().multiply(ageModifier);

                    BigDecimal riskPremium = basePremium
                            .multiply(modifiedCoefficient)
                            .setScale(2, RoundingMode.HALF_UP);

                    details.add(new RiskPremiumDetail(
                            risk.getCode(),
                            risk.getNameEn(),
                            riskPremium,
                            risk.getCoefficient(),
                            ageModifier
                    ));
                }
            }
        }

        return details;
    }

    /**
     * Шаги расчета
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

        steps.add(new CalculationStep(
                "Base rate per day",
                "Base Rate",
                baseRate
        ));

        steps.add(new CalculationStep(
                "Age coefficient",
                String.format("Base Rate × Age Coeff = %.2f × %.2f",
                        baseRate, ageCoefficient),
                baseRate.multiply(ageCoefficient)
        ));

        steps.add(new CalculationStep(
                "Country risk coefficient",
                String.format("Previous × Country Coeff = %.2f × %.2f",
                        baseRate.multiply(ageCoefficient), countryCoefficient),
                baseRate.multiply(ageCoefficient).multiply(countryCoefficient)
        ));

        steps.add(new CalculationStep(
                "Trip duration coefficient",
                String.format("Previous × Duration Coeff = %.2f × %.2f",
                        baseRate.multiply(ageCoefficient).multiply(countryCoefficient),
                        durationCoefficient),
                baseRate.multiply(ageCoefficient)
                        .multiply(countryCoefficient)
                        .multiply(durationCoefficient)
        ));

        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Additional risks coefficient (age-modified)",
                    String.format("Previous × (1 + %.2f)", additionalRisksCoefficient),
                    baseRate.multiply(ageCoefficient)
                            .multiply(countryCoefficient)
                            .multiply(durationCoefficient)
                            .multiply(BigDecimal.ONE.add(additionalRisksCoefficient))
            ));
        }

        steps.add(new CalculationStep(
                "Multiply by number of days",
                String.format("Previous × %d days", days),
                basePremium
        ));

        if (bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Bundle discount",
                    String.format("Previous - Bundle Discount = %.2f - %.2f",
                            basePremium, bundleDiscount),
                    finalPremium
            ));
        }

        return steps;
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    /**
     * Результат расчета дополнительных рисков с модификаторами
     */
    private record AdditionalRisksCalculation(
            BigDecimal totalCoefficient,
            List<ModifiedRiskDetail> modifiedRisks
    ) {}

    /**
     * Детали модифицированного риска
     */
    private record ModifiedRiskDetail(
            String riskCode,
            BigDecimal baseCoefficient,
            BigDecimal ageModifier,
            BigDecimal modifiedCoefficient
    ) {}

    /**
     * Результат пакетной скидки
     */
    public record BundleDiscountResult(
            RiskBundleService.ApplicableBundleResult bundle,
            BigDecimal discountAmount
    ) {}

    /**
     * Результат расчета премии (ОБНОВЛЕНО)
     */
    public record PremiumCalculationResult(
            BigDecimal premium,
            BigDecimal baseRate,
            int age,
            BigDecimal ageCoefficient,
            String ageGroupDescription,
            BigDecimal countryCoefficient,
            String countryName,
            BigDecimal durationCoefficient,  // 🆕
            BigDecimal additionalRisksCoefficient,
            BigDecimal totalCoefficient,
            int days,
            BigDecimal coverageAmount,
            List<RiskPremiumDetail> riskDetails,
            BundleDiscountResult bundleDiscount,  // 🆕
            List<CalculationStep> calculationSteps
    ) {}

    /**
     * Детали премии по риску
     */
    public record RiskPremiumDetail(
            String riskCode,
            String riskName,
            BigDecimal premium,
            BigDecimal coefficient,
            BigDecimal ageModifier  // 🆕
    ) {}

    public record CalculationStep(
            String description,
            String formula,
            BigDecimal result
    ) {}
}