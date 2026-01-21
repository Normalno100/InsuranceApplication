package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.MockSetupHelper;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRiskPremiumCalculatorTest extends MockSetupHelper {

    @Mock
    private AgeCalculator ageCalculator;
    @Mock
    private MedicalRiskLimitLevelRepository medicalLevelRepository;
    @Mock
    private CountryRepository countryRepository;
    @Mock
    private RiskTypeRepository riskTypeRepository;

    @Mock
    private TripDurationPricingService durationPricingService;
    @Mock
    private RiskBundleService riskBundleService;
    @Mock
    private AgeRiskPricingService ageRiskPricingService;

    @InjectMocks
    private MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void setup() {
        setupDefaultMocksLenient(
                ageCalculator,
                countryRepository,
                medicalLevelRepository,
                riskTypeRepository
        );

        lenient().when(durationPricingService.getDurationCoefficient(anyInt(), any()))
                .thenReturn(BigDecimal.ONE);

        lenient().when(ageRiskPricingService.getAgeRiskModifier(any(), anyInt(), any()))
                .thenReturn(BigDecimal.ONE);

        lenient().when(riskBundleService.getBestApplicableBundle(any(), any()))
                .thenReturn(Optional.empty());
    }

    // ==========================================
    // POSITIVE CASES
    // ==========================================

    @Test
    void shouldCalculatePremiumSuccessfully_withoutAdditionalRisks() {
        TravelCalculatePremiumRequest request = validRequest();

        BigDecimal premium = calculator.calculatePremium(request);

        assertNotNull(premium);
        assertTrue(premium.compareTo(BigDecimal.ZERO) > 0);
        assertEquals(2, premium.scale());
    }

    @Test
    void shouldReturnDetailedCalculationResult() {
        TravelCalculatePremiumRequest request = validRequest();

        var result = calculator.calculatePremiumWithDetails(request);

        assertNotNull(result);
        assertNotNull(result.premium());
        assertEquals("Spain", result.countryName());
        assertEquals(35, result.age());
        assertEquals(14, result.days());
        assertNotNull(result.calculationSteps());
    }

    @Test
    void shouldIncludeAdditionalRisk_whenOptionalRiskSelected() {
        RiskTypeEntity baggageRisk =
                optionalRisk("TRAVEL_BAGGAGE", new BigDecimal("0.1"));

        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_BAGGAGE"), any()))
                .thenReturn(Optional.of(baggageRisk));

        TravelCalculatePremiumRequest request =
                requestWithRisks("TRAVEL_BAGGAGE");

        var result = calculator.calculatePremiumWithDetails(request);

        assertTrue(
                result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0
        );

        assertEquals(2, result.riskDetails().size()); // MEDICAL + BAGGAGE
    }

    // ==========================================
    // NEGATIVE CASES
    // ==========================================

    @Test
    void shouldThrowException_whenCountryNotFound() {
        mockCountryNotFound(countryRepository, "ES");

        TravelCalculatePremiumRequest request = validRequest();

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculatePremium(request));
    }

    @Test
    void shouldThrowException_whenMedicalLevelNotFound() {
        mockMedicalLevelNotFound(medicalLevelRepository, "50000");

        TravelCalculatePremiumRequest request = validRequest();

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculatePremium(request));
    }
}

