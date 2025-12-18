package org.javaguru.travel.insurance.rest.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Упрощённые тесты обработчика ошибок - по 1 примеру каждого типа
 */
@WebMvcTest(TravelCalculatePremiumControllerV2.class)
@ContextConfiguration(classes = {
        TravelCalculatePremiumControllerV2.class,
        GlobalExceptionHandler.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumServiceV2 service;

    // ========== HTTP ERRORS ==========

    @Test
    void shouldReturn400_whenJsonMalformed() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn415_whenWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void shouldReturn405_whenWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void shouldReturn404_whenEndpointNotFound() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn500_whenUnexpectedException() throws Exception {
        when(service.calculatePremium(any()))
                .thenThrow(new RuntimeException("Unexpected error"));

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    // ========== HELPER ==========

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