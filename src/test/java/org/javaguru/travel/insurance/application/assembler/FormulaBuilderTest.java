package org.javaguru.travel.insurance.application.assembler;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit-тесты для FormulaBuilder.
 *
 * РЕФАКТОРИНГ (п. 4.2 плана): проверяем что централизованная логика
 * построения формулы корректна для обоих режимов расчёта.
 */
@DisplayName("FormulaBuilder")
class FormulaBuilderTest {

    private final FormulaBuilder formulaBuilder = new FormulaBuilder();

    // =========================================================
    // MEDICAL_LEVEL режим
    // =========================================================

    @Nested
    @DisplayName("MEDICAL_LEVEL mode")
    class MedicalLevelModeTests {

        @Test
        @DisplayName("should build formula with all components")
        void shouldBuildFormulaWithAllComponents() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.1000"),
                    new BigDecimal("1.3000"),
                    new BigDecimal("0.9500"),
                    new BigDecimal("0.3000"),
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("(daily rate)");
            assertThat(formula).contains("(age)");
            assertThat(formula).contains("(country)");
            assertThat(formula).contains("(duration)");
            assertThat(formula).contains("14 days");
        }

        @Test
        @DisplayName("should include base rate formatted to 2 decimals")
        void shouldIncludeBaseRateFormatted() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.1000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    7,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).startsWith("Premium = 4,50 (daily rate)");
        }

        @Test
        @DisplayName("should include country coefficient for MEDICAL_LEVEL")
        void shouldIncludeCountryCoefficientForMedicalLevel() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("7.00"),
                    new BigDecimal("1.3000"),
                    new BigDecimal("1.3000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    10,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("1,3000 (country)");
        }

        @Test
        @DisplayName("should include additional risks when coefficient > 0")
        void shouldIncludeAdditionalRisksWhenPresent() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("0.3000"),
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("(1 + 0,3000) (risks)");
        }

        @Test
        @DisplayName("should NOT include additional risks when coefficient is zero")
        void shouldNotIncludeAdditionalRisksWhenZero() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).doesNotContain("(risks)");
        }

        @Test
        @DisplayName("should include bundle discount when present")
        void shouldIncludeBundleDiscountWhenPresent() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    14,
                    new BigDecimal("10.50")
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("- 10,50 (bundle discount)");
        }

        @Test
        @DisplayName("should NOT include bundle discount when zero")
        void shouldNotIncludeBundleDiscountWhenZero() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).doesNotContain("bundle discount");
        }
    }

    // =========================================================
    // COUNTRY_DEFAULT режим
    // =========================================================

    @Nested
    @DisplayName("COUNTRY_DEFAULT mode")
    class CountryDefaultModeTests {

        @Test
        @DisplayName("should use 'country default rate' label instead of 'daily rate'")
        void shouldUseCountryDefaultRateLabel() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT,
                    new BigDecimal("6.00"),
                    new BigDecimal("1.1000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("0.9500"),
                    BigDecimal.ZERO,
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("(country default rate)");
            assertThat(formula).doesNotContain("(daily rate)");
        }

        @Test
        @DisplayName("should NOT include country coefficient for COUNTRY_DEFAULT")
        void shouldNotIncludeCountryCoefficientForCountryDefault() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT,
                    new BigDecimal("6.00"),
                    new BigDecimal("1.1000"),
                    new BigDecimal("1.3000"),  // country coeff — не должен попасть в формулу
                    new BigDecimal("0.9500"),
                    BigDecimal.ZERO,
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).doesNotContain("(country)");
        }

        @Test
        @DisplayName("should include age and duration coefficients")
        void shouldIncludeAgeAndDurationCoefficients() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT,
                    new BigDecimal("8.00"),
                    new BigDecimal("1.6000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("0.9000"),
                    BigDecimal.ZERO,
                    21,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("1,6000 (age)");
            assertThat(formula).contains("0,9000 (duration)");
            assertThat(formula).contains("21 days");
        }
    }

    // =========================================================
    // Общие проверки структуры формулы
    // =========================================================

    @Nested
    @DisplayName("Formula structure")
    class FormulaStructureTests {

        @Test
        @DisplayName("formula should always start with 'Premium = '")
        void shouldAlwaysStartWithPremiumEquals() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    7,
                    BigDecimal.ZERO
            );

            assertThat(formulaBuilder.build(result)).startsWith("Premium = ");
        }

        @Test
        @DisplayName("formula should always contain trip days")
        void shouldAlwaysContainTripDays() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("2.00"),
                    new BigDecimal("0.9000"),
                    new BigDecimal("1.0000"),
                    new BigDecimal("1.0000"),
                    BigDecimal.ZERO,
                    3,
                    BigDecimal.ZERO
            );

            assertThat(formulaBuilder.build(result)).contains("3 days");
        }

        @Test
        @DisplayName("coefficients should be formatted to 4 decimal places")
        void shouldFormatCoefficientsTo4Decimals() {
            var result = buildResult(
                    MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                    new BigDecimal("4.50"),
                    new BigDecimal("1.1"),
                    new BigDecimal("1.3"),
                    new BigDecimal("0.95"),
                    BigDecimal.ZERO,
                    14,
                    BigDecimal.ZERO
            );

            String formula = formulaBuilder.build(result);

            assertThat(formula).contains("1,1000 (age)");
            assertThat(formula).contains("1,3000 (country)");
            assertThat(formula).contains("0,9500 (duration)");
        }
    }

    // =========================================================
    // вспомогательный метод
    // =========================================================

    private MedicalRiskPremiumCalculator.PremiumCalculationResult buildResult(
            MedicalRiskPremiumCalculator.CalculationMode mode,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            BigDecimal additionalRisksCoefficient,
            int days,
            BigDecimal bundleDiscountAmount) {

        var bundleDiscount = new MedicalRiskPremiumCalculator.BundleDiscountResult(
                bundleDiscountAmount.compareTo(BigDecimal.ZERO) > 0
                        ? new RiskBundleService.ApplicableBundleResult(
                        "TEST_BUNDLE", "Test Bundle", new BigDecimal("10"), List.of())
                        : null,
                bundleDiscountAmount
        );

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                new BigDecimal("100.00"),  // premium
                baseRate,
                35,                         // age
                ageCoefficient,
                "Adults",
                countryCoefficient,
                "Spain",
                durationCoefficient,
                additionalRisksCoefficient,
                new BigDecimal("1.0000"),   // totalCoefficient
                days,
                new BigDecimal("50000"),    // coverageAmount
                List.of(),                  // riskDetails
                bundleDiscount,
                List.of(),                  // calculationSteps
                mode,
                null,                       // countryDefaultDayPremium
                null,                       // countryDefaultDayPremiumForInfo
                "EUR",
                null,                       // medicalPayoutLimit
                null,                       // appliedPayoutLimit
                false                       // payoutLimitApplied
        );
    }
}