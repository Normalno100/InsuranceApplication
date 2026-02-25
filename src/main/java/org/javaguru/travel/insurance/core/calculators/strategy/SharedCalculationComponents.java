package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.RiskPremiumDetail;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Общие компоненты расчёта, используемые обеими стратегиями.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * Устраняет дублирование между {@link MedicalLevelPremiumStrategy}
 * и {@link CountryDefaultPremiumStrategy}.
 *
 * Содержит:
 *   - Расчёт возраста и коэффициента
 *   - Расчёт длительности
 *   - Расчёт дополнительных рисков с возрастными модификаторами
 *   - Расчёт пакетных скидок
 *   - Построение деталей по рискам
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharedCalculationComponents {

    private final AgeCalculator ageCalculator;
    private final TripDurationPricingService durationPricingService;
    private final AgeRiskPricingService ageRiskPricingService;
    private final RiskBundleService riskBundleService;
    private final RiskTypeRepository riskTypeRepository;

    // ========================================
    // ВОЗРАСТ И КОЭФФИЦИЕНТ
    // ========================================

    /**
     * Рассчитывает возраст и возрастной коэффициент.
     *
     * referenceDate (agreementDateFrom) передаётся в AgeCalculator
     * для выбора актуальной версии коэффициента из БД (age_coefficients).
     */
    public AgeCalculator.AgeCalculationResult calculateAge(
            LocalDate birthDate,
            LocalDate referenceDate) {
        return ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);
    }

    // ========================================
    // ДЛИТЕЛЬНОСТЬ
    // ========================================

    /**
     * Рассчитывает количество дней и коэффициент длительности.
     */
    public long calculateDays(LocalDate dateFrom, LocalDate dateTo) {
        return java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo);
    }

    public BigDecimal getDurationCoefficient(long days, LocalDate date) {
        return durationPricingService.getDurationCoefficient((int) days, date);
    }

    // ========================================
    // ДОПОЛНИТЕЛЬНЫЕ РИСКИ
    // ========================================

    /**
     * Рассчитывает суммарный коэффициент дополнительных рисков
     * с учётом возрастных модификаторов.
     *
     * Результат используется как addend: (1 + additionalRisksCoeff)
     */
    public AdditionalRisksResult calculateAdditionalRisks(
            List<String> selectedRiskCodes,
            int age,
            LocalDate agreementDate) {

        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return new AdditionalRisksResult(BigDecimal.ZERO, new ArrayList<>());
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

        return new AdditionalRisksResult(totalCoefficient, modifiedRisks);
    }

    // ========================================
    // ПАКЕТНАЯ СКИДКА
    // ========================================

    public BundleDiscountResult calculateBundleDiscount(
            List<String> selectedRisks,
            BigDecimal premiumAmount,
            LocalDate agreementDate) {

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

    // ========================================
    // ДЕТАЛИ ПО РИСКАМ
    // ========================================

    /**
     * Строит список деталей по каждому риску для ответа.
     *
     * @param countryCoefficient передаётся BigDecimal.ONE в режиме COUNTRY_DEFAULT
     */
    public List<RiskPremiumDetail> buildRiskDetails(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            int days,
            int age,
            LocalDate agreementDate) {

        List<RiskPremiumDetail> details = new ArrayList<>();

        // Базовая премия по TRAVEL_MEDICAL
        BigDecimal basePremium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        var medicalRisk = riskTypeRepository.findActiveByCode("TRAVEL_MEDICAL", agreementDate)
                .orElseThrow(() -> new IllegalStateException("TRAVEL_MEDICAL risk not found"));

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

    // ========================================
    // ВЛОЖЕННЫЕ ТИПЫ
    // ========================================

    public record AdditionalRisksResult(
            BigDecimal totalCoefficient,
            List<ModifiedRiskDetail> modifiedRisks
    ) {}

    public record ModifiedRiskDetail(
            String riskCode,
            BigDecimal baseCoefficient,
            BigDecimal ageModifier,
            BigDecimal modifiedCoefficient
    ) {}
}
