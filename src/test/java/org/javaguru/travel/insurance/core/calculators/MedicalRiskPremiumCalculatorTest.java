package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Улучшенные тесты для MedicalRiskPremiumCalculator
 *
 * ПРИНЦИПЫ:
 * 1. Каждый тест настраивает ТОЛЬКО те моки, которые ему нужны
 * 2. Явные when() в каждом тесте - видно что именно используется
 * 3. verify() для проверки взаимодействий
 * 4. Группировка тестов через @Nested для читаемости
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRiskPremiumCalculatorTest extends BaseTestFixture {

    @Mock AgeCalculator ageCalculator;
    @Mock MedicalRiskLimitLevelRepository medicalLevelRepository;
    @Mock CountryRepository countryRepository;
    @Mock RiskTypeRepository riskTypeRepository;
    @Mock TripDurationPricingService durationPricingService;
    @Mock RiskBundleService riskBundleService;
    @Mock AgeRiskPricingService ageRiskPricingService;

    @InjectMocks
    MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void defaultMocks() {
        // age
        when(ageCalculator.calculateAgeAndCoefficient(any(), any()))
                .thenReturn(ageResult35Years());

        // country
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.of(spainLowRisk()));

        // medical level
        when(medicalLevelRepository.findActiveByCode(anyString(), any()))
                .thenReturn(Optional.of(medicalLevel50k()));

        // mandatory risk
        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                .thenReturn(Optional.of(travelMedicalMandatoryRisk()));

        // pricing defaults
        when(durationPricingService.getDurationCoefficient(anyInt(), any()))
                .thenReturn(BigDecimal.ONE);

        when(ageRiskPricingService.getAgeRiskModifier(anyString(), anyInt(), any()))
                .thenReturn(BigDecimal.ONE);

        when(riskBundleService.getBestApplicableBundle(anyList(), any()))
                .thenReturn(Optional.empty());
    }

    // =====================================================
    // CORE FORMULA
    // =====================================================

    @Test
    void shouldCalculatePremiumForStandardAdultTrip() {
        var request = standardAdultRequest();

        BigDecimal premium = calculator.calculatePremium(request);

        assertThat(premium)
                .isNotNull()
                .isPositive()
                .hasScaleOf(2);
    }

    @Test
    void shouldApplyDurationCoefficient() {
        when(durationPricingService.getDurationCoefficient(eq(14), any()))
                .thenReturn(new BigDecimal("1.2"));

        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.durationCoefficient())
                .isEqualByComparingTo("1.2");
    }

    @Test
    void shouldApplyCountryCoefficient() {
        when(countryRepository.findActiveByIsoCode(eq("ES"), any()))
                .thenReturn(Optional.of(spainLowRisk()));

        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.countryCoefficient())
                .isEqualByComparingTo(spainLowRisk().getRiskCoefficient());
    }

    // =====================================================
    // ADDITIONAL RISKS
    // =====================================================

    @Test
    void shouldIncludeOptionalRiskCoefficient() {
        var request = requestWithSelectedRisks("TRAVEL_BAGGAGE");

        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_BAGGAGE"), any()))
                .thenReturn(Optional.of(travelBaggageOptionalRisk()));

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.additionalRisksCoefficient())
                .isEqualByComparingTo(travelBaggageOptionalRisk().getCoefficient());
    }

    @Test
    void shouldApplyAgeModifierToRisk() {
        when(ageRiskPricingService.getAgeRiskModifier(eq("TRAVEL_BAGGAGE"), eq(35), any()))
                .thenReturn(new BigDecimal("1.5"));

        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_BAGGAGE"), any()))
                .thenReturn(Optional.of(travelBaggageOptionalRisk()));

        var result = calculator.calculatePremiumWithDetails(
                requestWithSelectedRisks("TRAVEL_BAGGAGE")
        );

        assertThat(result.additionalRisksCoefficient())
                .isEqualByComparingTo(
                        travelBaggageOptionalRisk()
                                .getCoefficient()
                                .multiply(new BigDecimal("1.5"))
                );
    }

    // =====================================================
    // NEGATIVE SCENARIOS
    // =====================================================

    @Test
    void shouldFailWhenCountryNotFound() {
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.calculatePremium(standardAdultRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country");
    }

    @Test
    void shouldFailWhenMedicalLevelNotFound() {
        when(medicalLevelRepository.findActiveByCode(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.calculatePremium(standardAdultRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Medical");
    }
}
