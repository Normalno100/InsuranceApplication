package org.javaguru.travel.insurance.application.assembler;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Утилитный компонент для построения строки формулы расчёта премии.
 *
 * РЕФАКТОРИНГ (п. 4.2 плана):
 *   ДО: логика buildFormula дублировалась в двух местах:
 *     1. ResponseAssembler.buildFormula()         — используется
 *     2. CountryDefaultDayPremiumService.buildFormula() — dead code
 *
 *   ПОСЛЕ:
 *     - Единственная реализация централизована здесь.
 *     - CountryDefaultDayPremiumService.buildFormula() — удалён (dead code).
 *     - ResponseAssembler делегирует вызов этому компоненту.
 *
 * ПОДДЕРЖИВАЕМЫЕ РЕЖИМЫ:
 *   MEDICAL_LEVEL   — формула с baseRate (daily rate) + countryCoefficient
 *   COUNTRY_DEFAULT — формула с countryDefaultRate (country risk включён в ставку)
 */
@Component
public class FormulaBuilder {

    /**
     * Строит строку формулы расчёта на основе результата калькулятора.
     *
     * @param result полный результат расчёта премии
     * @return человекочитаемая строка формулы, например:
     *         "Premium = 4.50 (daily rate) × 1.1000 (age) × 1.0000 (country) × 1.0000 (duration) × 14 days"
     */
    public String build(MedicalRiskPremiumCalculator.PremiumCalculationResult result) {
        boolean isCountryDefault = result.calculationMode()
                == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        StringBuilder formula = new StringBuilder("Premium = ")
                .append(String.format("%.2f", result.baseRate()));

        if (isCountryDefault) {
            formula.append(" (country default rate)");
        } else {
            formula.append(" (daily rate)");
        }

        formula.append(" × ")
                .append(String.format("%.4f", result.ageCoefficient()))
                .append(" (age)");

        if (!isCountryDefault) {
            formula.append(" × ")
                    .append(String.format("%.4f", result.countryCoefficient()))
                    .append(" (country)");
        }

        formula.append(" × ")
                .append(String.format("%.4f", result.durationCoefficient()))
                .append(" (duration)");

        if (result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.4f", result.additionalRisksCoefficient()))
                    .append(") (risks)");
        }

        formula.append(" × ").append(result.days()).append(" days");

        if (result.bundleDiscount() != null
                && result.bundleDiscount().discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" - ")
                    .append(String.format("%.2f", result.bundleDiscount().discountAmount()))
                    .append(" (bundle discount)");
        }

        return formula.toString();
    }
}