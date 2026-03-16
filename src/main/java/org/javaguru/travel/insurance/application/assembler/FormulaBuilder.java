package org.javaguru.travel.insurance.application.assembler;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Утилитный компонент для построения строки формулы расчёта премии.
 *
 * РЕФАКТОРИНГ (п. 4.2 плана):
 *   Логика buildFormula централизована здесь.
 *   CountryDefaultDayPremiumService.buildFormula() был dead code — удалён.
 *
 * РЕФАКТОРИНГ (п. 4.3 плана):
 *   Обновлён для работы с декомпозированным PremiumCalculationResult.
 *   Вместо прямых полей используются вложенные records:
 *     result.ageDetails().ageCoefficient()
 *     result.countryDetails().countryCoefficient()
 *     result.tripDetails().durationCoefficient()
 *     и т.д.
 */
@Component
public class FormulaBuilder {

    /**
     * Строит строку формулы расчёта на основе результата калькулятора.
     *
     * @param result полный результат расчёта премии
     * @return человекочитаемая строка формулы
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
                .append(String.format("%.4f", result.ageDetails().ageCoefficient()))
                .append(" (age)");

        if (!isCountryDefault) {
            formula.append(" × ")
                    .append(String.format("%.4f", result.countryDetails().countryCoefficient()))
                    .append(" (country)");
        }

        formula.append(" × ")
                .append(String.format("%.4f", result.tripDetails().durationCoefficient()))
                .append(" (duration)");

        if (result.tripDetails().additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.4f", result.tripDetails().additionalRisksCoefficient()))
                    .append(") (risks)");
        }

        formula.append(" × ").append(result.tripDetails().days()).append(" days");

        MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount =
                result.riskDetails().bundleDiscount();

        if (bundleDiscount != null
                && bundleDiscount.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" - ")
                    .append(String.format("%.2f", bundleDiscount.discountAmount()))
                    .append(" (bundle discount)");
        }

        return formula.toString();
    }
}