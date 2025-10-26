package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Тесты для проверки валидности всех JSON файлов
 */
@DisplayName("JSON Files Validation Tests")
public class JsonFilesValidationTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "valid-request.json",
            "empty-first-name-request.json",
            "empty-last-name-request.json",
            "null-date-from-request.json",
            "null-date-to-request.json",
            "invalid-date-order-request.json",
            "same-dates-request.json",
            "long-trip-request.json",
            "special-chars-request.json",
            "cyrillic-request.json",
            "all-invalid-request.json"
    })
    @DisplayName("All request JSON files should be valid and loadable")
    void allRequestJsonFilesShouldBeValid(String fileName) {
        assertDoesNotThrow(() -> {
            TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                    fileName,
                    TravelCalculatePremiumRequest.class
            );
            assertNotNull(request, "Request should not be null for file: " + fileName);
        }, "Failed to load request file: " + fileName);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "successful-response.json",
            "error-first-name-response.json",
            "error-last-name-response.json",
            "error-date-from-response.json",
            "error-date-to-response.json",
            "error-date-order-response.json",
            "zero-price-response.json",
            "long-trip-response.json",
            "all-errors-response.json"
    })
    @DisplayName("All response JSON files should be valid and loadable")
    void allResponseJsonFilesShouldBeValid(String fileName) {
        assertDoesNotThrow(() -> {
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    fileName,
                    TravelCalculatePremiumResponse.class
            );
            assertNotNull(response, "Response should not be null for file: " + fileName);
        }, "Failed to load response file: " + fileName);
    }

    @Test
    @DisplayName("Valid request should have all required fields")
    void validRequestShouldHaveAllRequiredFields() throws IOException {
        TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                "valid-request.json",
                TravelCalculatePremiumRequest.class
        );

        assertAll(
                () -> assertNotNull(request.getPersonFirstName(), "First name should not be null"),
                () -> assertNotNull(request.getPersonLastName(), "Last name should not be null"),
                () -> assertNotNull(request.getAgreementDateFrom(), "Date from should not be null"),
                () -> assertNotNull(request.getAgreementDateTo(), "Date to should not be null"),
                () -> assertFalse(request.getPersonFirstName().isEmpty(), "First name should not be empty"),
                () -> assertFalse(request.getPersonLastName().isEmpty(), "Last name should not be empty")
        );
    }

    @Test
    @DisplayName("Successful response should have all fields populated")
    void successfulResponseShouldHaveAllFields() throws IOException {
        TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                "successful-response.json",
                TravelCalculatePremiumResponse.class
        );

        assertAll(
                () -> assertNotNull(response.getPersonFirstName(), "First name should not be null"),
                () -> assertNotNull(response.getPersonLastName(), "Last name should not be null"),
                () -> assertNotNull(response.getAgreementDateFrom(), "Date from should not be null"),
                () -> assertNotNull(response.getAgreementDateTo(), "Date to should not be null"),
                () -> assertNotNull(response.getAgreementPrice(), "Price should not be null"),
                () -> assertFalse(response.hasErrors(), "Should not have errors")
        );
    }

    @Test
    @DisplayName("Error response should have errors list")
    void errorResponseShouldHaveErrors() throws IOException {
        TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                "error-first-name-response.json",
                TravelCalculatePremiumResponse.class
        );

        assertAll(
                () -> assertTrue(response.hasErrors(), "Should have errors"),
                () -> assertNotNull(response.getErrors(), "Errors list should not be null"),
                () -> assertFalse(response.getErrors().isEmpty(), "Errors list should not be empty"),
                () -> assertEquals("personFirstName", response.getErrors().get(0).getField()),
                () -> assertEquals("Must not be empty!", response.getErrors().get(0).getMessage())
        );
    }

    @Test
    @DisplayName("All errors response should have 4 errors")
    void allErrorsResponseShouldHave4Errors() throws IOException {
        TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                "all-errors-response.json",
                TravelCalculatePremiumResponse.class
        );

        assertAll(
                () -> assertTrue(response.hasErrors(), "Should have errors"),
                () -> assertEquals(4, response.getErrors().size(), "Should have exactly 4 errors")
        );
    }

    @Test
    @DisplayName("Special characters request should preserve special characters")
    void specialCharsRequestShouldPreserveSpecialCharacters() throws IOException {
        TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                "special-chars-request.json",
                TravelCalculatePremiumRequest.class
        );

        assertEquals("Jean-Pierre", request.getPersonFirstName());
        assertEquals("O'Connor", request.getPersonLastName());
    }

    @Test
    @DisplayName("Cyrillic request should preserve Cyrillic characters")
    void cyrillicRequestShouldPreserveCyrillicCharacters() throws IOException {
        TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                "cyrillic-request.json",
                TravelCalculatePremiumRequest.class
        );

        assertEquals("Иван", request.getPersonFirstName());
        assertEquals("Петров", request.getPersonLastName());
    }

    @Test
    @DisplayName("Same dates request should have equal dates")
    void sameDatesRequestShouldHaveEqualDates() throws IOException {
        TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                "same-dates-request.json",
                TravelCalculatePremiumRequest.class
        );

        assertEquals(request.getAgreementDateFrom(), request.getAgreementDateTo(),
                "Dates should be equal in same-dates-request.json");
    }

    @Test
    @DisplayName("Invalid date order request should have dateTo before dateFrom")
    void invalidDateOrderRequestShouldHaveDateToBeforeDateFrom() throws IOException {
        TravelCalculatePremiumRequest request = TestDataLoader.loadRequest(
                "invalid-date-order-request.json",
                TravelCalculatePremiumRequest.class
        );

        assertTrue(request.getAgreementDateTo().isBefore(request.getAgreementDateFrom()),
                "DateTo should be before dateFrom in invalid-date-order-request.json");
    }

    @Test
    @DisplayName("Request as string should be valid JSON")
    void requestAsStringShouldBeValidJson() throws IOException {
        String requestJson = TestDataLoader.loadRequestAsString("valid-request.json");

        assertAll(
                () -> assertNotNull(requestJson, "JSON string should not be null"),
                () -> assertFalse(requestJson.isEmpty(), "JSON string should not be empty"),
                () -> assertTrue(requestJson.contains("personFirstName"), "Should contain personFirstName field"),
                () -> assertTrue(requestJson.contains("John"), "Should contain John value")
        );
    }
}
