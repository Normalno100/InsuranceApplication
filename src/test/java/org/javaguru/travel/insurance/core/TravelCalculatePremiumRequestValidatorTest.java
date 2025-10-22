package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TravelCalculatePremiumRequestValidator Tests")
public class TravelCalculatePremiumRequestValidatorTest {

    private TravelCalculatePremiumRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidator();
    }

    @Nested
    @DisplayName("PersonFirstName Validation")
    class PersonFirstNameValidation {

        @Test
        @DisplayName("Should return error when personFirstName is null")
        void shouldReturnErrorWhenPersonFirstNameIsNull() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personFirstName", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @Test
        @DisplayName("Should return error when personFirstName is empty string")
        void shouldReturnErrorWhenPersonFirstNameIsEmpty() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("");

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personFirstName", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return error for null and empty personFirstName")
        void shouldReturnErrorForNullAndEmpty(String firstName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(firstName);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personFirstName", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should accept valid personFirstName")
        void shouldAcceptValidPersonFirstName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("John");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"John", "Jean-Pierre", "Mary Ann", "Иван", "José", "O'Brien"})
        @DisplayName("Should accept various valid first names")
        void shouldAcceptVariousValidFirstNames(String firstName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(firstName);

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personFirstName with spaces")
        void shouldAcceptPersonFirstNameWithSpaces() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("Mary Ann");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personFirstName with hyphen")
        void shouldAcceptPersonFirstNameWithHyphen() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("Jean-Pierre");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personFirstName with apostrophe")
        void shouldAcceptPersonFirstNameWithApostrophe() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("D'Angelo");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personFirstName in Cyrillic")
        void shouldAcceptPersonFirstNameInCyrillic() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("Иван");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long personFirstName")
        void shouldAcceptVeryLongPersonFirstName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("A".repeat(100));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept single character personFirstName")
        void shouldAcceptSingleCharacterPersonFirstName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("A");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("PersonLastName Validation")
    class PersonLastNameValidation {

        @Test
        @DisplayName("Should return error when personLastName is null")
        void shouldReturnErrorWhenPersonLastNameIsNull() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personLastName", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @Test
        @DisplayName("Should return error when personLastName is empty string")
        void shouldReturnErrorWhenPersonLastNameIsEmpty() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("");

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personLastName", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("Should return error for null and empty personLastName")
        void shouldReturnErrorForNullAndEmpty(String lastName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName(lastName);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personLastName", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should accept valid personLastName")
        void shouldAcceptValidPersonLastName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("Smith");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Smith", "O'Connor", "van der Berg", "Петров", "García", "McDonald"})
        @DisplayName("Should accept various valid last names")
        void shouldAcceptVariousValidLastNames(String lastName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName(lastName);

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personLastName with spaces")
        void shouldAcceptPersonLastNameWithSpaces() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("von Neumann");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personLastName with hyphen")
        void shouldAcceptPersonLastNameWithHyphen() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("Smith-Jones");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personLastName with apostrophe")
        void shouldAcceptPersonLastNameWithApostrophe() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("O'Brien");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept personLastName in Cyrillic")
        void shouldAcceptPersonLastNameInCyrillic() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("Петров");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept very long personLastName")
        void shouldAcceptVeryLongPersonLastName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("B".repeat(100));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept single character personLastName")
        void shouldAcceptSingleCharacterPersonLastName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName("B");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("AgreementDateFrom Validation")
    class AgreementDateFromValidation {

        @Test
        @DisplayName("Should return error when agreementDateFrom is null")
        void shouldReturnErrorWhenAgreementDateFromIsNull() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("agreementDateFrom", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @Test
        @DisplayName("Should accept valid agreementDateFrom")
        void shouldAcceptValidAgreementDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now());

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept agreementDateFrom in the past")
        void shouldAcceptAgreementDateFromInThePast() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().minusDays(10));
            request.setAgreementDateTo(LocalDate.now());

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept agreementDateFrom in the future")
        void shouldAcceptAgreementDateFromInTheFuture() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().plusDays(10));
            request.setAgreementDateTo(LocalDate.now().plusDays(20));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept agreementDateFrom far in the future")
        void shouldAcceptAgreementDateFromFarInTheFuture() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().plusYears(10));
            request.setAgreementDateTo(LocalDate.now().plusYears(10).plusDays(30));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept leap year date (Feb 29)")
        void shouldAcceptLeapYearDate() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2024, 2, 29));
            request.setAgreementDateTo(LocalDate.of(2024, 3, 10));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }
    }

    @Nested
    @DisplayName("AgreementDateTo Validation")
    class AgreementDateToValidation {

        @Test
        @DisplayName("Should return error when agreementDateTo is null")
        void shouldReturnErrorWhenAgreementDateToIsNull() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateTo(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("agreementDateTo", errors.get(0).getField());
            assertEquals("Must not be empty!", errors.get(0).getMessage());
        }

        @Test
        @DisplayName("Should return error when agreementDateTo is before agreementDateFrom")
        void shouldReturnErrorWhenAgreementDateToIsBeforeAgreementDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().plusDays(10));
            request.setAgreementDateTo(LocalDate.now());

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("agreementDateTo", errors.get(0).getField());
            assertEquals("Must be after agreementDateFrom!", errors.get(0).getMessage());
        }

        @Test
        @DisplayName("Should accept agreementDateTo after agreementDateFrom")
        void shouldAcceptAgreementDateToAfterAgreementDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now().plusDays(10));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept agreementDateTo equal to agreementDateFrom")
        void shouldAcceptAgreementDateToEqualToAgreementDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();
            LocalDate sameDate = LocalDate.now();
            request.setAgreementDateFrom(sameDate);
            request.setAgreementDateTo(sameDate);

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept agreementDateTo one day after agreementDateFrom")
        void shouldAcceptAgreementDateToOneDayAfter() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now().plusDays(1));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should accept long period (365+ days)")
        void shouldAcceptLongPeriod() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now().plusDays(365));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should return error when dateFrom is null but dateTo is before current implicit dateFrom")
        void shouldReturnErrorWhenDateFromIsNullAndDateToIsInvalid() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().plusDays(10));
            request.setAgreementDateTo(LocalDate.now());

            List<ValidationError> errors = validator.validate(request);

            assertFalse(errors.isEmpty());
            assertTrue(errors.stream()
                    .anyMatch(e -> e.getField().equals("agreementDateTo")));
        }
    }

    @Nested
    @DisplayName("Combined Validation")
    class CombinedValidation {

        @Test
        @DisplayName("Should return no errors for completely valid request")
        void shouldReturnNoErrorsForCompletelyValidRequest() {
            TravelCalculatePremiumRequest request = validRequest();

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should return all errors when all fields are invalid")
        void shouldReturnAllErrorsWhenAllFieldsAreInvalid() {
            TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
            request.setPersonFirstName(null);
            request.setPersonLastName(null);
            request.setAgreementDateFrom(null);
            request.setAgreementDateTo(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(4, errors.size());
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("personFirstName")));
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("personLastName")));
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("agreementDateFrom")));
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("agreementDateTo")));
        }

        @Test
        @DisplayName("Should return multiple errors for multiple invalid fields")
        void shouldReturnMultipleErrorsForMultipleInvalidFields() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("");
            request.setPersonLastName("");

            List<ValidationError> errors = validator.validate(request);

            assertEquals(2, errors.size());
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("personFirstName")));
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("personLastName")));
        }

        @Test
        @DisplayName("Should return error only for invalid firstName when others are valid")
        void shouldReturnErrorOnlyForInvalidFirstName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personFirstName", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should return error only for invalid lastName when others are valid")
        void shouldReturnErrorOnlyForInvalidLastName() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("personLastName", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should return error only for invalid dateFrom when others are valid")
        void shouldReturnErrorOnlyForInvalidDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("agreementDateFrom", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should return error only for invalid dateTo when others are valid")
        void shouldReturnErrorOnlyForInvalidDateTo() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateTo(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertEquals("agreementDateTo", errors.get(0).getField());
        }

        @Test
        @DisplayName("Should return errors for names and dateTo (3 errors)")
        void shouldReturnThreeErrors() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("");
            request.setPersonLastName("");
            request.setAgreementDateTo(null);

            List<ValidationError> errors = validator.validate(request);

            assertEquals(3, errors.size());
        }

        @Test
        @DisplayName("Should return only 'from' error when from is null and to is before now")
        void shouldReturnOnlyFromErrorWhenFromIsNull() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(null);
            request.setAgreementDateTo(LocalDate.now().minusDays(10));

            List<ValidationError> errors = validator.validate(request);

            assertEquals(1, errors.size());
            assertTrue(errors.stream().anyMatch(e -> e.getField().equals("agreementDateFrom")));
        }

    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle request with all fields at minimum valid values")
        void shouldHandleMinimumValidValues() {
            TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
            request.setPersonFirstName("A");
            request.setPersonLastName("B");
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now());

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should handle request with maximum realistic period (10 years)")
        void shouldHandleMaximumRealisticPeriod() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now().plusYears(10));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should handle names with mixed case")
        void shouldHandleNamesWithMixedCase() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("jOhN");
            request.setPersonLastName("sMiTh");

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should handle names with numbers (if allowed)")
        void shouldHandleNamesWithNumbers() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("John2");
            request.setPersonLastName("Smith3");

            List<ValidationError> errors = validator.validate(request);

            // Предполагаем, что цифры в именах допустимы
            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should validate dates across year boundary")
        void shouldValidateDatesAcrossYearBoundary() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2023, 12, 25));
            request.setAgreementDateTo(LocalDate.of(2024, 1, 5));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }

        @Test
        @DisplayName("Should validate dates in leap year")
        void shouldValidateDatesInLeapYear() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2024, 2, 28));
            request.setAgreementDateTo(LocalDate.of(2024, 3, 1));

            List<ValidationError> errors = validator.validate(request);

            assertTrue(errors.isEmpty());
        }
    }

    private TravelCalculatePremiumRequest validRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Smith");
        request.setAgreementDateFrom(LocalDate.now());
        request.setAgreementDateTo(LocalDate.now().plusDays(10));
        return request;
    }
}