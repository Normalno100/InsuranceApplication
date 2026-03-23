package org.javaguru.travel.insurance.application.assembler;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты FormulaBuilder — построение строки формулы расчёта.
 *
 * ЭТАП 6 (рефакторинг): Добавление отсутствующих тестов.
 *
 * ПОКРЫВАЕМ:
 *   - build() для режима MEDICAL_LEVEL: baseRate × ageCoeff × countryCoeff × durationCoeff × days
 *   - build() для режима COUNTRY_DEFAULT: без countryCoeff (уже в baseRate)
 *   - Дополнительные риски (additionalRisksCoefficient > 0): включаются в формулу
 *   - Bundle discount > 0: включается в формулу
 *   - Все коэффициенты = 1.0, нет рисков, нет скидки — минимальная формула
 */
@DisplayName("FormulaBuilder")
class FormulaBuilderTest {

    private final FormulaBuilder formulaBuilder = new FormulaBuilder();

    // ── MEDICAL_LEVEL режим ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Режим MEDICAL_LEVEL")
    class MedicalLevelMode {

        @Test
        @DisplayName("формула должна начинаться с 'Premium ='")
        void formulaShouldStartWithPremiumEquals() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).startsWith("Premium =");
        }

        @Test
        @DisplayName("формула должна содержать 'daily rate' в скобках")
        void formulaShouldContainDailyRateLabel() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).containsIgnoringCase("daily rate");
        }

        @Test
        @DisplayName("формула должна содержать возрастной коэффициент")
        void formulaShouldContainAgeCoefficient() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).contains("(age)");
        }

        @Test
        @DisplayName("формула должна содержать коэффициент страны")
        void formulaShouldContainCountryCoefficient() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).contains("(country)");
        }

        @Test
        @DisplayName("формула должна содержать коэффициент длительности")
        void formulaShouldContainDurationCoefficient() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).contains("(duration)");
        }

        @Test
        @DisplayName("формула должна содержать количество дней")
        void formulaShouldContainDays() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).contains("14 days");
        }

        @Test
        @DisplayName("формула должна включать дополнительные риски когда они > 0")
        void formulaShouldIncludeAdditionalRisksWhenPositive() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    new BigDecimal("0.30"), null));

            assertThat(formula).contains("(risks)");
        }

        @Test
        @DisplayName("формула НЕ должна включать риски когда additionalRisksCoefficient = 0")
        void formulaShouldNotIncludeRisksWhenZero() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, null));

            assertThat(formula).doesNotContain("risks");
        }

        @Test
        @DisplayName("формула должна включать bundle discount когда он > 0")
        void formulaShouldIncludeBundleDiscountWhenPositive() {
            MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount =
                    new MedicalRiskPremiumCalculator.BundleDiscountResult(null, new BigDecimal("5.25"));

            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, bundleDiscount));

            assertThat(formula).contains("bundle discount");
        }

        @Test
        @DisplayName("формула НЕ должна включать bundle discount когда он = 0")
        void formulaShouldNotIncludeBundleDiscountWhenZero() {
            MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount =
                    new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO);

            String formula = formulaBuilder.build(buildMedicalLevelResult(
                    BigDecimal.ZERO, bundleDiscount));

            assertThat(formula).doesNotContain("bundle discount");
        }
    }

    // ── COUNTRY_DEFAULT режим ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Режим COUNTRY_DEFAULT")
    class CountryDefaultMode {

        @Test
        @DisplayName("формула должна содержать 'country default rate'")
        void formulaShouldContainCountryDefaultRateLabel() {
            String formula = formulaBuilder.build(buildCountryDefaultResult(BigDecimal.ZERO, null));

            assertThat(formula).containsIgnoringCase("country default rate");
        }

        @Test
        @DisplayName("формула НЕ должна содержать отдельный country коэффициент")
        void formulaShouldNotContainSeparateCountryCoefficient() {
            String formula = formulaBuilder.build(buildCountryDefaultResult(BigDecimal.ZERO, null));

            assertThat(formula).doesNotContain("(country)");
        }

        @Test
        @DisplayName("формула должна содержать возрастной коэффициент")
        void formulaShouldContainAgeCoefficient() {
            String formula = formulaBuilder.build(buildCountryDefaultResult(BigDecimal.ZERO, null));

            assertThat(formula).contains("(age)");
        }

        @Test
        @DisplayName("формула должна содержать коэффициент длительности")
        void formulaShouldContainDurationCoefficient() {
            String formula = formulaBuilder.build(buildCountryDefaultResult(BigDecimal.ZERO, null));

            assertThat(formula).contains("(duration)");
        }

        @Test
        @DisplayName("формула должна включать дополнительные риски в COUNTRY_DEFAULT")
        void formulaShouldIncludeAdditionalRisksInCountryDefaultMode() {
            String formula = formulaBuilder.build(
                    buildCountryDefaultResult(new BigDecimal("0.20"), null));

            assertThat(formula).contains("risks");
        }

        @Test
        @DisplayName("формула должна включать bundle discount в COUNTRY_DEFAULT")
        void formulaShouldIncludeBundleDiscountInCountryDefaultMode() {
            MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount =
                    new MedicalRiskPremiumCalculator.BundleDiscountResult(null, new BigDecimal("8.00"));

            String formula = formulaBuilder.build(
                    buildCountryDefaultResult(BigDecimal.ZERO, bundleDiscount));

            assertThat(formula).contains("bundle discount");
        }
    }

    // ── Общие свойства формулы ────────────────────────────────────────────────

    @Nested
    @DisplayName("Общие свойства формулы")
    class GeneralProperties {

        @Test
        @DisplayName("формула не должна быть null")
        void formulaShouldNotBeNull() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(BigDecimal.ZERO, null));

            assertThat(formula).isNotNull();
        }

        @Test
        @DisplayName("формула не должна быть пустой строкой")
        void formulaShouldNotBeEmpty() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(BigDecimal.ZERO, null));

            assertThat(formula).isNotBlank();
        }

        @Test
        @DisplayName("формула должна содержать знак умножения ×")
        void formulaShouldContainMultiplicationSign() {
            String formula = formulaBuilder.build(buildMedicalLevelResult(BigDecimal.ZERO, null));

            assertThat(formula).contains("×");
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private MedicalRiskPremiumCalculator.PremiumCalculationResult buildMedicalLevelResult(
            BigDecimal additionalRisksCoeff,
            MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount) {

        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(
                35, new BigDecimal("1.10"), "Adults");

        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Spain",
                new BigDecimal("1.0000"),  // LOW risk
                null,
                null,
                "EUR");

        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14,
                new BigDecimal("0.9500"),  // duration coefficient
                additionalRisksCoeff,
                new BigDecimal("1.0450"),
                new BigDecimal("50000.00"));

        MedicalRiskPremiumCalculator.BundleDiscountResult effectiveBundleDiscount =
                bundleDiscount != null
                        ? bundleDiscount
                        : new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO);

        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(
                List.of(), effectiveBundleDiscount);

        var payoutLimitDetails = new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                new BigDecimal("50000.00"), new BigDecimal("50000.00"), false);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                new BigDecimal("62.70"),
                new BigDecimal("4.50"),   // baseRate: 50000 уровень
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                List.of(),
                payoutLimitDetails);
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult buildCountryDefaultResult(
            BigDecimal additionalRisksCoeff,
            MedicalRiskPremiumCalculator.BundleDiscountResult bundleDiscount) {

        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(
                35, new BigDecimal("1.10"), "Adults");

        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Germany",
                new BigDecimal("1.0000"),
                new BigDecimal("6.50"),   // countryDefaultDayPremium
                new BigDecimal("6.50"),
                "EUR");

        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14,
                new BigDecimal("0.9500"),
                additionalRisksCoeff,
                new BigDecimal("1.0450"),
                null);   // нет coverageAmount в COUNTRY_DEFAULT

        MedicalRiskPremiumCalculator.BundleDiscountResult effectiveBundleDiscount =
                bundleDiscount != null
                        ? bundleDiscount
                        : new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO);

        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(
                List.of(), effectiveBundleDiscount);

        var payoutLimitDetails = new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                null, null, false);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                new BigDecimal("95.09"),
                new BigDecimal("6.50"),   // baseRate = countryDefaultDayPremium
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT,
                List.of(),
                payoutLimitDetails);
    }
}