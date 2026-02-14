package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты для сценариев со скидками и промо-кодами
 */
@DisplayName("E2E: Discounts and Promo Codes Scenarios")
class DiscountsAndPromoCodesScenariosTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Валидный промо-код - процентная скидка")
    void shouldApplyPromoCode_percentageDiscount() throws Exception {

        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Promo")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1990, 5, 15))
                .agreementDateFrom(LocalDate.of(2025, 7, 1))
                .agreementDateTo(LocalDate.of(2025, 7, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .promoCode("SUMMER2025")
                .build();

        // When
        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(1)))
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("PROMO_CODE"))
                .andExpect(jsonPath("$.appliedDiscounts[0].code").value("SUMMER2025"))
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(10.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        // Then — сравниваем totalPremium и baseAmount
        String json = result.getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();
        double discount = root.path("pricing").path("totalDiscount").asDouble();

        assertThat(discount).isGreaterThan(0.0);
        assertThat(total).isLessThan(base);
    }

    @Test
    @DisplayName("Валидный промо-код - фиксированная скидка")
    void shouldApplyPromoCode_fixedAmount() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Welcome")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1985, 3, 20))
                .agreementDateFrom(LocalDate.now().plusDays(20))
                .agreementDateTo(LocalDate.now().plusDays(40))
                .countryIsoCode("IT")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .promoCode("WELCOME50") // фиксированная скидка 50 EUR
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThanOrEqualTo(40.0))) // близко к 50
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("PROMO_CODE"))
                .andExpect(jsonPath("$.appliedDiscounts[0].code").value("WELCOME50"))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Невалидный промо-код - игнорируется")
    void shouldIgnoreInvalidPromoCode() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Test")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(20))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("50000")
                .promoCode("INVALID_CODE")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(0.0))
                .andExpect(jsonPath("$.appliedDiscounts").isEmpty())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Истёкший промо-код - игнорируется")
    void shouldIgnoreExpiredPromoCode() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Late")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2026, 3, 15)) // после окончания WINTER2025
                .agreementDateTo(LocalDate.of(2026, 3, 25))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("50000")
                .promoCode("WINTER2025")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(0.0))
                .andExpect(jsonPath("$.appliedDiscounts").isEmpty());
    }

    @Test
    @DisplayName("Групповая скидка - 5 человек")
    void shouldApplyGroupDiscount_5persons() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Group")
                .personLastName("Leader")
                .personBirthDate(LocalDate.of(1985, 6, 10))
                .agreementDateFrom(LocalDate.now().plusDays(15))
                .agreementDateTo(LocalDate.now().plusDays(29))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .personsCount(5)
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(1)))
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("GROUP"))
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(10.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Групповая скидка - 10 человек (больше скидка)")
    void shouldApplyGroupDiscount_10persons() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Large")
                .personLastName("Group")
                .personBirthDate(LocalDate.of(1980, 4, 5))
                .agreementDateFrom(LocalDate.now().plusDays(20))
                .agreementDateTo(LocalDate.now().plusDays(34))
                .countryIsoCode("IT")
                .medicalRiskLimitLevel("100000")
                .personsCount(10)
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("GROUP"))
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(15.0)) // 15% для 10+
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Корпоративная скидка")
    void shouldApplyCorporateDiscount() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Corporate")
                .personLastName("Client")
                .personBirthDate(LocalDate.of(1988, 9, 15))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(24))
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("200000")
                .isCorporate(true)
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(1)))
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("CORPORATE"))
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(20.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Пакетная скидка за набор рисков")
    void shouldApplyBundleDiscount() throws Exception {

        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Bundle")
                .personLastName("Buyer")
                .personBirthDate(LocalDate.of(1992, 2, 28))
                .agreementDateFrom(LocalDate.now().plusDays(25))
                .agreementDateTo(LocalDate.now().plusDays(45))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("TRIP_CANCELLATION", "LUGGAGE_LOSS", "FLIGHT_DELAY"))
                .build();

        // When
        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.baseAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        // Then — сравниваем base и total
        String json = result.getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();

        assertThat(base).isGreaterThan(0.0);
        assertThat(total).isLessThan(base);
    }


    @Test
    @DisplayName("Конфликт скидок - применяется лучшая")
    void shouldApplyBestDiscount_whenMultipleAvailable() throws Exception {
        // Given: и промо-код, и корпоративная скидка
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Multi")
                .personLastName("Discount")
                .personBirthDate(LocalDate.of(1985, 11, 5))
                .agreementDateFrom(LocalDate.of(2025, 7, 10))
                .agreementDateTo(LocalDate.of(2025, 7, 25))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("100000")
                .promoCode("SUMMER2025") // 10%
                .isCorporate(true) // 20% - лучше!
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(1))) // только одна скидка
                .andExpect(jsonPath("$.appliedDiscounts[0].type").value("CORPORATE")) // лучшая
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(20.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Прогрессивная скидка за длительность - 30 дней")
    void shouldApplyDurationDiscount_30days() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Long")
                .personLastName("Trip")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(30))
                .agreementDateTo(LocalDate.now().plusDays(59))
                .countryIsoCode("IT")
                .medicalRiskLimitLevel("100000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(30))
                .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(0.90)) // -10% скидка
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Прогрессивная скидка за длительность - 60 дней")
    void shouldApplyDurationDiscount_60days() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Very")
                .personLastName("LongTrip")
                .personBirthDate(LocalDate.of(1988, 6, 15))
                .agreementDateFrom(LocalDate.now().plusDays(40))
                .agreementDateTo(LocalDate.now().plusDays(99))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("200000")
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(60))
                .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(0.88)) // -12% скидка
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Семейная скидка через промо-код")
    void shouldApplyFamilyPromoCode() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Family")
                .personLastName("Vacation")
                .personBirthDate(LocalDate.of(1980, 8, 20))
                .agreementDateFrom(LocalDate.now().plusDays(45))
                .agreementDateTo(LocalDate.now().plusDays(59))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("100000")
                .promoCode("FAMILY20") // 20% семейная скидка
                .personsCount(4)
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts[0].code").value("FAMILY20"))
                .andExpect(jsonPath("$.appliedDiscounts[0].percentage").value(20.0))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }
}
