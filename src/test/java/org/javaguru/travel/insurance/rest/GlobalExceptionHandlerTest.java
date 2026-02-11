package org.javaguru.travel.insurance.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.application.service.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.infrastructure.web.controller.TravelCalculatePremiumController;
import org.javaguru.travel.insurance.infrastructure.web.error.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты обработчика ошибок
 */
@WebMvcTest(TravelCalculatePremiumController.class)
@ContextConfiguration(classes = {
        TravelCalculatePremiumController.class,
        GlobalExceptionHandler.class
})
@DisplayName("Global Exception Handler")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumService service;

    // ========================================
    // HTTP ERROR TESTS
    // ========================================

    @Test
    @DisplayName("Returns 400 when JSON is malformed")
    void shouldReturn400_whenJsonMalformed() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("Returns 415 when content type is wrong")
    void shouldReturn415_whenWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    @DisplayName("Returns 405 when HTTP method is wrong")
    void shouldReturn405_whenWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("Returns 404 when endpoint not found")
    void shouldReturn404_whenEndpointNotFound() throws Exception {
        mockMvc.perform(get("/insurance/travel/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Returns 500 when unexpected exception occurs")
    void shouldReturn500_whenUnexpectedException() throws Exception {
        when(service.calculatePremium(any(), anyBoolean()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    // ========================================
    // SERVICE ERROR TESTS
    // ========================================

    @Test
    @DisplayName("Returns 400 for validation errors from service")
    void shouldReturn400_whenValidationError() throws Exception {
        var errorResponse = TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(java.util.List.of(
                        TravelCalculatePremiumResponse.ValidationError.builder()
                                .field("personFirstName")
                                .message("Must not be empty")
                                .build()
                ))
                .build();

        when(service.calculatePremium(any(), anyBoolean()))
                .thenReturn(errorResponse);

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("personFirstName"));
    }

    @Test
    @DisplayName("Returns 422 for declined underwriting")
    void shouldReturn422_whenDeclined() throws Exception {
        var declinedResponse = TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.DECLINED)
                .success(false)
                .errors(java.util.List.of(
                        TravelCalculatePremiumResponse.ValidationError.builder()
                                .field("underwriting")
                                .message("Application declined: Age exceeds maximum")
                                .build()
                ))
                .underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                        .decision("DECLINED")
                        .reason("Age exceeds maximum")
                        .build())
                .build();

        when(service.calculatePremium(any(), anyBoolean()))
                .thenReturn(declinedResponse);

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isUnprocessableEntity()) // 422
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("DECLINED"));
    }

    @Test
    @DisplayName("Returns 202 for requires manual review")
    void shouldReturn202_whenRequiresReview() throws Exception {
        var reviewResponse = TravelCalculatePremiumResponse.builder()
                .status(TravelCalculatePremiumResponse.ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .errors(java.util.List.of(
                        TravelCalculatePremiumResponse.ValidationError.builder()
                                .field("underwriting")
                                .message("Manual review required")
                                .build()
                ))
                .underwriting(TravelCalculatePremiumResponse.UnderwritingInfo.builder()
                        .decision("REQUIRES_MANUAL_REVIEW")
                        .reason("High coverage for age")
                        .build())
                .build();

        when(service.calculatePremium(any(), anyBoolean()))
                .thenReturn(reviewResponse);

        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isAccepted()) // 202
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("REQUIRES_MANUAL_REVIEW"));
    }

    // ========================================
    // HELPER METHODS
    // ========================================

    private String validRequestJson() {
        return """
            {
              "personFirstName": "John",
              "personLastName": "Doe",
              "personBirthDate": "1990-01-01",
              "agreementDateFrom": "2025-06-01",
              "agreementDateTo": "2025-06-15",
              "countryIsoCode": "ES",
              "medicalRiskLimitLevel": "10000"
            }
            """;
    }
}