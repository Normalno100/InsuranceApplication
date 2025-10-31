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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TravelCalculatePremiumController.class)
@DisplayName("Controller Tests Enhanced JSON Comparison")
public class TravelCalculatePremiumControllerTestWithJsonComparison{

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TravelCalculatePremiumService calculatePremiumService;

    // ========== УСПЕШНЫЕ СЦЕНАРИИ ==========

    @Nested
    @DisplayName("Success Scenarios - Complete JSON Validation")
    class SuccessScenarios {

        @Test
        @DisplayName("Should return complete valid response matching JSON specification")
        void shouldReturnCompleteValidResponse() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - полное сравнение с эталонным JSON
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should handle zero-day trip (same start and end dates)")
        void shouldHandleZeroDayTrip() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.SAME_DATES);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ZERO_PRICE,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ZERO_PRICE);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should calculate premium for long-term trip (60+ days)")
        void shouldCalculatePremiumForLongTrip() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.LONG_TRIP);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.LONG_TRIP,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.LONG_TRIP);
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should preserve special characters in names (hyphens, apostrophes)")
        void shouldPreserveSpecialCharactersInNames() throws Exception {
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - проверяем, что спецсимволы не искажены
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
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
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
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act & Assert - endpoint /
            MvcResult result1 = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonComparator.assertJsonEquals(expectedJson, result1.getResponse().getContentAsString());

            // Act & Assert - endpoint /calculate
            MvcResult result2 = mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonComparator.assertJsonEquals(expectedJson, result2.getResponse().getContentAsString());
        }
    }

    // ========== ОШИБКИ ВАЛИДАЦИИ ==========

    @Nested
    @DisplayName("Validation Errors - Single Field")
    class SingleFieldValidationErrors {

        @Test
        @DisplayName("Should return error when personFirstName is empty")
        void shouldReturnErrorWhenPersonFirstNameIsEmpty() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_FIRST_NAME);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_FIRST_NAME,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - точное соответствие структуре ошибки
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when personLastName is empty")
        void shouldReturnErrorWhenPersonLastNameIsEmpty() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_LAST_NAME);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_LAST_NAME);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_LAST_NAME,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateFrom is null")
        void shouldReturnErrorWhenAgreementDateFromIsNull() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.NULL_DATE_FROM);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_FROM);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_DATE_FROM,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateTo is null")
        void shouldReturnErrorWhenAgreementDateToIsNull() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.NULL_DATE_TO);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_TO);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_DATE_TO,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return error when agreementDateTo is before agreementDateFrom")
        void shouldReturnErrorWhenDateToIsBeforeDateFrom() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.INVALID_DATE_ORDER);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ERROR_DATE_ORDER);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_DATE_ORDER,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    @Nested
    @DisplayName("Validation Errors - Multiple Fields")
    class MultipleFieldValidationErrors {

        @Test
        @DisplayName("Should return all 4 errors when all fields are invalid")
        void shouldReturnAllErrorsWhenAllFieldsInvalid() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.ALL_INVALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.ALL_ERRORS);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ALL_ERRORS,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверяем точную структуру всех 4 ошибок
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should validate error array structure (field and message)")
        void shouldValidateErrorArrayStructure() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_FIRST_NAME,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверяем структуру объекта ошибки
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

            JsonComparator.assertJsonEqualsIgnoringExtraFields(expectedErrorStructure, actualJson);
        }
    }

    // ========== ПАРАМЕТРИЗОВАННЫЕ ТЕСТЫ ==========

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
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(requestFile);
            String expectedJson = TestDataLoader.loadResponseAsString(responseFile);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    responseFile,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().is(expectedStatus))
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== ЧАСТИЧНАЯ ПРОВЕРКА ==========

    @Nested
    @DisplayName("Partial JSON Validation - Required Fields Only")
    class PartialJsonValidation {

        @Test
        @DisplayName("Should contain all mandatory response fields")
        void shouldContainMandatoryResponseFields() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - проверяем только обязательные поля
            String actualJson = result.getResponse().getContentAsString();
            String mandatoryFields = JsonComparator.partialJson()
                    .addField("personFirstName", "John")
                    .addField("personLastName", "Smith")
                    .addField("agreementPrice", 10)
                    .build();

            JsonComparator.assertJsonContains(mandatoryFields, actualJson);
        }

        @Test
        @DisplayName("Should contain error field in validation error response")
        void shouldContainErrorFieldInValidationError() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_FIRST_NAME,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверяем только наличие поля error и минимальную структуру
            String actualJson = result.getResponse().getContentAsString();
            String minimalErrorStructure = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName"
                            }
                        ]
                    }
                    """;

            JsonComparator.assertJsonContains(minimalErrorStructure, actualJson);
        }

        @Test
        @DisplayName("Should preserve price precision in response")
        void shouldPreservePricePrecisionInResponse() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .fromFile(Responses.SUCCESSFUL)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - проверяем тип и значение цены
            String actualJson = result.getResponse().getContentAsString();
            String priceCheck = JsonComparator.partialJson()
                    .addField("agreementPrice", 10)
                    .build();

            JsonComparator.assertJsonContains(priceCheck, actualJson);
        }
    }

    // ========== ФОРМАТЫ И ТИПЫ ДАННЫХ ==========

    @Nested
    @DisplayName("Data Format Validation")
    class DataFormatValidation {

        @Test
        @DisplayName("Should preserve ISO 8601 date format (yyyy-MM-dd)")
        void shouldPreserveIsoDateFormat() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - JsonComparator проверит точное совпадение формата дат
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Should return price as numeric value, not string")
        void shouldReturnPriceAsNumericValue() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(TestDataBuilder.validRequest())
                    .withPrice(new BigDecimal("10"))
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - цена должна быть числом, не строкой
            String actualJson = result.getResponse().getContentAsString();
            String expectedWithNumericPrice = JsonComparator.partialJson()
                    .addField("agreementPrice", 10)  // Число, не "10"
                    .build();

            JsonComparator.assertJsonContains(expectedWithNumericPrice, actualJson);
        }

        @Test
        @DisplayName("Should handle null errors field in successful response")
        void shouldHandleNullErrorsFieldInSuccessfulResponse() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - errors должен быть null
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== ТЕХНИЧЕСКИЕ СЦЕНАРИИ ==========

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
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8")
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - кириллица должна сохраниться
            String actualJson = result.getResponse().getContentAsString();
            String cyrillicCheck = JsonComparator.partialJson()
                    .addField("personFirstName", "Иван")
                    .addField("personLastName", "Петров")
                    .build();

            JsonComparator.assertJsonContains(cyrillicCheck, actualJson);
        }
    }

    // ========== ИНТЕГРАЦИОННЫЕ ТЕСТЫ С BUILDER ==========

    @Nested
    @DisplayName("Integration with TestDataBuilder")
    class IntegrationWithBuilder {

        @Test
        @DisplayName("Should work with dynamically built request")
        void shouldWorkWithDynamicallyBuiltRequest() throws Exception {
            // Arrange - строим запрос через builder
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            TravelCalculatePremiumResponse actualResponse = TestDataLoader.getObjectMapper()
                    .readValue(actualJson, TravelCalculatePremiumResponse.class);

            JsonComparator.assertObjectsEqualAsJson(mockResponse, actualResponse);
        }

        @Test
        @DisplayName("Should work with builder-created error response")
        void shouldWorkWithBuilderCreatedErrorResponse() throws Exception {
            // Arrange - строим ответ с ошибкой через builder
            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .withError("personFirstName", "Must not be empty!")
                    .withError("personLastName", "Must not be empty!")
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверяем структуру ошибок
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

            // Используем частичное сравнение, так как может быть больше ошибок
            JsonComparator.assertJsonContains(expectedErrorStructure, actualJson);
        }

        @Test
        @DisplayName("Should handle request with special chars built dynamically")
        void shouldHandleRequestWithSpecialCharsDynamically() throws Exception {
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            String expectedNames = JsonComparator.partialJson()
                    .addField("personFirstName", "Jean-Pierre")
                    .addField("personLastName", "O'Connor")
                    .build();

            JsonComparator.assertJsonContains(expectedNames, actualJson);
        }
    }

    // ========== РЕГРЕССИОННЫЕ ТЕСТЫ ==========

    @Nested
    @DisplayName("Regression Tests - API Contract Validation")
    class RegressionTests {

        @Test
        @DisplayName("API v1 contract - successful response structure must not change")
        void apiV1ContractSuccessfulResponse() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String apiContractJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - строгая проверка контракта API
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(apiContractJson, actualJson);
        }

        @Test
        @DisplayName("API v1 contract - error response structure must not change")
        void apiV1ContractErrorResponse() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.ALL_INVALID);
            String apiContractJson = TestDataLoader.loadResponseAsString(Responses.ALL_ERRORS);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ALL_ERRORS,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверка контракта структуры ошибок
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(apiContractJson, actualJson);
        }

        @Test
        @DisplayName("Date format must remain ISO 8601 (yyyy-MM-dd)")
        void dateFormatMustRemainIso8601() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - даты должны быть в формате ISO 8601
            String actualJson = result.getResponse().getContentAsString();
            String expectedDateFormat = JsonComparator.partialJson()
                    .addField("agreementDateFrom", "2023-01-01")
                    .addField("agreementDateTo", "2023-01-11")
                    .build();

            JsonComparator.assertJsonContains(expectedDateFormat, actualJson);
        }

        @Test
        @DisplayName("Error message format must not change")
        void errorMessageFormatMustNotChange() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.EMPTY_FIRST_NAME);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.ERROR_FIRST_NAME,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest())
                    .andReturn();

            // Assert - проверяем точное сообщение об ошибке
            String actualJson = result.getResponse().getContentAsString();
            String expectedErrorMessage = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            JsonComparator.assertJsonEqualsIgnoringNullFields(expectedErrorMessage, actualJson);
        }
    }

    // ========== EDGE CASES И ГРАНИЧНЫЕ УСЛОВИЯ ==========

    @Nested
    @DisplayName("Edge Cases and Boundary Conditions")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very long names (100+ characters)")
        void shouldHandleVeryLongNames() throws Exception {
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
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
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertObjectsEqualAsJson(mockResponse,
                    TestDataLoader.getObjectMapper().readValue(actualJson, TravelCalculatePremiumResponse.class));
        }

        @Test
        @DisplayName("Should handle maximum realistic trip duration (1 year)")
        void shouldHandleMaximumTripDuration() throws Exception {
            // Arrange
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

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            String expectedPrice = JsonComparator.partialJson()
                    .addField("agreementPrice", 365)
                    .build();

            JsonComparator.assertJsonContains(expectedPrice, actualJson);
        }

        @Test
        @DisplayName("Should handle leap year dates (February 29)")
        void shouldHandleLeapYearDates() throws Exception {
            // Arrange
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName("John")
                    .withLastName("Smith")
                    .withDateFrom(LocalDate.of(2024, 2, 29))
                    .withDateTo(LocalDate.of(2024, 3, 10))
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(10)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - проверяем что дата 29 февраля корректно обработана
            String actualJson = result.getResponse().getContentAsString();
            String expectedLeapDate = JsonComparator.partialJson()
                    .addField("agreementDateFrom", "2024-02-29")
                    .build();

            JsonComparator.assertJsonContains(expectedLeapDate, actualJson);
        }

        @Test
        @DisplayName("Should handle year boundary crossing (Dec 31 -> Jan 1)")
        void shouldHandleYearBoundaryCrossing() throws Exception {
            // Arrange
            TravelCalculatePremiumRequest request = TestDataBuilder.request()
                    .withFirstName("John")
                    .withLastName("Smith")
                    .withDateFrom(LocalDate.of(2023, 12, 28))
                    .withDateTo(LocalDate.of(2024, 1, 5))
                    .build();

            TravelCalculatePremiumResponse mockResponse = TestDataBuilder.response()
                    .basedOnRequest(request)
                    .withPrice(8)
                    .build();

            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            String requestJson = TestDataLoader.getObjectMapper().writeValueAsString(request);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertObjectsEqualAsJson(mockResponse,
                    TestDataLoader.getObjectMapper().readValue(actualJson, TravelCalculatePremiumResponse.class));
        }
    }

    // ========== СРАВНЕНИЕ ПРОИЗВОДИТЕЛЬНОСТИ ==========

    @Nested
    @DisplayName("Performance Comparison - jsonPath vs JsonComparator")
    class PerformanceComparison {

        @Test
        @DisplayName("jsonPath approach - traditional way (for comparison)")
        void jsonPathApproach() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act & Assert - старый подход с jsonPath
            mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.personFirstName").value("John"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.personLastName").value("Smith"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.agreementDateFrom").value("2023-01-01"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.agreementDateTo").value("2023-01-11"))
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                            .jsonPath("$.agreementPrice").value(10));

            // 5 строк проверок vs 1 строка с JsonComparator
        }

        @Test
        @DisplayName("JsonComparator approach - modern way (recommended)")
        void jsonComparatorApproach() throws Exception {
            // Arrange
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Assert - новый подход с JsonComparator (1 строка!)
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }
    }

    // ========== ДОКУМЕНТАЦИЯ И ПРИМЕРЫ ==========

    @Nested
    @DisplayName("Documentation Examples - How to Use JsonComparator")
    class DocumentationExamples {

        @Test
        @DisplayName("Example 1: Full JSON comparison with file")
        void example1FullComparison() throws Exception {
            // Загружаем запрос и ожидаемый ответ из файлов
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            String expectedJson = TestDataLoader.loadResponseAsString(Responses.SUCCESSFUL);

            // Настраиваем mock
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Выполняем запрос
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Сравниваем полные JSON документы
            String actualJson = result.getResponse().getContentAsString();
            JsonComparator.assertJsonEquals(expectedJson, actualJson);
        }

        @Test
        @DisplayName("Example 2: Partial comparison with builder")
        void example2PartialComparison() throws Exception {
            // Настраиваем данные
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            TravelCalculatePremiumResponse mockResponse = TestDataLoader.loadResponse(
                    Responses.SUCCESSFUL,
                    TravelCalculatePremiumResponse.class
            );
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Выполняем запрос
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Проверяем только важные поля
            String actualJson = result.getResponse().getContentAsString();
            String criticalFields = JsonComparator.partialJson()
                    .addField("personFirstName", "John")
                    .addField("agreementPrice", 10)
                    .build();

            JsonComparator.assertJsonContains(criticalFields, actualJson);
        }

        @Test
        @DisplayName("Example 3: Object comparison")
        void example3ObjectComparison() throws Exception {
            // Создаем ожидаемый объект
            TravelCalculatePremiumResponse expected = TestDataBuilder.response()
                    .withFirstName("John")
                    .withLastName("Smith")
                    .withPrice(10)
                    .build();

            // Настраиваем mock
            when(calculatePremiumService.calculatePremium(any())).thenReturn(expected);

            // Выполняем запрос
            String requestJson = TestDataLoader.loadRequestAsString(Requests.VALID);
            MvcResult result = mockMvc.perform(post("/insurance/travel/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk())
                    .andReturn();

            // Десериализуем и сравниваем объекты
            String actualJson = result.getResponse().getContentAsString();
            TravelCalculatePremiumResponse actual = TestDataLoader.getObjectMapper()
                    .readValue(actualJson, TravelCalculatePremiumResponse.class);

            JsonComparator.assertObjectsEqualAsJson(expected, actual);
        }
    }
}