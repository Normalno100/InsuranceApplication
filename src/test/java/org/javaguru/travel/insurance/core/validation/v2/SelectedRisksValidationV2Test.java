package org.javaguru.travel.insurance.core.validation.v2;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SelectedRisksValidationV2Test {

    @Mock
    private RiskTypeRepository riskTypeRepository;

    @InjectMocks
    private SelectedRisksValidationV2 validation;

    @Test
    void shouldReturnEmptyWhenSelectedRisksIsNull() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(null);
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenSelectedRisksIsEmpty() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of());
        request.setAgreementDateFrom(LocalDate.now());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldReturnErrorWhenRiskCodeIsNull() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(Arrays.asList("SPORT_ACTIVITIES", null, "LUGGAGE_LOSS"));
        request.setAgreementDateFrom(LocalDate.now());

        // НУЖЕН только один мок — для первого риска:
        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(createOptionalRisk("SPORT_ACTIVITIES", "0.3")));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("selectedRisks", result.get().getField());
        assertEquals("Risk code cannot be empty!", result.get().getMessage());    }


    @Test
    void shouldReturnErrorWhenRiskCodeIsEmpty() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "   ", "LUGGAGE_LOSS"));
        request.setAgreementDateFrom(LocalDate.now());

        // НУЖЕН только один мок — для первого риска:
        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(createOptionalRisk("SPORT_ACTIVITIES", "0.3")));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("selectedRisks", result.get().getField());
        assertEquals("Risk code cannot be empty!", result.get().getMessage());
    }


    @Test
    void shouldReturnErrorWhenRiskNotFoundInDatabase() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("INVALID_RISK"));
        request.setAgreementDateFrom(LocalDate.now());

        when(riskTypeRepository.findActiveByCode(eq("INVALID_RISK"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("selectedRisks", result.get().getField());
        assertTrue(result.get().getMessage().contains("INVALID_RISK"));
        assertTrue(result.get().getMessage().contains("not found or not active"));
    }

    @Test
    void shouldReturnErrorWhenRiskIsMandatory() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("TRAVEL_MEDICAL"));
        request.setAgreementDateFrom(LocalDate.now());

        RiskTypeEntity mandatoryRisk = new RiskTypeEntity();
        mandatoryRisk.setCode("TRAVEL_MEDICAL");
        mandatoryRisk.setNameEn("Medical Coverage");
        mandatoryRisk.setCoefficient(BigDecimal.ZERO);
        mandatoryRisk.setIsMandatory(true);
        mandatoryRisk.setValidFrom(LocalDate.now().minusYears(1));

        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any(LocalDate.class)))
                .thenReturn(Optional.of(mandatoryRisk));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertEquals("selectedRisks", result.get().getField());
        assertTrue(result.get().getMessage().contains("TRAVEL_MEDICAL"));
        assertTrue(result.get().getMessage().contains("mandatory"));
    }

    @Test
    void shouldReturnEmptyWhenAllRisksAreValid() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"));
        request.setAgreementDateFrom(LocalDate.now());

        RiskTypeEntity sportRisk = createOptionalRisk("SPORT_ACTIVITIES", "0.3");
        RiskTypeEntity luggageRisk = createOptionalRisk("LUGGAGE_LOSS", "0.1");

        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(sportRisk));
        when(riskTypeRepository.findActiveByCode(eq("LUGGAGE_LOSS"), any(LocalDate.class)))
                .thenReturn(Optional.of(luggageRisk));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldHandleWhitespaceInRiskCode() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("  SPORT_ACTIVITIES  "));
        request.setAgreementDateFrom(LocalDate.now());

        RiskTypeEntity sportRisk = createOptionalRisk("SPORT_ACTIVITIES", "0.3");

        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(sportRisk));

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldStopAtFirstError() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("INVALID_RISK_1", "INVALID_RISK_2"));
        request.setAgreementDateFrom(LocalDate.now());

        when(riskTypeRepository.findActiveByCode(eq("INVALID_RISK_1"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertTrue(result.get().getMessage().contains("INVALID_RISK_1"));
    }

    @Test
    void shouldValidateMixOfValidAndInvalidRisks() {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "INVALID_RISK"));
        request.setAgreementDateFrom(LocalDate.now());

        RiskTypeEntity sportRisk = createOptionalRisk("SPORT_ACTIVITIES", "0.3");

        when(riskTypeRepository.findActiveByCode(eq("SPORT_ACTIVITIES"), any(LocalDate.class)))
                .thenReturn(Optional.of(sportRisk));
        when(riskTypeRepository.findActiveByCode(eq("INVALID_RISK"), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        Optional<ValidationError> result = validation.validate(request);

        assertTrue(result.isPresent());
        assertTrue(result.get().getMessage().contains("INVALID_RISK"));
    }

    private RiskTypeEntity createOptionalRisk(String code, String coefficient) {
        RiskTypeEntity risk = new RiskTypeEntity();
        risk.setCode(code);
        risk.setNameEn(code.replace("_", " "));
        risk.setCoefficient(new BigDecimal(coefficient));
        risk.setIsMandatory(false);
        risk.setValidFrom(LocalDate.now().minusYears(1));
        return risk;
    }
}
