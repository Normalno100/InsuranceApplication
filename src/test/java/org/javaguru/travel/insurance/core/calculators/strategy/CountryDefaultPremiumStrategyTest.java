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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * РЕФАКТОРИНГ (п. 4.3): Проверки обновлены для работы
 * с вложенными records PremiumCalculationResult.
 */
@ExtendWith(MockitoExtension.class)
class CountryDefaultPremiumStrategyTest {

    private static final LocalDate DATE_FROM  = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO    = LocalDate.of(2025, 6, 15);
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);
    private static final BigDecimal DEFAULT_PREMIUM = new BigDecimal("8.00");

    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private ReferenceDataPort referenceDataPort;
    @Mock private PersonAgeCalculator personAgeCalculator;
    @Mock private TripDurationCalculator tripDurationCalculator;
    @Mock private AdditionalRisksCalculator additionalRisksCalculator;
    @Mock private BundleDiscountCalculator bundleDiscountCalculator;
    @Mock private RiskDetailsBuilder riskDetailsBuilder;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;

    @InjectMocks
    private CountryDefaultPremiumStrategy strategy;

    @Test
    void calculate_returnsCountryDefaultMode() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("COUNTRY_DEFAULT");
    }

    @Test
    void calculate_returnsCorrectAgeViaNestedRecord() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageDetails().age()).isEqualTo(35);
        assertThat(result.ageDetails().ageGroupDescription()).isEqualTo("Adults");
    }

    @Test
    void calculate_countryDefaultMode_payoutLimitDetailsAreNull() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitDetails().medicalPayoutLimit()).isNull();
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isNull();
        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isFalse();
    }

    @Test
    void calculate_countryDetailsContainDefaultPremium() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.countryDetails().countryDefaultDayPremium())
                .isEqualByComparingTo(DEFAULT_PREMIUM);
        assertThat(result.countryDetails().countryDefaultCurrency()).isEqualTo("EUR");
        assertThat(result.countryDetails().countryName()).isEqualTo("Thailand");
    }

    @Test
    void calculate_tripDetailsNoCoverageAmount() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        // В COUNTRY_DEFAULT нет уровня покрытия
        assertThat(result.tripDetails().coverageAmount()).isNull();
        assertThat(result.tripDetails().days()).isEqualTo(14);
    }

    @Test
    void calculate_passesOneAsCountryCoefficientToRiskDetailsBuilder() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(riskDetailsBuilder).build(
                any(), eq(DEFAULT_PREMIUM), any(),
                eq(BigDecimal.ONE),
                any(), anyInt(), anyInt(), any());
    }

    @Test
    void calculate_whenApplyAgeCoefficientIsNull_resolvesFromDb() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(null, DATE_FROM);
    }

    @Test
    void calculate_delegatesToPersonAgeCalculator() {
        var request = buildRequest(true);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(personAgeCalculator).calculate(BIRTH_DATE, DATE_FROM, true);
    }

    @Test
    void calculate_delegatesToTripDurationCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(tripDurationCalculator).calculateDays(DATE_FROM, DATE_TO);
        verify(tripDurationCalculator).getDurationCoefficient(14L, DATE_FROM);
    }

    @Test
    void calculate_delegatesToAdditionalRisksCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(additionalRisksCalculator).calculate(
                eq(Collections.emptyList()), eq(35), eq(DATE_FROM));
    }

    @Test
    void calculate_delegatesToBundleDiscountCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE);

        strategy.calculate(request);

        verify(bundleDiscountCalculator).calculate(
                eq(Collections.emptyList()), any(BigDecimal.class), eq(DATE_FROM));
    }

    @Test
    void calculate_throwsException_whenDefaultPremiumNotFound() {
        var request = buildRequest(null);

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        assertThrows(IllegalStateException.class, () -> strategy.calculate(request));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

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

        var defaultPremiumResult = new CountryDefaultDayPremiumService.DefaultPremiumResult(
                "TH", DEFAULT_PREMIUM, "EUR", "Thailand base rate");

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.of(defaultPremiumResult));

        when(countryDefaultDayPremiumService.calculateBasePremium(any(), any(), any(), anyInt()))
                .thenReturn(new BigDecimal("106.40"));

        Country country = mock(Country.class);
        var coefficient = mock(org.javaguru.travel.insurance.domain.model.valueobject.Coefficient.class);
        when(coefficient.value()).thenReturn(new BigDecimal("1.3"));
        when(country.getRiskCoefficient()).thenReturn(coefficient);
        when(country.getNameEn()).thenReturn("Thailand");

        when(referenceDataPort.findCountry(any(CountryCode.class), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");
        when(personAgeCalculator.calculate(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);

        when(tripDurationCalculator.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);
        when(tripDurationCalculator.getDurationCoefficient(eq(14L), eq(DATE_FROM)))
                .thenReturn(new BigDecimal("0.95"));

        when(additionalRisksCalculator.calculate(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new AdditionalRisksCalculator.AdditionalRisksResult(BigDecimal.ZERO, List.of()));

        when(bundleDiscountCalculator.calculate(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));

        when(riskDetailsBuilder.build(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        when(stepsBuilder.buildCountryDefaultSteps(any(), any(), any(), any(),
                anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }
}