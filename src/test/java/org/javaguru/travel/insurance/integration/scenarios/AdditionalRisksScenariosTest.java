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
 * E2E тесты для сценариев с дополнительными рисками
 */
@DisplayName("E2E: Additional Risks Scenarios")
class AdditionalRisksScenariosTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Один дополнительный риск - Активный спорт")
    void shouldCalculatePremium_withSportActivities() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Alex")
                .personLastName("Skier")
                .personBirthDate(LocalDate.of(1990, 6, 15))
                .agreementDateFrom(LocalDate.now().plusDays(20))
                .agreementDateTo(LocalDate.now().plusDays(27))
                .countryIsoCode("AT")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(1)))
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(2))) // медицинский + спорт
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].premium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Несколько дополнительных рисков")
    void shouldCalculatePremium_withMultipleRisks() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Mary")
                .personLastName("Traveler")
                .personBirthDate(LocalDate.of(1985, 3, 20))
                .agreementDateFrom(LocalDate.now().plusDays(15))
                .agreementDateTo(LocalDate.now().plusDays(29))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of(
                        "SPORT_ACTIVITIES",
                        "LUGGAGE_LOSS",
                        "FLIGHT_DELAY",
                        "TRIP_CANCELLATION"
                ))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(4)))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(5))) // медицинский + 4
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Экстремальный спорт - молодой возраст (одобрено)")
    void shouldCalculatePremium_withExtremeSport_youngAge() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Jack")
                .personLastName("Extreme")
                .personBirthDate(LocalDate.of(1995, 8, 10))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(17))
                .countryIsoCode("CH")
                .medicalRiskLimitLevel("200000")
                .selectedRisks(List.of("EXTREME_SPORT"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("EXTREME_SPORT"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("EXTREME_SPORT"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].premium").value(greaterThan(20.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Экстремальный спорт - средний возраст (требует проверки)")
    void shouldRequireReview_withExtremeSport_middleAge() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Robert")
                .personLastName("Climber")
                .personBirthDate(LocalDate.of(1960, 5, 15)) // 65 лет
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(17))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("EXTREME_SPORT"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isAccepted()) // 202
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("REQUIRES_MANUAL_REVIEW"))
                .andExpect(jsonPath("$.underwriting.reason", containsString("Extreme sport")));
    }

    @Test
    @DisplayName("Экстремальный спорт - старше 70 лет (отклонено)")
    void shouldDecline_withExtremeSport_oldAge() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Senior")
                .personLastName("Adventurer")
                .personBirthDate(LocalDate.of(1950, 1, 1)) // 75 лет
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(17))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of("EXTREME_SPORT"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isUnprocessableEntity()) // 422
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("DECLINED"))
                .andExpect(jsonPath("$.underwriting.reason", containsString("Extreme sport")))
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    @DisplayName("Хронические заболевания - увеличенная премия")
    void shouldCalculatePremium_withChronicDiseases() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Patient")
                .personLastName("Smith")
                .personBirthDate(LocalDate.of(1975, 10, 5))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("CHRONIC_DISEASES"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("CHRONIC_DISEASES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].baseCoefficient").value(0.4))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Пакет рисков - получение скидки")
    void shouldApplyBundleDiscount_forRiskPackage() throws Exception {

        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Bundle")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1988, 7, 15))
                .agreementDateFrom(LocalDate.now().plusDays(14))
                .agreementDateTo(LocalDate.now().plusDays(28))
                .countryIsoCode("IT")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"))
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.baseAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        String json = result.getResponse().getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);

        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();

        assertThat(total).isLessThan(base);
    }


    @Test
    @DisplayName("Все доступные риски")
    void shouldCalculatePremium_withAllRisks() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Max")
                .personLastName("Coverage")
                .personBirthDate(LocalDate.of(1990, 5, 20))
                .agreementDateFrom(LocalDate.now().plusDays(30))
                .agreementDateTo(LocalDate.now().plusDays(60))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("200000")
                .selectedRisks(List.of(
                        "SPORT_ACTIVITIES",
                        "ACCIDENT_COVERAGE",
                        "TRIP_CANCELLATION",
                        "LUGGAGE_LOSS",
                        "FLIGHT_DELAY",
                        "CIVIL_LIABILITY"
                ))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(6)))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(7))) // медицинский + 6
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(100.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Возрастной модификатор - спорт для пожилых дороже")
    void shouldApplyAgeModifier_forSportRisk() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Senior")
                .personLastName("Athlete")
                .personBirthDate(LocalDate.of(1960, 3, 10)) // 65 лет
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(24))
                .countryIsoCode("AT")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].ageModifier").value(greaterThan(1.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Попытка включить обязательный риск TRAVEL_MEDICAL (игнорируется)")
    void shouldIgnore_mandatoryRiskInSelectedRisks() throws Exception {
        // Given
        TravelCalculatePremiumRequest request = TravelCalculatePremiumRequest.builder()
                .personFirstName("User")
                .personLastName("Test")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(5))
                .agreementDateTo(LocalDate.now().plusDays(12))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of("TRAVEL_MEDICAL", "LUGGAGE_LOSS")) // включён обязательный
                .build();

        // When & Then
        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(1))) // только LUGGAGE_LOSS
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("LUGGAGE_LOSS"))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }
}
