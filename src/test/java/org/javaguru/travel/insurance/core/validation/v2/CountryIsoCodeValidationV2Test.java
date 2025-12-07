package org.javaguru.travel.insurance.core.validation.v2;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CountryIsoCodeValidationV2Test {

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private CountryIsoCodeValidationV2 validation;

    @Test
    void shouldReturnErrorWhenCountryIsoCodeIsNull() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setCountryIsoCode(null);
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("countryIsoCode", result.get().getField());
        assertEquals("Must not be empty!", result.get().getMessage());
    }

    @Test
    void shouldReturnErrorWhenCountryIsoCodeIsEmpty() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setCountryIsoCode("   ");
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("countryIsoCode", result.get().getField());
        assertEquals("Must not be empty!", result.get().getMessage());
    }

    @Test
    void shouldReturnErrorWhenCountryNotFoundInDatabase() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setCountryIsoCode("XX");
        request.setAgreementDateFrom(LocalDate.now());

        when(countryRepository.findActiveByIsoCode(eq("XX"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("countryIsoCode", result.get().getField());
        assertTrue(result.get().getMessage().contains("not found or not active"));
    }

    @Test
    void shouldReturnEmptyWhenCountryExistsAndActive() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setCountryIsoCode("ES");
        request.setAgreementDateFrom(LocalDate.now());

        CountryEntity country = new CountryEntity();
        country.setIsoCode("ES");
        country.setNameEn("Spain");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setValidFrom(LocalDate.now().minusYears(1));

        when(countryRepository.findActiveByIsoCode(eq("ES"), any(LocalDate.class)))
                .thenReturn(Optional.of(country));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleLowercaseCountryCode() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setCountryIsoCode("es");
        request.setAgreementDateFrom(LocalDate.now());

        CountryEntity country = new CountryEntity();
        country.setIsoCode("ES");
        country.setNameEn("Spain");

        when(countryRepository.findActiveByIsoCode(eq("ES"), any(LocalDate.class)))
                .thenReturn(Optional.of(country));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }
}