package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.CalculationStep;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Построитель шагов расчёта для отображения в ответе.
 *
 * ИЗМЕНЕНИЯ task_117:
 * - buildMedicalLevelSteps принимает параметры payoutLimit и rawBasePremium.
 *   Если лимит выплат был применён — добавляется отдельный шаг.
 */
@Component
public class CalculationStepsBuilder {

    /**
     * Строит шаги расчёта для режима MEDICAL_LEVEL.
     *
     * task_117: если payoutLimit != null, добавляется шаг коррекции премии по лимиту.
     *
     * @param payoutLimit     применённый лимит выплат (null если не применялся)
     * @param rawBasePremium  базовая премия ДО применения лимита выплат
     */
    public List<CalculationStep> buildMedicalLevelSteps(
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal basePremium,
            BigDecimal bundleDiscount,
            BigDecimal finalPremium,
            BigDecimal payoutLimit,
            BigDecimal rawBasePremium) {

        List<CalculationStep> steps = new ArrayList<>();

        // Шаг 1: Базовая дневная ставка
        steps.add(new CalculationStep(
                "Base daily rate (medical level)",
                String.format("Daily Rate = %.2f EUR", baseRate),
                baseRate));

        // Шаг 2: Возрастной коэффициент
        BigDecimal afterAge = baseRate.multiply(ageCoefficient);
        steps.add(new CalculationStep(
                "Age coefficient applied",
                String.format("%.2f × %.4f (age coeff) = %.4f", baseRate, ageCoefficient, afterAge),
                afterAge));

        // Шаг 3: Коэффициент страны
        BigDecimal afterCountry = afterAge.multiply(countryCoefficient);
        steps.add(new CalculationStep(
                "Country risk coefficient applied",
                String.format("%.4f × %.4f (country coeff) = %.4f",
                        afterAge, countryCoefficient, afterCountry),
                afterCountry));

        // Шаг 4: Коэффициент длительности
        BigDecimal afterDuration = afterCountry.multiply(durationCoefficient);
        steps.add(new CalculationStep(
                "Duration coefficient applied",
                String.format("%.4f × %.4f (duration coeff) = %.4f",
                        afterCountry, durationCoefficient, afterDuration),
                afterDuration));

        // Шаг 5: Дополнительные риски (если есть)
        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afterRisks = afterDuration.multiply(
                    BigDecimal.ONE.add(additionalRisksCoefficient));
            steps.add(new CalculationStep(
                    "Additional risks (age-modified)",
                    String.format("%.4f × (1 + %.4f) = %.4f",
                            afterDuration, additionalRisksCoefficient, afterRisks),
                    afterRisks));
        }

        // Шаг 6: Умножение на количество дней
        BigDecimal premiumBeforeLimit = payoutLimit != null ? rawBasePremium : basePremium;
        steps.add(new CalculationStep(
                "Multiply by trip days",
                String.format("× %d days = %.2f EUR", days, premiumBeforeLimit),
                premiumBeforeLimit));

        // Шаг 7 (task_117): Применение лимита выплат (если был применён)
        if (payoutLimit != null) {
            steps.add(new CalculationStep(
                    "Payout limit correction applied",
                    String.format("%.2f × (payoutLimit %.2f / coverage) = %.2f EUR",
                            rawBasePremium, payoutLimit, basePremium),
                    basePremium));
        }

        // Шаг 8: Пакетная скидка (если есть)
        if (bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Bundle discount applied",
                    String.format("%.2f - %.2f (bundle discount) = %.2f EUR",
                            basePremium, bundleDiscount, finalPremium),
                    finalPremium));
        }

        return steps;
    }

    /**
     * Перегрузка без параметров лимита (обратная совместимость).
     */
    public List<CalculationStep> buildMedicalLevelSteps(
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal basePremium,
            BigDecimal bundleDiscount,
            BigDecimal finalPremium) {
        return buildMedicalLevelSteps(baseRate, ageCoefficient, countryCoefficient,
                durationCoefficient, additionalRisksCoefficient, days, basePremium,
                bundleDiscount, finalPremium, null, basePremium);
    }

    /**
     * Строит шаги расчёта для режима COUNTRY_DEFAULT.
     * Лимит выплат в этом режиме не применяется.
     */
    public List<CalculationStep> buildCountryDefaultSteps(
            BigDecimal defaultDayPremium,
            BigDecimal ageCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal basePremium,
            BigDecimal bundleDiscount,
            BigDecimal finalPremium) {

        List<CalculationStep> steps = new ArrayList<>();

        // Шаг 1: Дефолтная дневная ставка страны
        steps.add(new CalculationStep(
                "Country default day premium (country risk already included)",
                String.format("Default Day Rate = %.2f EUR", defaultDayPremium),
                defaultDayPremium));

        // Шаг 2: Возрастной коэффициент
        BigDecimal afterAge = defaultDayPremium.multiply(ageCoefficient);
        steps.add(new CalculationStep(
                "Age coefficient applied",
                String.format("%.2f × %.4f (age coeff) = %.4f", defaultDayPremium, ageCoefficient, afterAge),
                afterAge));

        // Шаг 3: Коэффициент длительности
        BigDecimal afterDuration = afterAge.multiply(durationCoefficient);
        steps.add(new CalculationStep(
                "Duration coefficient applied [country coeff is baked into base rate]",
                String.format("%.4f × %.4f (duration coeff) = %.4f",
                        afterAge, durationCoefficient, afterDuration),
                afterDuration));

        // Шаг 4: Дополнительные риски (если есть)
        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal afterRisks = afterDuration.multiply(
                    BigDecimal.ONE.add(additionalRisksCoefficient));
            steps.add(new CalculationStep(
                    "Additional risks (age-modified)",
                    String.format("%.4f × (1 + %.4f) = %.4f",
                            afterDuration, additionalRisksCoefficient, afterRisks),
                    afterRisks));
        }

        // Шаг 5: Умножение на количество дней
        steps.add(new CalculationStep(
                "Multiply by trip days",
                String.format("× %d days = %.2f EUR", days, basePremium),
                basePremium));

        // Шаг 6: Пакетная скидка (если есть)
        if (bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Bundle discount applied",
                    String.format("%.2f - %.2f (bundle discount) = %.2f EUR",
                            basePremium, bundleDiscount, finalPremium),
                    finalPremium));
        }

        return steps;
    }
}