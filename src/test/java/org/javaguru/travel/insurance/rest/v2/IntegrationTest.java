package org.javaguru.travel.insurance.rest.v2;

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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Упрощённые E2E тесты
 * Фокус: ключевые бизнес-сценарии через REST API
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = {
        "/test-data/countries.sql",
        "/test-data/medical-risk-limit-levels.sql",
        "/test-data/risk-types.sql"
})
@DisplayName("Travel Insurance E2E")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========== HAPPY PATH ==========

    @Test
    @DisplayName("calculates premium for simple trip")
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
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("calculates premium with additional risks")
    void shouldCalculateWithRisks() throws Exception {
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
    @DisplayName("applies promo code discount")
    void shouldApplyPromoCode() throws Exception {
        // Use SUMMER2025 with higher premium to meet min requirement (50 EUR)
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 7, 10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")  // Higher level for bigger premium
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
    @DisplayName("ignores promo code when premium below minimum")
    void shouldIgnorePromoCodeBelowMinimum() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 7, 1))
                .agreementDateTo(LocalDate.of(2025, 7, 3))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .promoCode("FAMILY20")  // Min amount is 150, premium will be ~4
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promoCodeInfo").doesNotExist())  // Not applied
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("applies group discount")
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

    // ========== VALIDATION ==========

    @Test
    @DisplayName("rejects invalid request with clear errors")
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
    @DisplayName("rejects when date_to before date_from")
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
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists());
    }

    @Test
    @DisplayName("rejects unknown country")
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
                .andExpect(jsonPath("$.errors[?(@.field == 'countryIsoCode')]").exists());
    }

    // ========== ERROR HANDLING ==========

    @Test
    @DisplayName("handles malformed JSON")
    void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    @DisplayName("handles wrong content type")
    void shouldHandleWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    @DisplayName("handles wrong HTTP method")
    void shouldHandleWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    // ========== HEALTH CHECK ==========

    @Test
    @DisplayName("health check returns 200")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("running")));
    }
}