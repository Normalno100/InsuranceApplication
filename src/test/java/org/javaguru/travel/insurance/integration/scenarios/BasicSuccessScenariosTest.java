package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.TestRequestBuilder;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты для базовых успешных сценариев расчёта страховой премии.
 *
 * КАК ПРИЛОЖЕНИЕ СЧИТАЕТ ДНИ:
 *   days = ChronoUnit.DAYS.between(agreementDateFrom, agreementDateTo)
 *   Конечный день НЕ включается:
 *     from=Apr17, to=May01 (+ 14) → days = 14
 *     from=Apr17, to=Apr24 (+ 7)  → days = 7
 *     from=Apr17, to=May17 (+ 30) → days = 30
 *   TestRequestBuilder использует: agreementDateTo = DATE_FROM + N
 */
@DisplayName("E2E: Basic Success Scenarios")
class BasicSuccessScenariosTest extends BaseIntegrationTest {

    private static final java.time.LocalDate DATE_FROM = TestConstants.TEST_DATE.plusDays(30); // 2026-04-17

    @Test
    @DisplayName("Стандартный взрослый 35 лет, Испания, 7 дней")
    void shouldCalculatePremium_withMinimalRequest() throws Exception {
        // young25Spain() → DATE_FROM + 7 = days=7
        var request = TestRequestBuilder.young25Spain()
                .personBirthDate(TestConstants.TEST_DATE.minusYears(35))  // 35 лет вместо 25
                .medicalRiskLimitLevel("10000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.pricing.currency").value("EUR"))
                .andExpect(jsonPath("$.person.age").value(35))
                .andExpect(jsonPath("$.trip.days").value(7))   // DATE_FROM + 7 → days=7
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Полный запрос - с рисками и промо-кодом, 14 дней")
    void shouldCalculatePremium_withFullRequest() throws Exception {
        // adult35Spain() → DATE_FROM + 14 → days=14
        var request = TestRequestBuilder.adult35Spain()
                .selectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"))
                .currency("EUR")
                .promoCode(TestRequestBuilder.PROMO_10PCT)
                .personsCount(2)
                .isCorporate(false)
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.baseAmount").isNumber())
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(2)))
                .andExpect(jsonPath("$.trip.days").value(14))  // DATE_FROM + 14 → days=14
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Короткая поездка - 1 день в Германию (from + 1 → days=1)")
    void shouldCalculatePremium_forShortTrip() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("20000")
                .agreementDateTo(DATE_FROM.plusDays(1))  // → days=1
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(1))   // DATE_FROM + 1 → days=1
                .andExpect(jsonPath("$.trip.countryName").value("Germany"))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Длительная поездка - 30 дней, durationCoefficient < 1.0")
    void shouldCalculatePremium_forLongTrip() throws Exception {
        // adult35SpainLongTrip() → DATE_FROM + 30 → days=30
        var request = TestRequestBuilder.adult35SpainLongTrip()
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("TRIP_CANCELLATION"))
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(30))  // DATE_FROM + 30 → days=30
                .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(lessThan(1.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Молодой путешественник - 25 лет, ageGroup=Young adults, ageCoefficient=1.0")
    void shouldCalculatePremium_forYoungTraveler() throws Exception {
        // young25Spain() → DATE_FROM + 7 → days=7
        var request = TestRequestBuilder.young25Spain().build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.age").value(25))
                .andExpect(jsonPath("$.person.ageGroup").value("Young adults"))
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(1.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Пожилой путешественник - 65 лет, ageGroup=Elderly, ageCoefficient > 1.5")
    void shouldCalculatePremium_forSeniorTraveler() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                .personBirthDate(DATE_FROM.minusYears(65).minusDays(1))  // 65 лет на дату начала
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.age").value(greaterThanOrEqualTo(65)))
                .andExpect(jsonPath("$.person.ageGroup").value("Elderly"))
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(greaterThan(1.5)))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Запрос без деталей расчёта (includeDetails=false) — pricingDetails отсутствует")
    void shouldCalculatePremium_withoutDetails() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                .medicalRiskLimitLevel("20000")
                .build();

        performCalculatePremium(request, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.person").exists())
                .andExpect(jsonPath("$.trip").exists())
                .andExpect(jsonPath("$.pricingDetails").doesNotExist())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Валюта USD — возвращается в ответе")
    void shouldCalculatePremium_inUSD() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                .countryIsoCode("US")
                .currency("USD")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.currency").value("USD"))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Страна со средним риском - Турция, countryCoefficient=1.3")
    void shouldCalculatePremium_forMediumRiskCountry() throws Exception {
        var request = TestRequestBuilder.adult35Turkey()
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.countryName").value("Turkey"))
                .andExpect(jsonPath("$.pricingDetails.countryCoefficient").value(1.3))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Минимальная премия — применяется при очень маленькой расчётной сумме")
    void shouldApplyMinimumPremium() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                // Ребёнок 10 лет: ageCoeff=0.9, покрытие 5000, 1 день → расчётная < MIN_PREMIUM=10
                .personBirthDate(DATE_FROM.minusYears(10))
                .medicalRiskLimitLevel("5000")
                .agreementDateTo(DATE_FROM.plusDays(1))  // days=1
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // После скидки LOYALTY_10% (10%) от MIN_PREMIUM=10 → итог ~9.0
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }
}