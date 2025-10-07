package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TravelCalculatePremiumRequestValidatorTest {

    private final TravelCalculatePremiumRequestValidator requestValidator =
            new TravelCalculatePremiumRequestValidator();

    // ---------- personFirstName ----------

    @Test
    void shouldReturnErrorWhenPersonFirstNameIsNull() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonFirstName(null);

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenPersonFirstNameIsEmpty() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonFirstName("");

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
    }

    @Test
    void shouldNotReturnErrorWhenPersonFirstNameIsPresent() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonFirstName("John");

        List<ValidationError> errors = requestValidator.validate(request);

        assertTrue(errors.isEmpty());
    }

    // ---------- personLastName ----------

    @Test
    void shouldReturnErrorWhenPersonLastNameIsNull() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonLastName(null);

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("personLastName", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenPersonLastNameIsEmpty() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonLastName("");

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("personLastName", errors.get(0).getField());
    }

    @Test
    void shouldNotReturnErrorWhenPersonLastNameIsPresent() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setPersonLastName("Smith");

        List<ValidationError> errors = requestValidator.validate(request);

        assertTrue(errors.isEmpty());
    }

    // ---------- agreementDateFrom ----------

    @Test
    void shouldReturnErrorWhenAgreementDateFromIsNull() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setAgreementDateFrom(null);

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("agreementDateFrom", errors.get(0).getField());
    }


    @Test
    void shouldNotReturnErrorWhenAgreementDateFromIsValid() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setAgreementDateFrom(LocalDate.now());

        List<ValidationError> errors = requestValidator.validate(request);

        assertTrue(errors.isEmpty());
    }

    // ---------- agreementDateTo ----------

    @Test
    void shouldReturnErrorWhenAgreementDateToIsNull() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setAgreementDateTo(null);

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("agreementDateTo", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenAgreementDateToIsBeforeAgreementDateFrom() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setAgreementDateFrom(LocalDate.now().plusDays(10)); // будущее
        request.setAgreementDateTo(LocalDate.now()); // прошлое

        List<ValidationError> errors = requestValidator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("agreementDateTo", errors.get(0).getField());
    }

    @Test
    void shouldNotReturnErrorWhenAgreementDateToIsAfterAgreementDateFrom() {
        TravelCalculatePremiumRequest request = validRequest();
        request.setAgreementDateFrom(LocalDate.now());
        request.setAgreementDateTo(LocalDate.now().plusDays(10));

        List<ValidationError> errors = requestValidator.validate(request);

        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldNotReturnErrorWhenAgreementDateToEqualsAgreementDateFrom() {
        TravelCalculatePremiumRequest request = validRequest();
        LocalDate sameDate = LocalDate.now();
        request.setAgreementDateFrom(sameDate);
        request.setAgreementDateTo(sameDate);

        List<ValidationError> errors = requestValidator.validate(request);

        assertTrue(errors.isEmpty());
    }

    // ---------- helper ----------

    private TravelCalculatePremiumRequest validRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Smith");
        request.setAgreementDateFrom(LocalDate.now());
        request.setAgreementDateTo(LocalDate.now().plusDays(10));
        return request;
    }
}
