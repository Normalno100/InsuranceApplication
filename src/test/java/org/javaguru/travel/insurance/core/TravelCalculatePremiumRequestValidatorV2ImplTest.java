package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Упрощённые тесты валидатора - проверяем только бизнес-правила
 */
@ExtendWith(MockitoExtension.class)
class TravelCalculatePremiumRequestValidatorV2ImplTest {

    @Mock
    private CountryRepository countryRepository;
    @Mock
    private MedicalRiskLimitLevelRepository medicalRepository;
    @Mock
    private RiskTypeRepository riskRepository;

    @InjectMocks
    private TravelCalculatePremiumRequestValidatorV2Impl validator;

    // ========== HAPPY PATH ==========

    @Test
    void shouldPassValidation_whenAllFieldsValid() {
        var request = validRequest();
        mockAllValid();

        var errors = validator.validate(request);

        assertThat(errors).isEmpty();
    }

    // ========== FIELD VALIDATION (1 тест на тип валидации) ==========

    @Test
    void shouldFail_whenRequiredFieldMissing() {
        var request = validRequest();
        request.setPersonFirstName(null);
        mockAllValid(); // Моки чтобы избежать ошибок из репозиториев

        var errors = validator.validate(request);

        assertThat(errors)
                .isNotEmpty()
                .anyMatch(e -> "personFirstName".equals(e.getField())
                        && e.getMessage().contains("Must not be empty"));
    }


    @Test
    void shouldFail_whenDateToBeforeDateFrom() {
        var request = validRequest();
        request.setAgreementDateFrom(LocalDate.of(2025, 6, 10));
        request.setAgreementDateTo(LocalDate.of(2025, 6, 5));

        var errors = validator.validate(request);

        assertThat(errors)
                .anyMatch(e -> e.getField().equals("agreementDateTo")
                        && e.getMessage().contains("Must be after"));
    }

    // ========== REPOSITORY VALIDATION ==========

    @Test
    void shouldFail_whenCountryNotFound() {
        var request = validRequest();
        request.setCountryIsoCode("XX");
        when(countryRepository.findActiveByIsoCode(eq("XX"), any()))
                .thenReturn(Optional.empty());

        var errors = validator.validate(request);

        assertThat(errors)
                .anyMatch(e -> e.getField().equals("countryIsoCode")
                        && e.getMessage().contains("not found"));
    }

    @Test
    void shouldFail_whenMedicalLevelNotFound() {
        var request = validRequest();
        mockValidCountry();
        when(medicalRepository.findActiveByCode(any(), any()))
                .thenReturn(Optional.empty());

        var errors = validator.validate(request);

        assertThat(errors)
                .anyMatch(e -> e.getField().equals("medicalRiskLimitLevel"));
    }

    @Test
    void shouldFail_whenOptionalRiskNotFound() {
        var request = validRequest();
        request.setSelectedRisks(List.of("UNKNOWN_RISK"));
        mockValidCountry();
        mockValidMedicalLevel();
        when(riskRepository.findActiveByCode(any(), any()))
                .thenReturn(Optional.empty());

        var errors = validator.validate(request);

        assertThat(errors)
                .anyMatch(e -> e.getField().equals("selectedRisks")
                        && e.getMessage().contains("not found"));
    }

    @Test
    void shouldFail_whenMandatoryRiskSelected() {
        var request = validRequest();
        request.setSelectedRisks(List.of("TRAVEL_MEDICAL"));
        mockValidCountry();
        mockValidMedicalLevel();

        var mandatoryRisk = new RiskTypeEntity();
        mandatoryRisk.setCode("TRAVEL_MEDICAL");
        mandatoryRisk.setIsMandatory(true);
        when(riskRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                .thenReturn(Optional.of(mandatoryRisk));

        var errors = validator.validate(request);

        assertThat(errors)
                .anyMatch(e -> e.getField().equals("selectedRisks")
                        && e.getMessage().contains("mandatory"));
    }

    // ========== MULTIPLE ERRORS ==========

    @Test
    void shouldReturnAllErrors_whenMultipleFieldsInvalid() {
        var request = TravelCalculatePremiumRequestV2.builder()
                .personFirstName(null)
                .personLastName(null)
                .personBirthDate(null)
                .agreementDateFrom(null)
                .agreementDateTo(null)
                .countryIsoCode(null)
                .medicalRiskLimitLevel(null)
                .build();

        var errors = validator.validate(request);

        assertThat(errors).hasSizeGreaterThanOrEqualTo(5);
    }

    // ========== HELPERS ==========

    private TravelCalculatePremiumRequestV2 validRequest() {
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

    private void mockAllValid() {
        mockValidCountry();
        mockValidMedicalLevel();
    }

    private void mockValidCountry() {
        var country = new CountryEntity();
        country.setIsoCode("ES");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        when(countryRepository.findActiveByIsoCode(any(), any()))
                .thenReturn(Optional.of(country));
    }

    private void mockValidMedicalLevel() {
        var level = new MedicalRiskLimitLevelEntity();
        level.setCode("50000");
        when(medicalRepository.findActiveByCode(any(), any()))
                .thenReturn(Optional.of(level));
    }
}