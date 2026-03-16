package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.core.services.PayoutLimitService;
import org.javaguru.travel.insurance.core.services.PayoutLimitService.PayoutLimitResult;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
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

/**
 * РЕФАКТОРИНГ (п. 4.3): Проверки обновлены для работы
 * с вложенными records PremiumCalculationResult.
 */
@ExtendWith(MockitoExtension.class)
class MedicalLevelPremiumStrategyTest {

    private static final LocalDate DATE_FROM  = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO    = LocalDate.of(2025, 6, 15);
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

    private static final BigDecimal COVERAGE       = new BigDecimal("10000.00");
    private static final BigDecimal DAILY_RATE     = new BigDecimal("2.00");
    private static final BigDecimal DURATION_COEFF = new BigDecimal("0.95");
    private static final BigDecimal COUNTRY_COEFF  = new BigDecimal("1.0");

    @Mock private ReferenceDataPort referenceDataPort;
    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private PersonAgeCalculator personAgeCalculator;
    @Mock private TripDurationCalculator tripDurationCalculator;
    @Mock private AdditionalRisksCalculator additionalRisksCalculator;
    @Mock private BundleDiscountCalculator bundleDiscountCalculator;
    @Mock private RiskDetailsBuilder riskDetailsBuilder;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;
    @Mock private PayoutLimitService payoutLimitService;

    @InjectMocks
    private MedicalLevelPremiumStrategy strategy;

    @Test
    void calculate_returnsResultWithCorrectMode() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("MEDICAL_LEVEL");
    }

    @Test
    void calculate_returnsCorrectAgeViaNestedRecord() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageDetails().age()).isEqualTo(35);
    }

    @Test
    void calculate_coverageAmountInTripDetails() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.tripDetails().coverageAmount()).isEqualByComparingTo(COVERAGE);
    }

    @Test
    void calculate_whenPayoutLimitApplied_flagIsTrueInPayoutLimitDetails() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, withPayoutLimit(new BigDecimal("8000")));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isTrue();
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isEqualByComparingTo("8000");
    }

    @Test
    void calculate_whenPayoutLimitNotApplied_flagIsFalseInPayoutLimitDetails() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isFalse();
    }

    @Test
    void calculate_countryDetailsContainCorrectCoefficient() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.countryDetails().countryCoefficient()).isEqualByComparingTo(COUNTRY_COEFF);
        assertThat(result.countryDetails().countryName()).isEqualTo("Spain");
    }

    @Test
    void calculate_tripDetailsContainDays() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.tripDetails().days()).isEqualTo(14);
        assertThat(result.tripDetails().durationCoefficient()).isEqualByComparingTo(DURATION_COEFF);
    }

    @Test
    void calculate_riskDetailsContainBundleDiscount() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.riskDetails().bundleDiscount().discountAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculate_delegatesToPersonAgeCalculator() {
        var request = buildRequest(true);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(personAgeCalculator).calculate(BIRTH_DATE, DATE_FROM, true);
    }

    @Test
    void calculate_delegatesToTripDurationCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(tripDurationCalculator).calculateDays(DATE_FROM, DATE_TO);
        verify(tripDurationCalculator).getDurationCoefficient(14L, DATE_FROM);
    }

    @Test
    void calculate_delegatesToAdditionalRisksCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(additionalRisksCalculator).calculate(
                eq(Collections.emptyList()), eq(35), eq(DATE_FROM));
    }

    @Test
    void calculate_delegatesToBundleDiscountCalculator() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(bundleDiscountCalculator).calculate(
                eq(Collections.emptyList()), any(BigDecimal.class), eq(DATE_FROM));
    }

    @Test
    void calculate_delegatesToRiskDetailsBuilder() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(riskDetailsBuilder).build(
                eq(Collections.emptyList()),
                eq(DAILY_RATE),
                eq(BigDecimal.ONE),
                eq(COUNTRY_COEFF),
                eq(DURATION_COEFF),
                eq(14),
                eq(35),
                eq(DATE_FROM));
    }

    @Test
    void calculate_whenAgeCoefficientDisabled_passesEnabledFalseToCalculator() {
        var request = buildRequest(false);
        setupMocks(false, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(personAgeCalculator).calculate(BIRTH_DATE, DATE_FROM, false);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private TravelCalculatePremiumRequest buildRequest(Boolean applyAgeCoefficient) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(BIRTH_DATE)
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_TO)
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .applyAgeCoefficient(applyAgeCoefficient)
                .selectedRisks(Collections.emptyList())
                .build();
    }

    private PayoutLimitResult noPayoutLimit() {
        return new PayoutLimitResult(new BigDecimal("26.60"), COVERAGE, false);
    }

    private PayoutLimitResult withPayoutLimit(BigDecimal appliedLimit) {
        return new PayoutLimitResult(new BigDecimal("21.00"), appliedLimit, true);
    }

    private void setupMocks(boolean ageCoefficientEnabled,
                            BigDecimal ageCoeff,
                            PayoutLimitResult payoutLimitResult) {

        MedicalRiskLimitLevel medicalLevel = mock(MedicalRiskLimitLevel.class);
        when(medicalLevel.getDailyRate()).thenReturn(DAILY_RATE);
        when(medicalLevel.getCoverageAmount()).thenReturn(COVERAGE);
        when(medicalLevel.getMaxPayoutAmount()).thenReturn(null);

        when(referenceDataPort.findMedicalLevel(eq("10000"), eq(DATE_FROM)))
                .thenReturn(Optional.of(medicalLevel));

        Country country = mock(Country.class);
        when(country.getNameEn()).thenReturn("Spain");

        var coefficient = mock(org.javaguru.travel.insurance.domain.model.valueobject.Coefficient.class);
        when(coefficient.value()).thenReturn(COUNTRY_COEFF);
        when(country.getRiskCoefficient()).thenReturn(coefficient);

        when(referenceDataPort.findCountry(any(CountryCode.class), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");
        when(personAgeCalculator.calculate(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);

        when(tripDurationCalculator.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);
        when(tripDurationCalculator.getDurationCoefficient(eq(14L), eq(DATE_FROM)))
                .thenReturn(DURATION_COEFF);

        when(additionalRisksCalculator.calculate(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new AdditionalRisksCalculator.AdditionalRisksResult(BigDecimal.ZERO, List.of()));

        when(bundleDiscountCalculator.calculate(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));

        when(riskDetailsBuilder.build(any(), any(), any(), any(), any(), anyInt(), eq(35), eq(DATE_FROM)))
                .thenReturn(List.of());

        when(payoutLimitService.applyPayoutLimit(any(), any(), any()))
                .thenReturn(payoutLimitResult);

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("ES"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        when(stepsBuilder.buildMedicalLevelSteps(
                any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }
}