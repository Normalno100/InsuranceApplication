package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelCalculatePremiumRequestValidatorV2ImplTest {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository;

    @Mock
    private RiskTypeRepository riskTypeRepository;

    private TravelCalculatePremiumRequestValidatorV2Impl validator;

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidatorV2Impl(
                countryRepository,
                medicalRiskLimitLevelRepository,
                riskTypeRepository
        );
    }

    // ========== HAPPY PATH ==========

    @Test
    void shouldReturnEmptyListWhenAllFieldsValid() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        mockValidRepositories();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    // ========== PERSONAL INFO VALIDATION ==========

    @Test
    void shouldReturnErrorWhenFirstNameIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonFirstName(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
        assertEquals("Must not be empty!", errors.get(0).getMessage());
    }

    @Test
    void shouldReturnErrorWhenFirstNameIsEmpty() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonFirstName("");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenFirstNameIsBlank() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonFirstName("   ");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personFirstName", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenLastNameIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonLastName(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personLastName", errors.get(0).getField());
        assertEquals("Must not be empty!", errors.get(0).getMessage());
    }

    @Test
    void shouldReturnErrorWhenLastNameIsEmpty() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonLastName("");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personLastName", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenBirthDateIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonBirthDate(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("personBirthDate", errors.get(0).getField());
        assertEquals("Must not be empty!", errors.get(0).getMessage());
    }

    // ========== DATE VALIDATION ==========

    @Test
    void shouldReturnErrorWhenDateFromIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setAgreementDateFrom(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "agreementDateFrom".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenDateToIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setAgreementDateTo(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "agreementDateTo".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenDateToBeforeDateFrom() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setAgreementDateFrom(LocalDate.of(2025, 6, 10));
        request.setAgreementDateTo(LocalDate.of(2025, 6, 5));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("agreementDateTo", errors.get(0).getField());
        assertEquals("Must be after agreementDateFrom!", errors.get(0).getMessage());
    }

    @Test
    void shouldAcceptWhenDateToEqualDateFrom() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        LocalDate sameDate = LocalDate.of(2025, 6, 10);
        request.setAgreementDateFrom(sameDate);
        request.setAgreementDateTo(sameDate);
        mockValidRepositories();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "agreementDateTo".equals(e.getField())));
    }

    // ========== COUNTRY VALIDATION ==========

    @Test
    void shouldReturnErrorWhenCountryIsoCodeIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setCountryIsoCode(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("countryIsoCode", errors.get(0).getField());
        assertEquals("Must not be empty!", errors.get(0).getMessage());
    }

    @Test
    void shouldReturnErrorWhenCountryIsoCodeIsEmpty() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setCountryIsoCode("");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("countryIsoCode", errors.get(0).getField());
    }

    @Test
    void shouldReturnErrorWhenCountryNotFoundInDatabase() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setCountryIsoCode("XX");

        when(countryRepository.findActiveByIsoCode(eq("XX"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("countryIsoCode", errors.get(0).getField());
        assertTrue(errors.get(0).getMessage().contains("not found or not active"));
    }

    @Test
    void shouldTrimCountryIsoCode() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setCountryIsoCode("  ES  ");

        when(countryRepository.findActiveByIsoCode(eq("ES"), any(LocalDate.class)))
                .thenReturn(Optional.of(createCountryEntity()));
        mockValidMedicalLevel();
        mockValidRisks();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "countryIsoCode".equals(e.getField())));
    }

    // ========== MEDICAL LEVEL VALIDATION ==========

    @Test
    void shouldReturnErrorWhenMedicalLevelIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setMedicalRiskLimitLevel(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenMedicalLevelIsEmpty() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setMedicalRiskLimitLevel("");

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenMedicalLevelNotFoundInDatabase() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setMedicalRiskLimitLevel("999999");

        mockValidCountry();
        when(medicalRiskLimitLevelRepository.findActiveByCode(eq("999999"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField())
                        && e.getMessage().contains("not found or not active")));
    }

    // ========== RISK VALIDATION ==========

    @Test
    void shouldAcceptNullSelectedRisks() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(null);
        mockValidRepositories();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldAcceptEmptySelectedRisks() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of());
        mockValidRepositories();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenRiskCodeIsNull() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of((String) null));
        mockValidCountry();
        mockValidMedicalLevel();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())
                        && e.getMessage().contains("cannot be empty")));
    }

    @Test
    void shouldReturnErrorWhenRiskCodeIsEmpty() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of(""));
        mockValidCountry();
        mockValidMedicalLevel();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenRiskCodeIsBlank() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("   "));
        mockValidCountry();
        mockValidMedicalLevel();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldReturnErrorWhenRiskNotFoundInDatabase() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("UNKNOWN_RISK"));
        mockValidCountry();
        mockValidMedicalLevel();

        when(riskTypeRepository.findActiveByCode(eq("UNKNOWN_RISK"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())
                        && e.getMessage().contains("not found or not active")));
    }

    @Test
    void shouldReturnErrorWhenRiskIsMandatory() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("TRAVEL_MEDICAL"));
        mockValidCountry();
        mockValidMedicalLevel();

        RiskTypeEntity mandatoryRisk = createRiskTypeEntity("TRAVEL_MEDICAL", true);
        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any(LocalDate.class)))
                .thenReturn(Optional.of(mandatoryRisk));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())
                        && e.getMessage().contains("mandatory")));
    }

    @Test
    void shouldAcceptValidOptionalRisk() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("SPORT_ACTIVITIES"));
        mockValidCountry();
        mockValidMedicalLevel();

        RiskTypeEntity optionalRisk = createRiskTypeEntity("SPORT_ACTIVITIES", false);
        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(optionalRisk));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldValidateMultipleRisks() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"));
        mockValidCountry();
        mockValidMedicalLevel();

        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(createRiskTypeEntity("SPORT_ACTIVITIES", false)));
        when(riskTypeRepository.findActiveByCode(eq("LUGGAGE_LOSS"), any(LocalDate.class)))
                .thenReturn(Optional.of(createRiskTypeEntity("LUGGAGE_LOSS", false)));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertFalse(errors.stream()
                .anyMatch(e -> "selectedRisks".equals(e.getField())));
    }

    @Test
    void shouldStopValidatingRisksAfterFirstError() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setSelectedRisks(List.of("", "VALID_RISK"));
        mockValidCountry();
        mockValidMedicalLevel();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        // Должна быть только одна ошибка (пустой код), валидация останавливается
        long riskErrors = errors.stream()
                .filter(e -> "selectedRisks".equals(e.getField()))
                .count();
        assertEquals(1, riskErrors);
    }

    // ========== MULTIPLE ERRORS ==========

    @Test
    void shouldReturnAllErrorsWhenMultipleFieldsInvalid() {
        // Given
        TravelCalculatePremiumRequestV2 request = TravelCalculatePremiumRequestV2.builder()
                .personFirstName(null)
                .personLastName("")
                .personBirthDate(null)
                .agreementDateFrom(null)
                .agreementDateTo(null)
                .countryIsoCode(null)
                .medicalRiskLimitLevel(null)
                .build();

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.size() >= 5); // At least 5 validation errors
        assertTrue(errors.stream().anyMatch(e -> "personFirstName".equals(e.getField())));
        assertTrue(errors.stream().anyMatch(e -> "personLastName".equals(e.getField())));
        assertTrue(errors.stream().anyMatch(e -> "personBirthDate".equals(e.getField())));
    }

    @Test
    void shouldContinueValidationAfterFirstError() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonFirstName(null);
        request.setPersonLastName(null);

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(2, errors.size());
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumRequestV2 createValidRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    private void mockValidRepositories() {
        mockValidCountry();
        mockValidMedicalLevel();
        mockValidRisks();
    }

    private void mockValidCountry() {
        when(countryRepository.findActiveByIsoCode(any(String.class), any(LocalDate.class)))
                .thenReturn(Optional.of(createCountryEntity()));
    }

    private void mockValidMedicalLevel() {
        when(medicalRiskLimitLevelRepository.findActiveByCode(any(String.class), any(LocalDate.class)))
                .thenReturn(Optional.of(createMedicalLevelEntity()));
    }

    private void mockValidRisks() {
        when(riskTypeRepository.findActiveByCode(any(String.class), any(LocalDate.class)))
                .thenReturn(Optional.of(createRiskTypeEntity("SPORT_ACTIVITIES", false)));
    }

    private CountryEntity createCountryEntity() {
        CountryEntity entity = new CountryEntity();
        entity.setIsoCode("ES");
        entity.setNameEn("Spain");
        entity.setRiskCoefficient(new BigDecimal("1.0"));
        return entity;
    }

    private MedicalRiskLimitLevelEntity createMedicalLevelEntity() {
        MedicalRiskLimitLevelEntity entity = new MedicalRiskLimitLevelEntity();
        entity.setCode("50000");
        entity.setCoverageAmount(new BigDecimal("50000"));
        entity.setDailyRate(new BigDecimal("4.5"));
        return entity;
    }

    private RiskTypeEntity createRiskTypeEntity(String code, boolean mandatory) {
        RiskTypeEntity entity = new RiskTypeEntity();
        entity.setCode(code);
        entity.setNameEn("Test Risk");
        entity.setCoefficient(new BigDecimal("0.3"));
        entity.setIsMandatory(mandatory);
        return entity;
    }
}