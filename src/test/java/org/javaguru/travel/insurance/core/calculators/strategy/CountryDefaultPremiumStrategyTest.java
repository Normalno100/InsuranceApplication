package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CountryDefaultPremiumStrategyTest {

    private static final LocalDate DATE_FROM  = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO    = LocalDate.of(2025, 6, 15);
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private ReferenceDataPort referenceDataPort;
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;

    @InjectMocks
    private CountryDefaultPremiumStrategy strategy;

    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsNull_resolvesFromDb() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(null, DATE_FROM);
    }

    @Test
    void calculate_returnsCountryDefaultMode() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("COUNTRY_DEFAULT");
    }

    @Test
    void calculate_returnsCorrectAge() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.age()).isEqualTo(35);
    }

    @Test
    void calculate_doesNotPassCountryCoefficientToFormula() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(shared).buildRiskDetails(any(), any(), any(),
                eq(BigDecimal.ONE),
                any(), anyInt(), anyInt(), any());
    }

    @Test
    void calculate_throwsException_whenDefaultPremiumNotFound() {
        var request = buildRequest(null);

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> strategy.calculate(request));
    }

    // =========================================================
    // helpers
    // =========================================================

    private TravelCalculatePremiumRequest buildRequest(Boolean applyAgeCoefficient) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(BIRTH_DATE)
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_TO)
                .countryIsoCode("TH")
                .useCountryDefaultPremium(true)
                .applyAgeCoefficient(applyAgeCoefficient)
                .selectedRisks(Collections.emptyList())
                .build();
    }

    private void setupMocks(boolean ageCoefficientEnabled, BigDecimal ageCoeff) {

        // default premium
        var defaultPremium = new CountryDefaultDayPremiumService.DefaultPremiumResult(
                "FR", new BigDecimal("8.00"), "EUR", "a");

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.of(defaultPremium));

        when(countryDefaultDayPremiumService.calculateBasePremium(
                any(), any(), any(), anyInt()))
                .thenReturn(new BigDecimal("106.40"));

        // Country (domain entity)
        Country country = mock(Country.class);

        var coefficient = mock(org.javaguru.travel.insurance.domain.model.valueobject.Coefficient.class);
        when(coefficient.value()).thenReturn(new BigDecimal("1.3"));

        when(country.getRiskCoefficient()).thenReturn(coefficient);
        when(country.getNameEn()).thenReturn("Thailand");

        when(referenceDataPort.findCountry(any(CountryCode.class), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        // Age config
        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        // Age
        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");

        when(shared.calculateAge(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);

        when(shared.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);

        when(shared.getDurationCoefficient(eq(14L), eq(DATE_FROM)))
                .thenReturn(new BigDecimal("0.95"));

        when(shared.calculateAdditionalRisks(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(BigDecimal.ZERO, List.of()));

        when(shared.calculateBundleDiscount(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));

        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        when(stepsBuilder.buildCountryDefaultSteps(any(), any(), any(), any(),
                anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }
}