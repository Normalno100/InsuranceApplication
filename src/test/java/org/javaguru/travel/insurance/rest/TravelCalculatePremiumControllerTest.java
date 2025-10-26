package org.javaguru.travel.insurance.rest;

import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.util.TestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TravelCalculatePremiumController.class)
@DisplayName("TravelCalculatePremiumController Tests with JSON Files")
public class TravelCalculatePremiumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TravelCalculatePremiumService calculatePremiumService;

    @Nested
    @DisplayName("Happy Path - Successful Scenarios")
    class HappyPath {

        @Test
        @DisplayName("Should return successful response for valid request")
        void shouldReturnSuccessfulResponse() throws Exception {
            // Загружаем запрос и ответ из JSON файлов
            String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.personFirstName").value("John"))
                    .andExpect(jsonPath("$.personLastName").value("Smith"))
                    .andExpect(jsonPath("$.agreementPrice").value(10));
        }

        @Test
        @DisplayName("Should accept /calculate endpoint")
        void shouldAcceptCalculateEndpoint() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreementPrice").value(10));
        }

        @Test
        @DisplayName("Should calculate zero days for same dates")
        void shouldCalculateZeroDaysForSameDates() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("same-dates-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "zero-price-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreementPrice").value(0));
        }

        @Test
        @DisplayName("Should calculate long trip premium")
        void shouldCalculateLongTripPremium() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("long-trip-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "long-trip-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreementPrice").value(60));
        }

        @Test
        @DisplayName("Should accept special characters in names")
        void shouldAcceptSpecialCharactersInNames() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("special-chars-request.json");

            // Создаем ответ динамически на основе запроса
            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    "special-chars-request.json",
                    TravelCalculatePremiumRequest.class
            );

            TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
            response.setPersonFirstName(request.getPersonFirstName());
            response.setPersonLastName(request.getPersonLastName());
            response.setAgreementDateFrom(request.getAgreementDateFrom());
            response.setAgreementDateTo(request.getAgreementDateTo());
            response.setAgreementPrice(new java.math.BigDecimal("10"));

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personFirstName").value("Jean-Pierre"))
                    .andExpect(jsonPath("$.personLastName").value("O'Connor"));
        }

        @Test
        @DisplayName("Should accept Cyrillic characters in names")
        void shouldAcceptCyrillicCharactersInNames() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("cyrillic-request.json");

            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    "cyrillic-request.json",
                    TravelCalculatePremiumRequest.class
            );

            TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
            response.setPersonFirstName(request.getPersonFirstName());
            response.setPersonLastName(request.getPersonLastName());
            response.setAgreementDateFrom(request.getAgreementDateFrom());
            response.setAgreementDateTo(request.getAgreementDateTo());
            response.setAgreementPrice(new java.math.BigDecimal("10"));

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personFirstName").value("Иван"))
                    .andExpect(jsonPath("$.personLastName").value("Петров"));
        }

        @Test
        @DisplayName("Should accept valid request with all fields")
        void shouldAcceptValidRequestWithAllFields() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personFirstName").exists())
                    .andExpect(jsonPath("$.personLastName").exists())
                    .andExpect(jsonPath("$.agreementDateFrom").exists())
                    .andExpect(jsonPath("$.agreementDateTo").exists())
                    .andExpect(jsonPath("$.agreementPrice").exists());
        }
    }

    @Nested
    @DisplayName("Validation Errors")
    class ValidationErrors {

        @Test
        @DisplayName("Should return error when firstName is empty")
        void shouldReturnErrorWhenFirstNameIsEmpty() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("empty-first-name-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-first-name-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value("personFirstName"))
                    .andExpect(jsonPath("$.errors[0].message").value("Must not be empty!"));
        }

        @Test
        @DisplayName("Should return error when lastName is empty")
        void shouldReturnErrorWhenLastNameIsEmpty() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("empty-last-name-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-last-name-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.length()").value(1))
                    .andExpect(jsonPath("$.errors[0].field").value("personLastName"));
        }

        @Test
        @DisplayName("Should return error when dateFrom is null")
        void shouldReturnErrorWhenDateFromIsNull() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("null-date-from-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-date-from-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("agreementDateFrom"));
        }

        @Test
        @DisplayName("Should return error when dateTo is null")
        void shouldReturnErrorWhenDateToIsNull() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("null-date-to-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-date-to-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("agreementDateTo"));
        }

        @Test
        @DisplayName("Should handle date validation (dateTo before dateFrom)")
        void shouldHandleDateValidation() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("invalid-date-order-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-date-order-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value("agreementDateTo"))
                    .andExpect(jsonPath("$.errors[0].message").value("Must be after agreementDateFrom!"));
        }

        @Test
        @DisplayName("Should handle empty request body (all fields invalid)")
        void shouldHandleEmptyRequestBody() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("all-invalid-request.json");
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "all-errors-response.json",
                    TravelCalculatePremiumResponse.class
            );

            when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors").isArray())
                    .andExpect(jsonPath("$.errors.length()").value(4));
        }
    }

    @Nested
    @DisplayName("Technical Scenarios")
    class TechnicalScenarios {

        @Test
        @DisplayName("Should return health check status")
        void shouldReturnHealthCheckStatus() throws Exception {
            mockMvc.perform(get("/insurance/travel/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Travel Insurance Service is running"));
        }

        @Test
        @DisplayName("Should return error for invalid content type")
        void shouldReturnErrorForInvalidContentType() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(requestJson))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should return error for malformed JSON")
        void shouldReturnErrorForMalformedJson() throws Exception {
            String malformedJson = "{\"personFirstName\": \"John\"}"; // Неполный JSON

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().is5xxServerError());
        }
    }
}