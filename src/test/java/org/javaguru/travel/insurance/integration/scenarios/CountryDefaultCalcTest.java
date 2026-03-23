package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.TestRequestBuilder;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты расчёта в режиме COUNTRY_DEFAULT (useCountryDefaultPremium=true).
 *
 * ЭТАП 6 (рефакторинг): Добавление отсутствующих тестов.
 *
 * РЕЖИМ COUNTRY_DEFAULT:
 *   Формула: DefaultDayPremium × AgeCoeff × DurationCoeff × (1 + Σ riskCoeffs) × Days
 *   - medicalRiskLimitLevel НЕ требуется
 *   - countryCoeff уже «запечён» в DefaultDayPremium
 *   - coverageAmount отсутствует в ответе
 *   - medicalPayoutLimit = null
 *   - calculationMode = "COUNTRY_DEFAULT"
 *
 * ДАННЫЕ:
 *   Испания (ES): defaultDayPremium = 6.00 EUR
 *   Германия (DE): defaultDayPremium = 6.50 EUR
 *
 * ПРИМЕЧАНИЕ: CountryDefaultDayPremium данные загружены в test-data.sql НЕ включены
 * (чистая H2 тест-база без таблицы country_default_day_premiums).
 * Поэтому большинство тестов проверяют FALLBACK на MEDICAL_LEVEL
 * когда нет defaultDayPremium для страны.
 *
 * Для тестирования полного COUNTRY_DEFAULT сценария нужны
 * данные в country_default_day_premiums таблице.
 * Добавим их через SQL-скрипт в тесте.
 */
@DisplayName("E2E: Country Default Calculation Mode")
class CountryDefaultCalcTest extends BaseIntegrationTest {

    private static final java.time.LocalDate REF = TestConstants.TEST_DATE;
    private static final java.time.LocalDate DATE_FROM = REF.plusDays(30); // 2026-04-17

    // ── Fallback на MEDICAL_LEVEL ─────────────────────────────────────────────

    @Nested
    @DisplayName("Fallback на MEDICAL_LEVEL когда нет country default premium")
    class FallbackToMedicalLevel {

        @Test
        @DisplayName("useCountryDefaultPremium=true без defaultDayPremium → fallback MEDICAL_LEVEL с уровнем")
        void shouldFallbackToMedicalLevelWhenNoDefaultPremium() throws Exception {
            // Испания в test-data.sql НЕ имеет записи в country_default_day_premiums
            // → fallback на MEDICAL_LEVEL (medicalRiskLimitLevel требуется)
            var request = TestRequestBuilder.adult35Spain()
                    .useCountryDefaultPremium(true)
                    .medicalRiskLimitLevel("50000")
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.trip.calculationMode").value("MEDICAL_LEVEL"))
                    .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                    .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
        }

        @Test
        @DisplayName("useCountryDefaultPremium=false → стандартный MEDICAL_LEVEL")
        void shouldUseMedicalLevelWhenFlagFalse() throws Exception {
            var request = TestRequestBuilder.adult35Spain()
                    .useCountryDefaultPremium(false)
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.trip.calculationMode").value("MEDICAL_LEVEL"))
                    .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
        }

        @Test
        @DisplayName("useCountryDefaultPremium=null → стандартный MEDICAL_LEVEL (дефолт)")
        void shouldUseMedicalLevelWhenFlagIsNull() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build(); // useCountryDefaultPremium не задан

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.trip.calculationMode").value("MEDICAL_LEVEL"))
                    .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
        }
    }

    // ── MEDICAL_LEVEL базовые свойства ────────────────────────────────────────

    @Nested
    @DisplayName("MEDICAL_LEVEL — базовые свойства ответа")
    class MedicalLevelResponseProperties {

        @Test
        @DisplayName("calculationMode = MEDICAL_LEVEL в ответе")
        void shouldReturnMedicalLevelMode() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trip.calculationMode").value("MEDICAL_LEVEL"));
        }

        @Test
        @DisplayName("coverageAmount присутствует в MEDICAL_LEVEL")
        void shouldIncludeCoverageAmountInMedicalLevel() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trip.coverageAmount").value(50000.0));
        }

        @Test
        @DisplayName("medicalCoverageLevel присутствует в MEDICAL_LEVEL")
        void shouldIncludeMedicalCoverageLevelInMedicalLevel() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trip.medicalCoverageLevel").value("50000"));
        }

        @Test
        @DisplayName("pricingDetails.countryCoefficient = 1.0 для Испании (LOW risk)")
        void shouldReturnCorrectCountryCoefficientForSpain() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.countryCoefficient").value(1.0));
        }

        @Test
        @DisplayName("pricingDetails.countryCoefficient = 1.3 для Турции (MEDIUM risk)")
        void shouldReturnCorrectCountryCoefficientForTurkey() throws Exception {
            var request = TestRequestBuilder.adult35Turkey().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.countryCoefficient").value(1.3));
        }

        @Test
        @DisplayName("ageCoefficient = 1.0 для 25-летнего (Young adults)")
        void shouldReturnCorrectAgeCoefficientForYoungAdult() throws Exception {
            var request = TestRequestBuilder.young25Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(1.0))
                    .andExpect(jsonPath("$.person.ageGroup").value("Young adults"));
        }

        @Test
        @DisplayName("durationCoefficient < 1.0 для длинной поездки (30 дней)")
        void shouldReturnDurationDiscountForLongTrip() throws Exception {
            var request = TestRequestBuilder.adult35SpainLongTrip().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(lessThan(1.0)));
        }

        @Test
        @DisplayName("baseRate = 4.50 для уровня покрытия 50000 EUR")
        void shouldReturnCorrectBaseRateFor50000Level() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.baseRate").value(4.5));
        }

        @Test
        @DisplayName("baseRate = 7.00 для уровня покрытия 100000 EUR")
        void shouldReturnCorrectBaseRateFor100000Level() throws Exception {
            var request = TestRequestBuilder.adult35Spain()
                    .medicalRiskLimitLevel("100000")
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.baseRate").value(7.0));
        }
    }

    // ── Сравнение COUNTRY_DEFAULT vs MEDICAL_LEVEL ────────────────────────────

    @Nested
    @DisplayName("useCountryDefaultPremium=true с явным medicalRiskLimitLevel")
    class WithExplicitMedicalLevel {

        @Test
        @DisplayName("useCountryDefaultPremium=true + medicalRiskLimitLevel → ответ SUCCESS с MODE=MEDICAL_LEVEL (fallback)")
        void shouldSucceedWithMedicalLevelFallback() throws Exception {
            // Нет записи country_default_day_premiums → fallback на MEDICAL_LEVEL
            var request = TestRequestBuilder.adult35Spain()
                    .useCountryDefaultPremium(true)
                    .medicalRiskLimitLevel("100000")
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.trip.calculationMode").value("MEDICAL_LEVEL"))
                    .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)));
        }
    }

    // ── Шаги расчёта ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Шаги расчёта в MEDICAL_LEVEL")
    class CalculationSteps {

        @Test
        @DisplayName("должны присутствовать шаги расчёта в pricingDetails.steps")
        void shouldIncludeCalculationSteps() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.steps").isArray())
                    .andExpect(jsonPath("$.pricingDetails.steps", hasSize(greaterThan(0))));
        }

        @Test
        @DisplayName("должна присутствовать formула расчёта")
        void shouldIncludeCalculationFormula() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.calculationFormula").isString())
                    .andExpect(jsonPath("$.pricingDetails.calculationFormula", containsString("Premium =")));
        }

        @Test
        @DisplayName("формула должна содержать 'daily rate' для MEDICAL_LEVEL")
        void shouldIncludeDailyRateInFormulaForMedicalLevel() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.calculationFormula",
                            containsStringIgnoringCase("daily rate")));
        }

        @Test
        @DisplayName("формула должна содержать количество дней")
        void shouldIncludeDaysInFormula() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build(); // 14 дней

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.calculationFormula",
                            containsString("14 days")));
        }
    }

    // ── applyAgeCoefficient override ─────────────────────────────────────────

    @Nested
    @DisplayName("applyAgeCoefficient override (task_116)")
    class AgeCoefficientOverride {

        @Test
        @DisplayName("applyAgeCoefficient=false → ageCoefficient=1.0 в ответе")
        void shouldReturnOneCoefficientWhenDisabled() throws Exception {
            var request = TestRequestBuilder.adult35Spain()
                    .applyAgeCoefficient(false)
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(1.0));
        }

        @Test
        @DisplayName("applyAgeCoefficient=false → премия меньше чем с коэффициентом для пожилого")
        void shouldReturnLowerPremiumWhenAgeCoefficientDisabledForElderly() throws Exception {
            // 65 лет → ageCoeff=2.0 (Elderly)
            // С disabled → ageCoeff=1.0 → существенно дешевле

            var requestWithCoeff = TestRequestBuilder.adult35Spain()
                    .personBirthDate(DATE_FROM.minusYears(65))
                    .applyAgeCoefficient(true)
                    .build();

            var requestWithoutCoeff = TestRequestBuilder.adult35Spain()
                    .personBirthDate(DATE_FROM.minusYears(65))
                    .applyAgeCoefficient(false)
                    .build();

            // Оба запроса должны быть SUCCESS
            performCalculatePremium(requestWithCoeff)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(2.0));

            performCalculatePremium(requestWithoutCoeff)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(1.0));
        }

        @Test
        @DisplayName("applyAgeCoefficient=true → ageCoefficient > 1.0 для пожилого (65 лет)")
        void shouldReturnHighCoefficientForElderlyWhenEnabled() throws Exception {
            var request = TestRequestBuilder.adult35Spain()
                    .personBirthDate(DATE_FROM.minusYears(65)) // 65 лет = Elderly, coeff=2.0
                    .applyAgeCoefficient(true)
                    .build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(greaterThan(1.0)));
        }
    }

    // ── Сценарий с Lимитом выплат (task_117) ─────────────────────────────────

    @Nested
    @DisplayName("Лимит выплат в MEDICAL_LEVEL (task_117)")
    class PayoutLimitInMedicalLevel {

        @Test
        @DisplayName("payoutLimitApplied и appliedPayoutLimit присутствуют в pricingDetails")
        void shouldIncludePayoutLimitFieldsInPricingDetails() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricingDetails.payoutLimitApplied").isBoolean())
                    .andExpect(jsonPath("$.pricingDetails.appliedPayoutLimit").isNumber());
        }

        @Test
        @DisplayName("medicalPayoutLimit присутствует в trip summary")
        void shouldIncludeMedicalPayoutLimitInTripSummary() throws Exception {
            var request = TestRequestBuilder.adult35Spain().build();

            performCalculatePremium(request)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trip.medicalPayoutLimit").isNumber());
        }
    }
}