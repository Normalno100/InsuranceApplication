package org.javaguru.travel.insurance.rest;

import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.util.JsonComparator;
import org.javaguru.travel.insurance.util.TestDataBuilder;
import org.javaguru.travel.insurance.util.TestDataLoader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.javaguru.travel.insurance.util.TestDataConstants.Requests;
import static org.javaguru.travel.insurance.util.TestDataConstants.Responses;
import static org.javaguru.travel.insurance.util.TestFixtures.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TravelCalculatePremiumController.class)
@DisplayName("Controller Tests Enhanced JSON Comparison")
public class TravelCalculatePremiumControllerTestWithJsonComparison {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TravelCalculatePremiumService calculatePremiumService;

    // ========== SUCCESS SCENARIOS ==========

    @Nested
    @DisplayName("Success Scenarios - Complete JSON Validation")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return complete valid response matching JSON specification")
        void shouldReturnCompleteValidResponse() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should handle zero-day trip (same start and end dates)")
        void shouldHandleZeroDayTrip() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.SAME_DATES);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ZERO_PRICE,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ZERO_PRICE);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should calculate premium for long-term trip (60+ days)")
        void shouldCalculatePremiumForLongTrip() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.LONG_TRIP);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.LONG_TRIP,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String expectedJson = TestDataLoader.loadResponseAsString(Responses.LONG_TRIP);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should preserve special characters in names (hyphens, apostrophes)")
        void shouldPreserveSpecialCharactersInNames() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.SPECIAL_CHARS);
            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    Requests.SPECIAL_CHARS,
                    TravelCalculatePremiumRequest.class
            );

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(10)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedSpecialChars = JsonComparator.partialJson()
                    .addField("personFirstName", "Jean-Pierre")
                    .addField("personLastName", "O'Connor")
                    .build();

            JsonComparator.assertJsonContains(expectedSpecialChars, actualJson);
        }

        @Test
        @DisplayName("Should preserve Cyrillic characters in names")
        void shouldPreserveCyrillicCharactersInNames() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.CYRILLIC);
            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    Requests.CYRILLIC,
                    TravelCalculatePremiumRequest.class
            );

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(10)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedCyrillicNames = JsonComparator.partialJson()
                    .addField("personFirstName", "Иван")
                    .addField("personLastName", "Петров")
                    .build();

            JsonComparator.assertJsonContains(expectedCyrillicNames, actualJson);
        }

        @Test
        @DisplayName("Should work with both / and /calculate endpoints")
        void shouldWorkWithBothEndpoints() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result1 = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonComparator.assertJsonEquals(expectedJson, result1.getResponse().getContentAsString());

            MvcResult result2 = mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonComparator.assertJsonEquals(expectedJson, result2.getResponse().getContentAsString());
        }
    }

    // ========== VALIDATION ERRORS - SINGLE FIELD ==========

    @Nested
    @DisplayName("Validation Errors - Single Field")
    class SingleFieldValidationErrors {

        @Test
        @DisplayName("Should return error when personFirstName is empty")
        void shouldReturnErrorWhenPersonFirstNameIsEmpty() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_FIRST_NAME);

            TravelCalculatePremiumResponse mockResponse = errorResponseForFirstName();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when personLastName is empty")
        void shouldReturnErrorWhenPersonLastNameIsEmpty() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_LAST_NAME);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_LAST_NAME);

            TravelCalculatePremiumResponse mockResponse = errorResponseForLastName();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateFrom is null")
        void shouldReturnErrorWhenAgreementDateFromIsNull() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.NULL_DATE_FROM);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_FROM);

            TravelCalculatePremiumResponse mockResponse = errorResponseForDateFrom();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateTo is null")
        void shouldReturnErrorWhenAgreementDateToIsNull() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.NULL_DATE_TO);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_TO);

            TravelCalculatePremiumResponse mockResponse = errorResponseForDateTo();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateTo is before agreementDateFrom")
        void shouldReturnErrorWhenDateToIsBeforeDateFrom() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.INVALID_DATE_ORDER);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_ORDER);

            TravelCalculatePremiumResponse mockResponse = errorResponseForDateOrder();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== VALIDATION ERRORS - MULTIPLE FIELDS ==========

    @Nested
    @DisplayName("Validation Errors - Multiple Fields")
    class MultipleFieldValidationErrors {

        @Test
        @DisplayName("Should return all 4 errors when all fields are invalid")
        void shouldReturnAllErrorsWhenAllFieldsInvalid() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.ALL_INVALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ALL_ERRORS);

            TravelCalculatePremiumResponse mockResponse = allErrorsResponse();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== PARAMETERIZED TESTS ==========

    @Nested
    @DisplayName("Parameterized Tests - Various Scenarios")
    class ParameterizedTests {

        @ParameterizedTest(name = "Request: {0}, Response: {1}, Expected Status: {2}")
        @CsvSource({
                "valid-request.json,             successful-response.json,         200",
                "same-dates-request.json,        zero-price-response.json,         200",
                "long-trip-request.json,         long-trip-response.json,          200",
                "empty-first-name-request.json,  error-first-name-response.json,   400",
                "empty-last-name-request.json,   error-last-name-response.json,    400",
                "null-date-from-request.json,    error-date-from-response.json,    400",
                "null-date-to-request.json,      error-date-to-response.json,      400",
                "invalid-date-order-request.json,error-date-order-response.json,   400",
                "all-invalid-request.json,       all-errors-response.json,         400"
        })
        @DisplayName("Should handle various request/response scenarios")
        void shouldHandleVariousScenarios(String requestFile, String responseFile, int expectedStatus)
                throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(requestFile);
            String expectedJson = TestDataLoader.loadResponseAsString(responseFile);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    responseFile,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().is(expectedStatus))
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== PARTIAL JSON VALIDATION ==========

    @Nested
    @DisplayName("Partial JSON Validation - Required Fields Only")
    class PartialJsonValidation {

        @Test
        @DisplayName("Should contain all mandatory response fields")
        void shouldContainMandatoryResponseFields() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String mandatoryFields = JsonComparator.partialJson()
                    .addField("personFirstName", "John")
                    .addField("personLastName", "Smith")
                    .addField("agreementPrice", 10)
                    .build();

            JsonComparator.assertJsonContains(mandatoryFields, actualJson);
        }
    }

    // ========== INTEGRATION WITH TESTDATABUILDER ==========

    @Nested
    @DisplayName("Integration with TestDataBuilder")
    class IntegrationWithBuilder {

        @Test
        @DisplayName("Should work with dynamically built request")
        void shouldWorkWithDynamicallyBuiltRequest() throws Exception {
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName("Alice")
                    .withLastName("Johnson")
                    .withDateFrom(LocalDate.of(2023, 6, 1))
                    .withDateTo(LocalDate.of(2023, 6, 15))
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(14)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            TravelCalculatePremiumResponse actualResponse = TestDataLoader.getObjectMapper()
                    .readValue(actualJson, TravelCalculatePremiumResponse.class);

            JsonComparator.assertObjectsEqualAsJson(mockResponse, actualResponse);
        }

        @Test
        @DisplayName("Should work with builder-created error response")
        void shouldWorkWithBuilderCreatedErrorResponse() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .withError("personFirstName", "Must not be empty!")
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedErrorStructure = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            JsonComparator.assertJsonContains(expectedErrorStructure, actualJson);
        }

        @Test
        @DisplayName("Should handle request with special chars built dynamically")
        void shouldHandleRequestWithSpecialCharsDynamically() throws Exception {
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withSpecialCharacters()
                    .withPeriodDays(7)
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(7)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedNames = JsonComparator.partialJson()
                    .addField("personFirstName", "Jean-Pierre")
                    .addField("personLastName", "O'Connor")
                    .build();

            JsonComparator.assertJsonContains(expectedNames, actualJson);
        }
    }

    // ========== EDGE CASES ==========

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very long names (100+ characters)")
        void shouldHandleVeryLongNames() throws Exception {
            String longName = "A".repeat(100);
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName(longName)
                    .withLastName(longName)
                    .withPeriodDays(5)
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(5)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedLongNames = JsonComparator.partialJson()
                    .addField("personFirstName", longName)
                    .addField("personLastName", longName)
                    .build();

            JsonComparator.assertJsonContains(expectedLongNames, actualJson);
        }

        @Test
        @DisplayName("Should handle single character names")
        void shouldHandleSingleCharacterNames() throws Exception {
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName("A")
                    .withLastName("B")
                    .withPeriodDays(3)
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(3)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertObjectsEqualAsJson(mockResponse,
                    TestDataLoader.getObjectMapper().readValue(actualJson, TravelCalculatePremiumResponse.class));
        }

        @Test
        @DisplayName("Should handle maximum realistic trip duration (1 year)")
        void shouldHandleMaximumTripDuration() throws Exception {
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName("John")
                    .withLastName("Smith")
                    .withDateFrom(LocalDate.of(2023, 1, 1))
                    .withDateTo(LocalDate.of(2024, 1, 1))
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(365)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String expectedPrice = JsonComparator.partialJson()
                    .addField("agreementPrice", 365)
                    .build();

            JsonComparator.assertJsonContains(expectedPrice, actualJson);
        }
    }

    // ========== TECHNICAL SCENARIOS ==========

    @Nested
    @DisplayName("Technical Scenarios and Edge Cases")
    class TechnicalScenarios {

        @Test
        @DisplayName("Should return health check status")
        void shouldReturnHealthCheckStatus() throws Exception {
            mockMvc.perform(get("/insurance/travel/health"))
                    .andExpect(status().isOk())
                    .andReturn();
        }

        @Test
        @DisplayName("Should reject request with unsupported content type")
        void shouldRejectUnsupportedContentType() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content(requestJson))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("Should handle malformed JSON gracefully")
        void shouldHandleMalformedJson() throws Exception {
            String malformedJson = "{\"personFirstName\": incomplete";

            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().is5xxServerError());
        }

        @Test
        @DisplayName("Should accept application/json charset=UTF-8")
        void shouldAcceptJsonWithUtf8Charset() throws Exception {
            String requestJson = TestDataLoader.loadRequestAsString(Requests.CYRILLIC);
            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    Requests.CYRILLIC,
                    TravelCalculatePremiumRequest.class
            );

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(10)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            String actualJson = result.getResponse().getContentAsString();
            String cyrillicCheck = JsonComparator.partialJson()
                    .addField("personFirstName", "Иван")
                    .addField("personLastName", "Петров")
                    .build();

            JsonComparator.assertJsonContains(cyrillicCheck, actualJson);
        }
    }
}