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
 * E2E Integration Tests
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
@DisplayName("Travel Insurance E2E Tests")
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
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.agreementPrice").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.personFirstName").value("John"))
                .andExpect(jsonPath("$.personLastName").value("Doe"))
                .andExpect(jsonPath("$.countryName").value("Spain"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should calculate premium with additional risks")
    void shouldCalculateWithAdditionalRisks() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.selectedRisks[0]").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.riskPremiums").isArray())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should apply promo code discount")
    void shouldApplyPromoCodeDiscount() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 7, 10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .promoCode("SUMMER2025")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoCodeInfo").exists())
                .andExpect(jsonPath("$.promoCodeInfo.code").value("SUMMER2025"))
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.discountAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should apply group discount")
    void shouldApplyGroupDiscount() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .personsCount(10)
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedDiscounts").isArray())
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.discountAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // ========================================
    // VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should reject invalid request with clear errors")
    void shouldRejectInvalidRequest() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("")
                .personLastName("")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(greaterThan(0)))
                .andExpect(jsonPath("$.agreementPrice").doesNotExist());
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

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*after.*/i)]").exists());
    }

    @Test
    @DisplayName("Should reject unknown country")
    void shouldRejectUnknownCountry() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 10))
                .countryIsoCode("ZZ")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'countryIsoCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*not found.*/i)]").exists());
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Should handle malformed JSON")
    void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("malformed")));
    }

    @Test
    @DisplayName("Should handle wrong content type")
    void shouldHandleWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("unsupported")));
    }

    @Test
    @DisplayName("Should handle wrong HTTP method")
    void shouldHandleWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("method not allowed")));
    }

    // ========================================
    // HEALTH CHECK TEST
    // ========================================

    @Test
    @DisplayName("Health check returns 200")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("running")));
    }
}