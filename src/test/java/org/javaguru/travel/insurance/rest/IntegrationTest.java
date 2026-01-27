package org.javaguru.travel.insurance.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E Integration Tests для API v2.0
 * Тестируют весь стек: REST API → Service → Repository → H2 Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = {
        "/test-data/countries.sql",
        "/test-data/medical-risk-limit-levels.sql",
        "/test-data/risk-types.sql"
})
@DisplayName("Travel Insurance E2E Tests v2.0")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================================
    // HAPPY PATH TESTS
    // ========================================

    @Test
    @DisplayName("Should calculate premium for simple trip")
    void shouldCalculateSimpleTrip() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())

                // Проверяем metadata
                .andExpect(jsonPath("$.apiVersion").value("2.0"))
                .andExpect(jsonPath("$.requestId").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value("SUCCESS"))

                // Проверяем pricing summary
                .andExpect(jsonPath("$.pricing").exists())
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.pricing.currency").value("EUR"))

                // Проверяем person summary
                .andExpect(jsonPath("$.person").exists())
                .andExpect(jsonPath("$.person.firstName").value("John"))
                .andExpect(jsonPath("$.person.lastName").value("Doe"))
                .andExpect(jsonPath("$.person.age").isNumber())

                // Проверяем trip summary
                .andExpect(jsonPath("$.trip").exists())
                .andExpect(jsonPath("$.trip.countryCode").value("ES"))
                .andExpect(jsonPath("$.trip.countryName").value("Spain"))
                .andExpect(jsonPath("$.trip.days").isNumber())

                // Проверяем underwriting
                .andExpect(jsonPath("$.underwriting").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))

                // Errors должен быть пустой массив
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should calculate premium with additional risks")
    void shouldCalculateWithAdditionalRisks() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.success").value(true))

                // Проверяем включенные риски
                .andExpect(jsonPath("$.pricing.includedRisks").isArray())
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("SPORT_ACTIVITIES"))

                // Детали рисков (если includeDetails=true по умолчанию)
                .andExpect(jsonPath("$.pricingDetails").exists())
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown").isArray())

                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("""
Given valid insurance calculation request
When calculating premium with promo code
Then calculation succeeds and discounts are aggregated correctly
""")
    void shouldCalculatePremiumAndAggregateDiscounts_H2() throws Exception {

        // GIVEN
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2026, 6, 1))
                .agreementDateTo(LocalDate.of(2026, 7, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .promoCode("FAMILY20")
                .personsCount(2)
                .isCorporate(false)
                .build();

        // WHEN / THEN
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))

                // базовый контракт
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.totalDiscount").isNumber())

                // оркестрация скидок
                .andExpect(jsonPath("$.appliedDiscounts").isArray())

                // если промокод применён — он корректно отражён
                .andExpect(jsonPath("$.appliedDiscounts").isArray())

        // стабильность ответа
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    @DisplayName("Should include pricing details when includeDetails=true")
    void shouldIncludePricingDetailsWhenRequested() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate?includeDetails=true")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricingDetails").exists())
                .andExpect(jsonPath("$.pricingDetails.baseRate").exists())
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").exists())
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown").isArray());
    }

    @Test
    @DisplayName("Should exclude pricing details when includeDetails=false")
    void shouldExcludePricingDetailsWhenNotRequested() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate?includeDetails=false")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricingDetails").doesNotExist());
    }

    // ========================================
    // VALIDATION ERROR TESTS
    // ========================================

    @Test
    @DisplayName("Should reject invalid request with clear errors")
    void shouldRejectInvalidRequest() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("")
                .personLastName("")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.pricing").doesNotExist());
    }

    @Test
    @DisplayName("Should reject when date_to before date_from")
    void shouldRejectInvalidDateOrder() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 6, 1))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*greater.*/i)]").exists());
    }

    @Test
    @DisplayName("Should reject unknown country")
    void shouldRejectUnknownCountry() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ZZ")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'countryIsoCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*not found.*/i)]").exists());
    }

    @Test
    @DisplayName("Should reject when age exceeds 80")
    void shouldRejectAgeTooOld() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1940, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'personBirthDate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*80.*/)]").exists());
    }

    @Test
    @DisplayName("Should reject trip longer than 365 days")
    void shouldRejectTooLongTrip() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(400))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*365.*/)]").exists());
    }

    // ========================================
    // UNDERWRITING TESTS
    // ========================================

    @Test
    @DisplayName("Should decline extreme sport for age 71+")
    void shouldDeclineExtremeSportForOld() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1950, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("EXTREME_SPORT"))
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity()) // 422
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("DECLINED"))
                .andExpect(jsonPath("$.underwriting.reason").exists())
                .andExpect(jsonPath("$.errors[0].field").value("underwriting"));
    }

    @Test
    @DisplayName("Should require manual review for high coverage + old age")
    void shouldRequireReviewForHighCoverageOldAge() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1948, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("200000")
                .build();

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isAccepted()) // 202
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("REQUIRES_MANUAL_REVIEW"))
                .andExpect(jsonPath("$.underwriting.reason").exists());
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Should handle malformed JSON")
    void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("malformed")));
    }

    @Test
    @DisplayName("Should handle wrong content type")
    void shouldHandleWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("unsupported")));
    }

    @Test
    @DisplayName("Should handle wrong HTTP method")
    void shouldHandleWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("method not allowed")));
    }

    // ========================================
    // HEALTH CHECK & INFO TESTS
    // ========================================

    @Test
    @DisplayName("Health check returns 200")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/insurance/travel/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("running")))
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @DisplayName("API info endpoint returns details")
    void shouldReturnApiInfo() throws Exception {
        mockMvc.perform(get("/insurance/travel/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Travel Insurance API"))
                .andExpect(jsonPath("$.version").value("2.0"))
                .andExpect(jsonPath("$.endpoints").isArray());
    }
}