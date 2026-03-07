package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.RiskPremiumDetail;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Фасад общих компонентов расчёта.
 *
 * ИЗМЕНЕНИЯ (п. 3.2 плана рефакторинга — декомпозиция God Component):
 *
 *   ДО: SharedCalculationComponents выполнял 5 разных ответственностей:
 *     1. Расчёт возраста и коэффициента       → PersonAgeCalculator
 *     2. Расчёт длительности поездки           → TripDurationCalculator
 *     3. Расчёт дополнительных рисков          → AdditionalRisksCalculator
 *     4. Расчёт пакетной скидки                → BundleDiscountCalculator
 *     5. Построение деталей рисков для ответа  → RiskDetailsBuilder
 *
 *   ПОСЛЕ: каждая ответственность вынесена в отдельный компонент (SRP).
 *   Данный класс сохранён как тонкий фасад для обратной совместимости
 *   со стратегиями (MedicalLevelPremiumStrategy, CountryDefaultPremiumStrategy),
 *   которые могут быть обновлены постепенно.
 *
 * ПЛАН МИГРАЦИИ:
 *   В следующем PR стратегии следует обновить так, чтобы они зависели
 *   непосредственно от нужных калькуляторов, а SharedCalculationComponents
 *   можно будет удалить.
 *
 * ИСПРАВЛЕНИЕ DIP (п. 3.1):
 *   Прежняя реализация зависела от RiskTypeRepository (infrastructure).
 *   Теперь зависимость идёт через domain-port внутри AdditionalRisksCalculator
 *   и RiskDetailsBuilder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SharedCalculationComponents {

    private final PersonAgeCalculator personAgeCalculator;
    private final TripDurationCalculator tripDurationCalculator;
    private final AdditionalRisksCalculator additionalRisksCalculator;
    private final BundleDiscountCalculator bundleDiscountCalculator;
    private final RiskDetailsBuilder riskDetailsBuilder;

    // ========================================
    // ВОЗРАСТ И КОЭФФИЦИЕНТ
    // ========================================

    /** @see PersonAgeCalculator#calculate(LocalDate, LocalDate) */
    public AgeCalculator.AgeCalculationResult calculateAge(
            LocalDate birthDate,
            LocalDate referenceDate) {
        return personAgeCalculator.calculate(birthDate, referenceDate);
    }

    /** @see PersonAgeCalculator#calculate(LocalDate, LocalDate, boolean) */
    public AgeCalculator.AgeCalculationResult calculateAge(
            LocalDate birthDate,
            LocalDate referenceDate,
            boolean ageCoefficientEnabled) {
        return personAgeCalculator.calculate(birthDate, referenceDate, ageCoefficientEnabled);
    }

    // ========================================
    // ДЛИТЕЛЬНОСТЬ
    // ========================================

    /** @see TripDurationCalculator#calculateDays(LocalDate, LocalDate) */
    public long calculateDays(LocalDate dateFrom, LocalDate dateTo) {
        return tripDurationCalculator.calculateDays(dateFrom, dateTo);
    }

    /** @see TripDurationCalculator#getDurationCoefficient(long, LocalDate) */
    public BigDecimal getDurationCoefficient(long days, LocalDate date) {
        return tripDurationCalculator.getDurationCoefficient(days, date);
    }

    // ========================================
    // ДОПОЛНИТЕЛЬНЫЕ РИСКИ
    // ========================================

    /** @see AdditionalRisksCalculator#calculate(List, int, LocalDate) */
    public AdditionalRisksResult calculateAdditionalRisks(
            List<String> selectedRiskCodes,
            int age,
            LocalDate agreementDate) {

        var result = additionalRisksCalculator.calculate(selectedRiskCodes, age, agreementDate);
        // Адаптируем тип из нового калькулятора к локальному record (сохраняем API)
        return new AdditionalRisksResult(
                result.totalCoefficient(),
                result.modifiedRisks().stream()
                        .map(d -> new ModifiedRiskDetail(
                                d.riskCode(),
                                d.baseCoefficient(),
                                d.ageModifier(),
                                d.modifiedCoefficient()))
                        .toList()
        );
    }

    // ========================================
    // ПАКЕТНАЯ СКИДКА
    // ========================================

    /** @see BundleDiscountCalculator#calculate(List, BigDecimal, LocalDate) */
    public BundleDiscountResult calculateBundleDiscount(
            List<String> selectedRisks,
            BigDecimal premiumAmount,
            LocalDate agreementDate) {
        return bundleDiscountCalculator.calculate(selectedRisks, premiumAmount, agreementDate);
    }

    // ========================================
    // ДЕТАЛИ ПО РИСКАМ
    // ========================================

    /** @see RiskDetailsBuilder#build(List, BigDecimal, BigDecimal, BigDecimal, BigDecimal, int, int, LocalDate) */
    public List<RiskPremiumDetail> buildRiskDetails(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            int days,
            int age,
            LocalDate agreementDate) {

        return riskDetailsBuilder.build(
                selectedRiskCodes,
                baseRate,
                ageCoefficient,
                countryCoefficient,
                durationCoefficient,
                days,
                age,
                agreementDate);
    }

    // ========================================
    // ВЛОЖЕННЫЕ ТИПЫ (сохранены для обратной совместимости)
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