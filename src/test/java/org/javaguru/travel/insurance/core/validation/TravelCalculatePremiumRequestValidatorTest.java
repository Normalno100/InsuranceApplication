package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelCalculatePremiumRequestValidatorTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository;

    @Mock
    private RiskTypeRepository riskRepository;

    @Mock
    ReferenceDataPort referenceDataPort;

    private TravelCalculatePremiumRequestValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidator(
                countryRepository,
                medicalRiskLimitLevelRepository,
                riskRepository,
                referenceDataPort
        );
    }

    @Test
    void shouldReturnError_whenPersonFirstNameIsNull() {
        // Given
        TravelCalculatePremiumRequest request = createValidRequest();
        request.setPersonFirstName(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream()
                .anyMatch(e -> "personFirstName".equals(e.getField())));
    }

    @Test
    void shouldReturnError_whenPersonFirstNameIsBlank() {
        // Given
        TravelCalculatePremiumRequest request = createValidRequest();
        request.setPersonFirstName("   ");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream()
                .anyMatch(e -> "personFirstName".equals(e.getField())));
    }

    @Test
    void shouldReturnSingleCriticalError_whenFirstFieldIsNull() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName(null);

        List<ValidationError> errors = validator.validate(request);

        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
    }


    // Helper methods

    private TravelCalculatePremiumRequest createValidRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Doe");
        request.setPersonBirthDate(LocalDate.of(1990, 1, 1));
        request.setAgreementDateFrom(LocalDate.now().plusDays(1));
        request.setAgreementDateTo(LocalDate.now().plusDays(10));
        request.setCountryIsoCode("ES");
        request.setMedicalRiskLimitLevel("LEVEL_10000");
        request.setSelectedRisks(List.of("TRAVEL_CANCELLATION","MEDICAL"));
        return request;
    }

    private CountryEntity createCountryEntity() {
        CountryEntity entity = new CountryEntity();
        entity.setIsoCode("ES");
        entity.setNameEn("Spain");
        return entity;
    }

    private MedicalRiskLimitLevelEntity createMedicalLevelEntity() {
        MedicalRiskLimitLevelEntity entity = new MedicalRiskLimitLevelEntity();
        entity.setCode("LEVEL_10000");
        return entity;
    }

    private RiskTypeEntity createRiskTypeEntity(){
        RiskTypeEntity entity = new RiskTypeEntity();
        entity.setCode("TRAVEL_CANCELLATION");
        return entity;
    }
}