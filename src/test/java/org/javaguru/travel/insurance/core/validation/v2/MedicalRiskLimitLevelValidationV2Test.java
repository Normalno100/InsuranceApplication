package org.javaguru.travel.insurance.core.validation.v2;

import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
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
class MedicalRiskLimitLevelValidationV2Test {

    @Mock
    private MedicalRiskLimitLevelRepository repository;

    @InjectMocks
    private MedicalRiskLimitLevelValidationV2 validation;

    @Test
    void shouldReturnErrorWhenMedicalRiskLimitLevelIsNull() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel(null);
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("medicalRiskLimitLevel", result.get().getField());
        assertEquals("Must not be empty!", result.get().getMessage());
    }

    @Test
    void shouldReturnErrorWhenMedicalRiskLimitLevelIsEmpty() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel("   ");
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("medicalRiskLimitLevel", result.get().getField());
    }

    @Test
    void shouldReturnErrorWhenLevelNotFoundInDatabase() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel("999999");
        request.setAgreementDateFrom(LocalDate.now());

        when(repository.findActiveByCode(eq("999999"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("medicalRiskLimitLevel", result.get().getField());
        assertTrue(result.get().getMessage().contains("not found or not active"));
    }

    @Test
    void shouldReturnEmptyWhenLevelExistsAndActive() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel("10000");
        request.setAgreementDateFrom(LocalDate.now());

        MedicalRiskLimitLevelEntity level = new MedicalRiskLimitLevelEntity();
        level.setCode("10000");
        level.setCoverageAmount(new BigDecimal("10000"));
        level.setDailyRate(new BigDecimal("2.00"));
        level.setValidFrom(LocalDate.now().minusYears(1));

        when(repository.findActiveByCode(eq("10000"), any(LocalDate.class)))
                .thenReturn(Optional.of(level));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldValidateWithWhitespaceInCode() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel("  10000  ");
        request.setAgreementDateFrom(LocalDate.now());

        MedicalRiskLimitLevelEntity level = new MedicalRiskLimitLevelEntity();
        level.setCode("10000");

        when(repository.findActiveByCode(eq("10000"), any(LocalDate.class)))
                .thenReturn(Optional.of(level));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }
}