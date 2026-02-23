package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.javaguru.travel.insurance.core.services.TripDurationPricingService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    @Mock CountryDefaultDayPremiumService countryDefaultDayPremiumService;

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

        // useCountryDefaultPremium не установлен → MEDICAL_LEVEL режим всегда
        // findDefaultDayPremium вызывается для информации в MEDICAL_LEVEL — возвращаем empty
        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.empty());

        // hasDefaultDayPremium — false, чтобы shouldUseCountryDefaultMode() → false
        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(false);
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

    // =====================================================
    // CALCULATION MODE
    // =====================================================

    @Test
    void shouldUseMedicalLevelModeByDefault() {
        var request = standardAdultRequest(); // useCountryDefaultPremium = null

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
    }

    @Test
    void shouldFallbackToMedicalLevelWhenNoDefaultPremiumExists() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);

        // Нет записи в country_default_day_premiums → fallback
        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(false);

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
    }

    @Test
    void shouldUseCountryDefaultModeWhenFlagSetAndPremiumExists() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);

        BigDecimal defaultRate = new BigDecimal("2.50");

        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(true);
        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.of(new CountryDefaultDayPremiumService.DefaultPremiumResult(
                        "ES", defaultRate, "EUR", "Spain default rate")));
        when(countryDefaultDayPremiumService.calculateBasePremium(
                any(), any(), any(), anyInt()))
                .thenReturn(new BigDecimal("35.00"));

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT);
        assertThat(result.countryDefaultDayPremium())
                .isEqualByComparingTo(defaultRate);
    }
}