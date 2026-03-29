package org.javaguru.travel.insurance.infrastructure.web.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.v3.*;
import org.javaguru.travel.insurance.application.service.TravelCalculatePremiumServiceV3;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для TravelCalculatePremiumControllerV3.
 *
 * task_135: REST-контроллер V3 API.
 * Проверяем правильность HTTP-статусов и маппинга URL.
 */
@WebMvcTest(TravelCalculatePremiumControllerV3.class)
@ContextConfiguration(classes = {
        TravelCalculatePremiumControllerV3.class
})
@DisplayName("TravelCalculatePremiumControllerV3 — task_135")
class TravelCalculatePremiumControllerV3Test {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private TravelCalculatePremiumServiceV3 calculatePremiumServiceV3;

    private static final String V3_ENDPOINT = "/insurance/travel/v3/calculate";
    private static final LocalDate DATE_FROM = TestConstants.TEST_DATE.plusDays(30);

    // ── HTTP статусы ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("HTTP статусы ответов")
    class HttpStatuses {

        @Test
        @DisplayName("должен вернуть 200 для SUCCESS")
        void shouldReturn200ForSuccess() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(successResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SUCCESS"))
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("должен вернуть 400 для VALIDATION_ERROR")
        void shouldReturn400ForValidationError() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(validationErrorResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("должен вернуть 422 для DECLINED")
        void shouldReturn422ForDeclined() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(declinedResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value("DECLINED"));
        }

        @Test
        @DisplayName("должен вернуть 202 для REQUIRES_REVIEW")
        void shouldReturn202ForRequiresReview() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(requiresReviewResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"));
        }
    }

    // ── Структура ответа SUCCESS ──────────────────────────────────────────────

    @Nested
    @DisplayName("Структура SUCCESS ответа")
    class SuccessResponseStructure {

        @Test
        @DisplayName("должен содержать personPremiums[] в ответе")
        void shouldContainPersonPremiumsInResponse() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(successResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.personPremiums").isArray())
                    .andExpect(jsonPath("$.personPremiums", hasSize(1)))
                    .andExpect(jsonPath("$.personPremiums[0].firstName").value("Ivan"))
                    .andExpect(jsonPath("$.personPremiums[0].age").value(35));
        }

        @Test
        @DisplayName("должен содержать pricing с totalPersonsPremium")
        void shouldContainPricingWithTotalPersonsPremium() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(successResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                    .andExpect(jsonPath("$.pricing.totalPersonsPremium").isNumber())
                    .andExpect(jsonPath("$.pricing.currency").value("EUR"));
        }

        @Test
        @DisplayName("должен содержать apiVersion = '3.0'")
        void shouldContainApiVersion30() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(successResponse());

            mockMvc.perform(post(V3_ENDPOINT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.apiVersion").value("3.0"));
        }
    }

    // ── Ошибки валидации с индексом персоны ──────────────────────────────────

    @Test
    @DisplayName("должен передавать ошибки с индексом персоны")
    void shouldPassPersonIndexedErrors() throws Exception {
        var response = TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(
                        TravelCalculatePremiumResponseV3.ValidationError.builder()
                                .field("persons[0].personBirthDate")
                                .message("Must not be empty!")
                                .build()
                ))
                .build();
        when(calculatePremiumServiceV3.calculatePremium(any())).thenReturn(response);

        mockMvc.perform(post(V3_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("persons[0].personBirthDate"))
                .andExpect(jsonPath("$.errors[0].message").value("Must not be empty!"));
    }

    // ── URL маппинг ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("URL маппинг")
    class UrlMapping {

        @Test
        @DisplayName("POST /insurance/travel/v3/calculate должен существовать")
        void v3CalculateEndpointShouldExist() throws Exception {
            when(calculatePremiumServiceV3.calculatePremium(any()))
                    .thenReturn(successResponse());

            mockMvc.perform(post("/insurance/travel/v3/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validRequestJson()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /insurance/travel/v3/countries должен вернуть 200")
        void countriesEndpointShouldReturn200() throws Exception {
            mockMvc.perform(get("/insurance/travel/v3/countries"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /insurance/travel/v3/coverage-levels должен вернуть 200")
        void coverageLevelsEndpointShouldReturn200() throws Exception {
            mockMvc.perform(get("/insurance/travel/v3/coverage-levels"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET /insurance/travel/v3/risk-types должен вернуть 200")
        void riskTypesEndpointShouldReturn200() throws Exception {
            mockMvc.perform(get("/insurance/travel/v3/risk-types"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("GET на /calculate должен вернуть 405 Method Not Allowed")
        void getShouldReturn405() throws Exception {
            mockMvc.perform(get("/insurance/travel/v3/calculate"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @DisplayName("POST без Content-Type должен вернуть 415")
        void postWithoutContentTypeShouldReturn415() throws Exception {
            mockMvc.perform(post("/insurance/travel/v3/calculate")
                            .content(validRequestJson()))
                    .andExpect(status().isUnsupportedMediaType());
        }
    }

    // ── V2 контроллер изоляция ────────────────────────────────────────────────

    @Test
    @DisplayName("V3 контроллер не отвечает на старый URL /insurance/travel/calculate")
    void v3ControllerShouldNotHandleV2Url() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isNotFound());
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private TravelCalculatePremiumResponseV3 successResponse() {
        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                .success(true)
                .errors(List.of())
                .pricing(PricingSummaryV3.builder()
                        .totalPremium(new BigDecimal("69.30"))
                        .totalPersonsPremium(new BigDecimal("69.30"))
                        .baseAmount(new BigDecimal("69.30"))
                        .totalDiscount(BigDecimal.ZERO)
                        .currency("EUR")
                        .build())
                .personPremiums(List.of(
                        PersonPremium.builder()
                                .firstName("Ivan").lastName("Petrov")
                                .age(35).ageGroup("Adults")
                                .premium(new BigDecimal("69.30"))
                                .ageCoefficient(new BigDecimal("1.10"))
                                .build()
                ))
                .build();
    }

    private TravelCalculatePremiumResponseV3 validationErrorResponse() {
        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                .success(false)
                .errors(List.of(
                        TravelCalculatePremiumResponseV3.ValidationError.builder()
                                .field("persons[0].personFirstName")
                                .message("Must not be empty!")
                                .build()
                ))
                .build();
    }

    private TravelCalculatePremiumResponseV3 declinedResponse() {
        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.DECLINED)
                .success(false)
                .errors(List.of(
                        TravelCalculatePremiumResponseV3.ValidationError.builder()
                                .field("underwriting")
                                .message("Age 85 exceeds max 80")
                                .build()
                ))
                .underwriting(TravelCalculatePremiumResponseV3.UnderwritingInfo.builder()
                        .decision("DECLINED")
                        .reason("Age 85 exceeds max 80")
                        .build())
                .build();
    }

    private TravelCalculatePremiumResponseV3 requiresReviewResponse() {
        return TravelCalculatePremiumResponseV3.builder()
                .status(TravelCalculatePremiumResponseV3.ResponseStatus.REQUIRES_REVIEW)
                .success(false)
                .errors(List.of(
                        TravelCalculatePremiumResponseV3.ValidationError.builder()
                                .field("underwriting")
                                .message("Manual review required: Age 77 requires review")
                                .build()
                ))
                .underwriting(TravelCalculatePremiumResponseV3.UnderwritingInfo.builder()
                        .decision("REQUIRES_MANUAL_REVIEW")
                        .reason("Age 77 requires review")
                        .build())
                .build();
    }

    private String validRequestJson() {
        return """
                {
                  "persons": [
                    {
                      "personFirstName": "Ivan",
                      "personLastName": "Petrov",
                      "personBirthDate": "1990-05-15"
                    }
                  ],
                  "agreementDateFrom": "2026-04-17",
                  "agreementDateTo": "2026-05-01",
                  "countryIsoCode": "ES",
                  "medicalRiskLimitLevel": "50000",
                  "currency": "EUR"
                }
                """;
    }
}