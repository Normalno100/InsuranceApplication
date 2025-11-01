package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("JsonComparator Tests - Updated")
public class JsonComparatorTest {

    @Nested
    @DisplayName("Exact JSON Comparison")
    class ExactJsonComparison {

        @Test
        @DisplayName("Should pass when JSONs are identical")
        void shouldPassWhenJsonsAreIdentical() {
            String json1 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementPrice": 10
                    }
                    """;

            String json2 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementPrice": 10
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should pass when JSONs are identical but with different whitespace")
        void shouldPassWhenJsonsHaveDifferentWhitespace() {
            String json1 = """
                    {"personFirstName":"John","personLastName":"Smith"}
                    """;

            String json2 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith"
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should pass when field order is different")
        void shouldPassWhenFieldOrderIsDifferent() {
            String json1 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith"
                    }
                    """;

            String json2 = """
                    {
                        "personLastName": "Smith",
                        "personFirstName": "John"
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should fail when string values differ")
        void shouldFailWhenStringValuesDiffer() {
            String json1 = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            String json2 = """
                    {
                        "personFirstName": "Jane"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("personFirstName"));
            assertTrue(error.getMessage().contains("John"));
            assertTrue(error.getMessage().contains("Jane"));
        }

        @Test
        @DisplayName("Should fail when numeric values differ")
        void shouldFailWhenNumericValuesDiffer() {
            String json1 = """
                    {
                        "agreementPrice": 10
                    }
                    """;

            String json2 = """
                    {
                        "agreementPrice": 20
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("agreementPrice"));
            assertTrue(error.getMessage().contains("10"));
            assertTrue(error.getMessage().contains("20"));
        }

        @Test
        @DisplayName("Should fail when field is missing in actual")
        void shouldFailWhenFieldIsMissingInActual() {
            String expected = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith"
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("personLastName"));
            assertTrue(error.getMessage().contains("missing"));
        }

        @Test
        @DisplayName("Should fail when unexpected field exists in actual")
        void shouldFailWhenUnexpectedFieldExistsInActual() {
            String expected = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("personLastName"));
            assertTrue(error.getMessage().contains("Unexpected"));
        }

        @Test
        @DisplayName("Should handle null values correctly")
        void shouldHandleNullValuesCorrectly() {
            String json1 = """
                    {
                        "personFirstName": "John",
                        "agreementPrice": null
                    }
                    """;

            String json2 = """
                    {
                        "personFirstName": "John",
                        "agreementPrice": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should fail when null vs non-null")
        void shouldFailWhenNullVsNonNull() {
            String json1 = """
                    {
                        "agreementPrice": null
                    }
                    """;

            String json2 = """
                    {
                        "agreementPrice": 10
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("agreementPrice"));
        }
    }

    @Nested
    @DisplayName("Error Response Scenarios - Real Production Cases")
    class ErrorResponseScenarios {

        @Test
        @DisplayName("Should compare error response with null fields")
        void shouldCompareErrorResponseWithNullFields() {
            // Реальный случай: error response содержит null поля
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Should fail when error response has wrong error field")
        void shouldFailWhenErrorResponseHasWrongErrorField() {
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personLastName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("errors[0].field"));
            assertTrue(error.getMessage().contains("personFirstName"));
            assertTrue(error.getMessage().contains("personLastName"));
        }

        @Test
        @DisplayName("Should compare multiple errors correctly")
        void shouldCompareMultipleErrorsCorrectly() {
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "personLastName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "personLastName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Should detect missing null field in error response")
        void shouldDetectMissingNullFieldInErrorResponse() {
            // Expected имеет все null поля
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            // Actual не имеет одного null поля
            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("agreementPrice"));
            assertTrue(error.getMessage().contains("missing"));
        }
    }

    @Nested
    @DisplayName("Array Comparison")
    class ArrayComparison {

        @Test
        @DisplayName("Should pass when arrays are identical")
        void shouldPassWhenArraysAreIdentical() {
            String json1 = """
                    {
                        "errors": [
                            {"field": "personFirstName", "message": "Must not be empty!"},
                            {"field": "personLastName", "message": "Must not be empty!"}
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {"field": "personFirstName", "message": "Must not be empty!"},
                            {"field": "personLastName", "message": "Must not be empty!"}
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should fail when array sizes differ")
        void shouldFailWhenArraySizesDiffer() {
            String json1 = """
                    {
                        "errors": [
                            {"field": "personFirstName", "message": "Error 1"},
                            {"field": "personLastName", "message": "Error 2"}
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {"field": "personFirstName", "message": "Error 1"}
                        ]
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("Array size mismatch"));
            assertTrue(error.getMessage().contains("expected 2"));
            assertTrue(error.getMessage().contains("got 1"));
        }

        @Test
        @DisplayName("Should fail when array elements differ")
        void shouldFailWhenArrayElementsDiffer() {
            String json1 = """
                    {
                        "errors": [
                            {"field": "personFirstName", "message": "Error 1"}
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {"field": "personLastName", "message": "Error 1"}
                        ]
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("errors[0].field"));
            assertTrue(error.getMessage().contains("personFirstName"));
            assertTrue(error.getMessage().contains("personLastName"));
        }

        @Test
        @DisplayName("Should handle empty arrays")
        void shouldHandleEmptyArrays() {
            String json1 = """
                    {
                        "errors": []
                    }
                    """;

            String json2 = """
                    {
                        "errors": []
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should detect array order differences")
        void shouldDetectArrayOrderDifferences() {
            String json1 = """
                    {
                        "errors": [
                            {"field": "field1"},
                            {"field": "field2"}
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {"field": "field2"},
                            {"field": "field1"}
                        ]
                    }
                    """;

            // Порядок элементов важен!
            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("errors[0].field"));
        }
    }

    @Nested
    @DisplayName("Nested Object Comparison")
    class NestedObjectComparison {

        @Test
        @DisplayName("Should compare nested objects correctly")
        void shouldCompareNestedObjectsCorrectly() {
            String json1 = """
                    {
                        "person": {
                            "firstName": "John",
                            "lastName": "Smith"
                        }
                    }
                    """;

            String json2 = """
                    {
                        "person": {
                            "firstName": "John",
                            "lastName": "Smith"
                        }
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should detect differences in nested objects")
        void shouldDetectDifferencesInNestedObjects() {
            String json1 = """
                    {
                        "person": {
                            "firstName": "John"
                        }
                    }
                    """;

            String json2 = """
                    {
                        "person": {
                            "firstName": "Jane"
                        }
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("person.firstName"));
            assertTrue(error.getMessage().contains("John"));
            assertTrue(error.getMessage().contains("Jane"));
        }

        @Test
        @DisplayName("Should handle deeply nested structures")
        void shouldHandleDeeplyNestedStructures() {
            String json1 = """
                    {
                        "level1": {
                            "level2": {
                                "level3": {
                                    "value": "deep"
                                }
                            }
                        }
                    }
                    """;

            String json2 = """
                    {
                        "level1": {
                            "level2": {
                                "level3": {
                                    "value": "deep"
                                }
                            }
                        }
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should compare error objects nested in arrays")
        void shouldCompareErrorObjectsNestedInArrays() {
            String json1 = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!",
                                "details": {
                                    "code": "VALIDATION_ERROR",
                                    "severity": "HIGH"
                                }
                            }
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!",
                                "details": {
                                    "code": "VALIDATION_ERROR",
                                    "severity": "HIGH"
                                }
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }
    }

    @Nested
    @DisplayName("Type Mismatch Detection")
    class TypeMismatchDetection {

        @Test
        @DisplayName("Should detect string vs number mismatch")
        void shouldDetectStringVsNumberMismatch() {
            String json1 = """
                    {
                        "value": 10
                    }
                    """;

            String json2 = """
                    {
                        "value": "10"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("Type mismatch"));
            assertTrue(error.getMessage().contains("NUMBER"));
            assertTrue(error.getMessage().contains("STRING"));
        }

        @Test
        @DisplayName("Should detect object vs array mismatch")
        void shouldDetectObjectVsArrayMismatch() {
            String json1 = """
                    {
                        "data": {}
                    }
                    """;

            String json2 = """
                    {
                        "data": []
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("Type mismatch"));
            assertTrue(error.getMessage().contains("OBJECT"));
            assertTrue(error.getMessage().contains("ARRAY"));
        }

        @Test
        @DisplayName("Should detect boolean vs string mismatch")
        void shouldDetectBooleanVsStringMismatch() {
            String json1 = """
                    {
                        "flag": true
                    }
                    """;

            String json2 = """
                    {
                        "flag": "true"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("Type mismatch"));
        }
    }

    @Nested
    @DisplayName("Partial JSON Contains")
    class PartialJsonContains {

        @Test
        @DisplayName("Should pass when all expected fields are present")
        void shouldPassWhenAllExpectedFieldsArePresent() {
            String expected = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementPrice": 10
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(expected, actual));
        }

        @Test
        @DisplayName("Should fail when expected field is missing")
        void shouldFailWhenExpectedFieldIsMissing() {
            String expected = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith"
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonContains(expected, actual));

            assertTrue(error.getMessage().contains("missing"));
            assertTrue(error.getMessage().contains("personLastName"));
        }

        @Test
        @DisplayName("Should allow extra fields in actual")
        void shouldAllowExtraFieldsInActual() {
            String expected = """
                    {
                        "personFirstName": "John"
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John",
                        "extraField": "extraValue"
                    }
                    """;

            // Не должно выбросить ошибку - дополнительные поля разрешены
            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(expected, actual));
        }

        @Test
        @DisplayName("Should check nested fields in partial comparison")
        void shouldCheckNestedFieldsInPartialComparison() {
            String expected = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(expected, actual));
        }

        @Test
        @DisplayName("Should use assertJsonContains for error responses with extra null fields")
        void shouldUseAssertJsonContainsForErrorResponsesWithExtraNullFields() {

            String expected = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;


            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(expected, actual));
        }
    }

    @Nested
    @DisplayName("Object to JSON Comparison")
    class ObjectToJsonComparison {

        @Test
        @DisplayName("Should compare objects as JSON successfully")
        void shouldCompareObjectsAsJsonSuccessfully() {
            TravelCalculatePremiumRequest request1 = new TravelCalculatePremiumRequest();
            request1.setPersonFirstName("John");
            request1.setPersonLastName("Smith");
            request1.setAgreementDateFrom(LocalDate.of(2023, 1, 1));
            request1.setAgreementDateTo(LocalDate.of(2023, 1, 11));

            TravelCalculatePremiumRequest request2 = new TravelCalculatePremiumRequest();
            request2.setPersonFirstName("John");
            request2.setPersonLastName("Smith");
            request2.setAgreementDateFrom(LocalDate.of(2023, 1, 1));
            request2.setAgreementDateTo(LocalDate.of(2023, 1, 11));

            assertDoesNotThrow(() ->
                    JsonComparator.assertObjectsEqualAsJson(request1, request2));
        }

        @Test
        @DisplayName("Should detect differences when comparing objects")
        void shouldDetectDifferencesWhenComparingObjects() {
            TravelCalculatePremiumRequest request1 = new TravelCalculatePremiumRequest();
            request1.setPersonFirstName("John");
            request1.setPersonLastName("Smith");

            TravelCalculatePremiumRequest request2 = new TravelCalculatePremiumRequest();
            request2.setPersonFirstName("Jane");
            request2.setPersonLastName("Smith");

            AssertionError error = assertThrows(AssertionError.class, () ->
                    JsonComparator.assertObjectsEqualAsJson(request1, request2));

            assertTrue(error.getMessage().contains("personFirstName"));
        }

        @Test
        @DisplayName("Should compare response object with JSON file")
        void shouldCompareResponseObjectWithJsonFile() throws Exception {
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            assertDoesNotThrow(() ->
                    JsonComparator.assertJsonEqualsFromFile("successful-response.json", response));
        }

        @Test
        @DisplayName("Should detect difference between object and JSON file")
        void shouldDetectDifferenceBetweenObjectAndJsonFile() throws Exception {
            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            response.setAgreementPrice(new BigDecimal("999"));

            AssertionError error = assertThrows(AssertionError.class, () ->
                    JsonComparator.assertJsonEqualsFromFile("successful-response.json", response));

            assertTrue(error.getMessage().contains("agreementPrice"));
        }

        @Test
        @DisplayName("Should compare error response objects correctly")
        void shouldCompareErrorResponseObjectsCorrectly() {
            TravelCalculatePremiumResponse response1 = new TravelCalculatePremiumResponse();
            response1.setErrors(List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            ));

            TravelCalculatePremiumResponse response2 = new TravelCalculatePremiumResponse();
            response2.setErrors(List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            ));

            assertDoesNotThrow(() ->
                    JsonComparator.assertObjectsEqualAsJson(response1, response2));
        }
    }

    @Nested
    @DisplayName("Partial JSON Builder")
    class PartialJsonBuilder {

        @Test
        @DisplayName("Should build JSON with string fields")
        void shouldBuildJsonWithStringFields() {
            String json = JsonComparator.partialJson()
                    .addField("firstName", "John")
                    .addField("lastName", "Smith")
                    .build();

            assertTrue(json.contains("\"firstName\":\"John\""));
            assertTrue(json.contains("\"lastName\":\"Smith\""));
        }

        @Test
        @DisplayName("Should build JSON with numeric fields")
        void shouldBuildJsonWithNumericFields() {
            String json = JsonComparator.partialJson()
                    .addField("price", 10)
                    .addField("count", 5)
                    .build();

            assertTrue(json.contains("\"price\":10"));
            assertTrue(json.contains("\"count\":5"));
        }

        @Test
        @DisplayName("Should build JSON with boolean fields")
        void shouldBuildJsonWithBooleanFields() {
            String json = JsonComparator.partialJson()
                    .addField("active", true)
                    .addField("deleted", false)
                    .build();

            assertTrue(json.contains("\"active\":true"));
            assertTrue(json.contains("\"deleted\":false"));
        }

        @Test
        @DisplayName("Should build JSON with null fields")
        void shouldBuildJsonWithNullFields() {
            String json = JsonComparator.partialJson()
                    .addField("value", null)
                    .build();

            assertTrue(json.contains("\"value\":null"));
        }

        @Test
        @DisplayName("Should build JSON with mixed field types")
        void shouldBuildJsonWithMixedFieldTypes() {
            String json = JsonComparator.partialJson()
                    .addField("name", "John")
                    .addField("age", 30)
                    .addField("active", true)
                    .addField("balance", null)
                    .build();

            assertTrue(json.contains("\"name\":\"John\""));
            assertTrue(json.contains("\"age\":30"));
            assertTrue(json.contains("\"active\":true"));
            assertTrue(json.contains("\"balance\":null"));
        }

        @Test
        @DisplayName("Should build partial JSON for error response checking")
        void shouldBuildPartialJsonForErrorResponseChecking() {
            String partialJson = JsonComparator.partialJson()
                    .addField("errors", "[{\"field\":\"personFirstName\"}]")
                    .build();

            String actualJson = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(partialJson, actualJson));
        }
    }

    @Nested
    @DisplayName("Error Messages Quality")
    class ErrorMessagesQuality {

        @Test
        @DisplayName("Error message should include path to difference")
        void errorMessageShouldIncludePathToDifference() {
            String json1 = """
                    {
                        "person": {
                            "address": {
                                "city": "London"
                            }
                        }
                    }
                    """;

            String json2 = """
                    {
                        "person": {
                            "address": {
                                "city": "Paris"
                            }
                        }
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("person.address.city"));
        }

        @Test
        @DisplayName("Error message should show expected and actual JSON")
        void errorMessageShouldShowExpectedAndActualJson() {
            String expected = "{\"value\": 10}";
            String actual = "{\"value\": 20}";

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("Expected JSON"));
            assertTrue(error.getMessage().contains("Actual JSON"));
        }

        @Test
        @DisplayName("Error message should pretty-print JSON")
        void errorMessageShouldPrettyPrintJson() {
            String expected = "{\"a\":1,\"b\":2}";
            String actual = "{\"a\":1,\"b\":3}";

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            String message = error.getMessage();
            assertTrue(message.contains("\n"));
        }

        @Test
        @DisplayName("Error message should show all differences for error response")
        void errorMessageShouldShowAllDifferencesForErrorResponse() {
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personLastName",
                                "message": "Different message!"
                            }
                        ]
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(expected, actual));

            assertTrue(error.getMessage().contains("errors[0].field"));
            assertTrue(error.getMessage().contains("errors[0].message"));
        }
    }

    @Nested
    @DisplayName("Integration with TestDataLoader")
    class IntegrationWithTestDataLoader {

        @Test
        @DisplayName("Should load and compare request JSONs")
        void shouldLoadAndCompareRequestJsons() throws Exception {
            String json1 = TestDataLoader.loadRequestAsString("valid-request.json");
            String json2 = TestDataLoader.loadRequestAsString("valid-request.json");

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should load and compare response JSONs")
        void shouldLoadAndCompareResponseJsons() throws Exception {
            String json1 = TestDataLoader.loadResponseAsString("successful-response.json");
            String json2 = TestDataLoader.loadResponseAsString("successful-response.json");

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should compare loaded objects")
        void shouldCompareLoadedObjects() throws Exception {
            TravelCalculatePremiumResponse response1 = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            TravelCalculatePremiumResponse response2 = TestDataLoader.loadResponse(
                    "successful-response.json",
                    TravelCalculatePremiumResponse.class
            );

            assertDoesNotThrow(() ->
                    JsonComparator.assertObjectsEqualAsJson(response1, response2));
        }

        @Test
        @DisplayName("Should load and compare error response with null fields")
        void shouldLoadAndCompareErrorResponseWithNullFields() throws Exception {
            String errorJson = TestDataLoader.loadResponseAsString("error-first-name-response.json");

            TravelCalculatePremiumResponse response = TestDataLoader.loadResponse(
                    "error-first-name-response.json",
                    TravelCalculatePremiumResponse.class
            );

            String responseJson = TestDataLoader.getObjectMapper().writeValueAsString(response);

            // Должно пройти - null поля должны совпадать
            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(errorJson, responseJson));
        }
    }

    @Nested
    @DisplayName("Real-World Scenarios from Production")
    class RealWorldScenarios {

        @Test
        @DisplayName("Scenario 1: Successful response comparison")
        void scenario1SuccessfulResponseComparison() {
            String expected = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Scenario 2: Error response with all null fields")
        void scenario2ErrorResponseWithAllNullFields() {
            String expected = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "personLastName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "agreementDateFrom",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "agreementDateTo",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "personLastName",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "agreementDateFrom",
                                "message": "Must not be empty!"
                            },
                            {
                                "field": "agreementDateTo",
                                "message": "Must not be empty!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Scenario 3: Partial check for error without caring about null fields")
        void scenario3PartialCheckForErrorWithoutCaringAboutNullFields() {
            String expectedErrorOnly = """
                    {
                        "errors": [
                            {
                                "field": "agreementDateTo",
                                "message": "Must be after agreementDateFrom!"
                            }
                        ]
                    }
                    """;

            String actualWithNullFields = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": [
                            {
                                "field": "agreementDateTo",
                                "message": "Must be after agreementDateFrom!"
                            }
                        ]
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonContains(
                    expectedErrorOnly, actualWithNullFields));
        }

        @Test
        @DisplayName("Scenario 5: Comparing objects from different sources")
        void scenario5ComparingObjectsFromDifferentSources() {
            TravelCalculatePremiumResponse manualResponse = new TravelCalculatePremiumResponse();
            manualResponse.setPersonFirstName(null);
            manualResponse.setPersonLastName(null);
            manualResponse.setAgreementDateFrom(null);
            manualResponse.setAgreementDateTo(null);
            manualResponse.setAgreementPrice(null);
            manualResponse.setErrors(List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            ));

            TravelCalculatePremiumResponse loadedResponse = new TravelCalculatePremiumResponse();
            loadedResponse.setErrors(List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            ));

            assertDoesNotThrow(() ->
                    JsonComparator.assertObjectsEqualAsJson(manualResponse, loadedResponse));
        }

        @Test
        @DisplayName("Scenario 6: Zero-price response validation")
        void scenario6ZeroPriceResponseValidation() {
            String expected = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-01",
                        "agreementPrice": 0,
                        "errors": null
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-01",
                        "agreementPrice": 0,
                        "errors": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Scenario 7: Special characters preservation")
        void scenario7SpecialCharactersPreservation() {
            String expected = """
                    {
                        "personFirstName": "Jean-Pierre",
                        "personLastName": "O'Connor",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "Jean-Pierre",
                        "personLastName": "O'Connor",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }

        @Test
        @DisplayName("Scenario 8: Cyrillic characters preservation")
        void scenario8CyrillicCharactersPreservation() {
            String expected = """
                    {
                        "personFirstName": "Иван",
                        "personLastName": "Петров",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            String actual = """
                    {
                        "personFirstName": "Иван",
                        "personLastName": "Петров",
                        "agreementDateFrom": "2023-01-01",
                        "agreementDateTo": "2023-01-11",
                        "agreementPrice": 10,
                        "errors": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(expected, actual));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Corner Cases")
    class EdgeCasesAndCornerCases {

        @Test
        @DisplayName("Should handle response with all fields as null")
        void shouldHandleResponseWithAllFieldsAsNull() {
            String json1 = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": null
                    }
                    """;

            String json2 = """
                    {
                        "personFirstName": null,
                        "personLastName": null,
                        "agreementDateFrom": null,
                        "agreementDateTo": null,
                        "agreementPrice": null,
                        "errors": null
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should handle empty error array")
        void shouldHandleEmptyErrorArray() {
            String json1 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "errors": []
                    }
                    """;

            String json2 = """
                    {
                        "personFirstName": "John",
                        "personLastName": "Smith",
                        "errors": []
                    }
                    """;

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should distinguish between null and empty array")
        void shouldDistinguishBetweenNullAndEmptyArray() {
            String json1 = """
                    {
                        "errors": null
                    }
                    """;

            String json2 = """
                    {
                        "errors": []
                    }
                    """;

            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("errors"));
        }

        @Test
        @DisplayName("Should handle very long error messages")
        void shouldHandleVeryLongErrorMessages() {
            String longMessage = "Error: " + "A".repeat(1000);

            String json1 = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "%s"
                            }
                        ]
                    }
                    """.formatted(longMessage);

            String json2 = """
                    {
                        "errors": [
                            {
                                "field": "personFirstName",
                                "message": "%s"
                            }
                        ]
                    }
                    """.formatted(longMessage);

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }

        @Test
        @DisplayName("Should handle multiple errors with different orders")
        void shouldHandleMultipleErrorsWithDifferentOrders() {
            String json1 = """
                    {
                        "errors": [
                            {"field": "field1", "message": "error1"},
                            {"field": "field2", "message": "error2"}
                        ]
                    }
                    """;

            String json2 = """
                    {
                        "errors": [
                            {"field": "field2", "message": "error2"},
                            {"field": "field1", "message": "error1"}
                        ]
                    }
                    """;

            // Порядок важен - должно упасть
            AssertionError error = assertThrows(AssertionError.class,
                    () -> JsonComparator.assertJsonEquals(json1, json2));

            assertTrue(error.getMessage().contains("errors[0]"));
        }

        @Test
        @DisplayName("Should handle BigDecimal precision")
        void shouldHandleBigDecimalPrecision() {
            String json1 = """
                    {
                        "agreementPrice": 10.0
                    }
                    """;

            String json2 = """
                    {
                        "agreementPrice": 10
                    }
                    """;

            // Jackson обрабатывает 10.0 и 10 как эквивалентные
            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }
    }

    @Nested
    @DisplayName("Performance and Optimization")
    class PerformanceAndOptimization {

        @Test
        @DisplayName("Should handle large JSON documents efficiently")
        void shouldHandleLargeJsonDocumentsEfficiently() {
            // Создаем большой JSON с множеством полей
            StringBuilder largeJson = new StringBuilder("{");
            for (int i = 0; i < 1000; i++) {
                if (i > 0) largeJson.append(",");
                largeJson.append("\"field").append(i).append("\":\"value").append(i).append("\"");
            }
            largeJson.append("}");

            String json1 = largeJson.toString();
            String json2 = largeJson.toString();

            // Должно работать быстро даже с большими документами
            long startTime = System.currentTimeMillis();
            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
            long endTime = System.currentTimeMillis();

            // Проверка что выполнилось быстро (< 1 секунды)
            assertTrue((endTime - startTime) < 1000, "Comparison took too long");
        }

        @Test
        @DisplayName("Should handle deeply nested structures efficiently")
        void shouldHandleDeeplyNestedStructuresEfficiently() {
            // Создаем глубоко вложенную структуру
            StringBuilder deepJson = new StringBuilder("{");
            for (int i = 0; i < 50; i++) {
                deepJson.append("\"level").append(i).append("\":{");
            }
            deepJson.append("\"value\":\"deep\"");
            for (int i = 0; i < 50; i++) {
                deepJson.append("}");
            }
            deepJson.append("}");

            String json1 = deepJson.toString();
            String json2 = deepJson.toString();

            assertDoesNotThrow(() -> JsonComparator.assertJsonEquals(json1, json2));
        }
    }
}