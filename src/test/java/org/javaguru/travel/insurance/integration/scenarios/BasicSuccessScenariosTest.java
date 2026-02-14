package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты для базовых успешных сценариев расчёта страховой премии
 */
@DisplayName("E2E: Basic Success Scenarios")
class BasicSuccessScenariosTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Минимальный запрос - только обязательные поля")
    void shouldCalculatePremium_withMinimalRequest() throws Exception {
        // Given: минимальный валидный запрос
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 15))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(17))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errors").isEmpty())
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.pricing.currency").value("EUR"))
                .andExpect(jsonPath("$.person").exists())
                .andExpect(jsonPath("$.person.age").value(greaterThanOrEqualTo(30)))
                .andExpect(jsonPath("$.trip.days").value(8))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Полный запрос - со всеми опциональными полями")
    void shouldCalculatePremium_withFullRequest() throws Exception {
        // Given: полный запрос со всеми полями
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Jane")
                .personLastName("Smith")
                .personBirthDate(LocalDate.of(1985, 5, 20))
                .personEmail("jane.smith@example.com")
                .personPhone("+1234567890")
                .agreementDateFrom(LocalDate.now().plusDays(14))
                .agreementDateTo(LocalDate.now().plusDays(28))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"))
                .currency("EUR")
                .promoCode("SUMMER2025")
                .personsCount(2)
                .isCorporate(false)
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.baseAmount").isNumber())
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(2)))
                .andExpect(jsonPath("$.person.email").doesNotExist()) // email не возвращается
                .andExpect(jsonPath("$.trip.days").value(15))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(greaterThan(0))))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Короткая поездка - 3 дня в страну с низким риском")
    void shouldCalculatePremium_forShortTrip() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Bob")
                .personLastName("Johnson")
                .personBirthDate(LocalDate.of(1995, 3, 10))
                .agreementDateFrom(LocalDate.now().plusDays(5))
                .agreementDateTo(LocalDate.now().plusDays(7))
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("20000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(3))
                .andExpect(jsonPath("$.trip.countryName").value("Germany"))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Длительная поездка - 30 дней с прогрессивной скидкой")
    void shouldCalculatePremium_forLongTrip() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Alice")
                .personLastName("Williams")
                .personBirthDate(LocalDate.of(1992, 7, 15))
                .agreementDateFrom(LocalDate.now().plusDays(20))
                .agreementDateTo(LocalDate.now().plusDays(49))
                .countryIsoCode("IT")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("TRIP_CANCELLATION"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(30))
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(lessThan(1.0))) // скидка
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Молодой путешественник - возраст 25 лет")
    void shouldCalculatePremium_forYoungTraveler() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Mike")
                .personLastName("Brown")
                .personBirthDate(LocalDate.now().minusYears(25))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.age").value(25))
                .andExpect(jsonPath("$.person.ageGroup").value("Young adults"))
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(1.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Пожилой путешественник - возраст 65 лет")
    void shouldCalculatePremium_forSeniorTraveler() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Robert")
                .personLastName("Davis")
                .personBirthDate(LocalDate.now().minusYears(65))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(24))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("100000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.person.age").value(65))
                .andExpect(jsonPath("$.person.ageGroup").value("Elderly"))
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").value(greaterThan(1.5)))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Запрос без деталей расчёта (includeDetails=false)")
    void shouldCalculatePremium_withoutDetails() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Sarah")
                .personLastName("Miller")
                .personBirthDate(LocalDate.of(1988, 9, 5))
                .agreementDateFrom(LocalDate.now().plusDays(15))
                .agreementDateTo(LocalDate.now().plusDays(22))
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("20000")
                .build();

        // When & Then
        performCalculatePremium(request, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.person").exists())
                .andExpect(jsonPath("$.trip").exists())
                .andExpect(jsonPath("$.pricingDetails").doesNotExist()) // детали не включены
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Разные валюты - USD")
    void shouldCalculatePremium_inUSD() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Tom")
                .personLastName("Wilson")
                .personBirthDate(LocalDate.of(1991, 11, 20))
                .agreementDateFrom(LocalDate.now().plusDays(5))
                .agreementDateTo(LocalDate.now().plusDays(12))
                .countryIsoCode("US")
                .medicalRiskLimitLevel("50000")
                .currency("USD")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.currency").value("USD"))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Страна со средним риском - Турция")
    void shouldCalculatePremium_forMediumRiskCountry() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Emma")
                .personLastName("Taylor")
                .personBirthDate(LocalDate.of(1987, 4, 12))
                .agreementDateFrom(LocalDate.now().plusDays(30))
                .agreementDateTo(LocalDate.now().plusDays(44))
                .countryIsoCode("TR")
                .medicalRiskLimitLevel("100000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.countryName").value("Turkey"))
                .andExpect(jsonPath("$.pricingDetails.countryCoefficient").value(1.3))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Минимальная премия - очень короткая поездка")
    void shouldApplyMinimumPremium() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Child")
                .personLastName("Test")
                .personBirthDate(LocalDate.now().minusYears(10))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(2))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("5000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThanOrEqualTo(10.0))) // MIN_PREMIUM
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }
}
