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
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;
    @Mock private PayoutLimitService payoutLimitService;

    @InjectMocks
    private MedicalLevelPremiumStrategy strategy;

    // =========================================================

    @Test
    void calculate_returnsResultWithCorrectMode() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("MEDICAL_LEVEL");
    }

    @Test
    void calculate_returnsCorrectAge() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.age()).isEqualTo(35);
    }

    @Test
    void calculate_whenPayoutLimitApplied_flagIsTrue() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, withPayoutLimit(new BigDecimal("8000")));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitApplied()).isTrue();
    }

    @Test
    void calculate_whenPayoutLimitNotApplied_flagIsFalse() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitApplied()).isFalse();
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
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .applyAgeCoefficient(applyAgeCoefficient)
                .selectedRisks(Collections.emptyList())
                .build();
    }

    private PayoutLimitResult noPayoutLimit() {
        return new PayoutLimitResult(
                new BigDecimal("26.60"),
                COVERAGE,
                false
        );
    }

    private PayoutLimitResult withPayoutLimit(BigDecimal appliedLimit) {
        return new PayoutLimitResult(
                new BigDecimal("21.00"),
                appliedLimit,
                true
        );
    }

    private void setupMocks(boolean ageCoefficientEnabled,
                            BigDecimal ageCoeff,
                            PayoutLimitResult payoutLimitResult) {

        // Domain entity MedicalRiskLimitLevel
        MedicalRiskLimitLevel medicalLevel = mock(MedicalRiskLimitLevel.class);
        when(medicalLevel.getDailyRate()).thenReturn(DAILY_RATE);
        when(medicalLevel.getCoverageAmount()).thenReturn(COVERAGE);
        when(medicalLevel.getMaxPayoutAmount()).thenReturn(null);

        when(referenceDataPort.findMedicalLevel(eq("10000"), eq(DATE_FROM)))
                .thenReturn(Optional.of(medicalLevel));

        // Domain entity Country
        Country country = mock(Country.class);
        when(country.getNameEn()).thenReturn("Spain");

        var coefficient = mock(org.javaguru.travel.insurance.domain.model.valueobject.Coefficient.class);
        when(coefficient.value()).thenReturn(COUNTRY_COEFF);

        when(country.getRiskCoefficient()).thenReturn(coefficient);

        when(referenceDataPort.findCountry(any(CountryCode.class), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        // Age config
        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        // Age
        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");
        when(shared.calculateAge(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);

        // Days
        when(shared.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);

        when(shared.getDurationCoefficient(eq(14L), eq(DATE_FROM)))
                .thenReturn(DURATION_COEFF);

        when(shared.calculateAdditionalRisks(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(BigDecimal.ZERO, List.of()));

        when(shared.calculateBundleDiscount(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));

        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), eq(35), eq(DATE_FROM)))
                .thenReturn(List.of());

        // payout limit
        when(payoutLimitService.applyPayoutLimit(any(), any(), any()))
                .thenReturn(payoutLimitResult);

        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("ES"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        when(stepsBuilder.buildMedicalLevelSteps(
                any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(),
                any(), any()))
                .thenReturn(List.of());
    }
}