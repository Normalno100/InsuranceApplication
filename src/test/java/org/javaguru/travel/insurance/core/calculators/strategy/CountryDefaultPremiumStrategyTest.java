package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
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
    private static final LocalDate DATE_TO    = LocalDate.of(2025, 6, 15); // 14 дней
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);  // 35 лет

    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private CountryRepository countryRepository;
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;

    @InjectMocks
    private CountryDefaultPremiumStrategy strategy;

    // =========================================================
    // applyAgeCoefficient = null → читает из DB
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsNull_resolvesFromDb() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(null, DATE_FROM);
    }

    @Test
    void calculate_whenDbEnablesCoefficient_passesEnabledTrueToShared() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, true);
    }

    @Test
    void calculate_whenDbDisablesCoefficient_passesEnabledFalseToShared() {
        var request = buildRequest(null);
        setupMocks(false, BigDecimal.ONE);

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    // =========================================================
    // applyAgeCoefficient = true → принудительно включает
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsTrue_resolvesToTrue() {
        var request = buildRequest(true);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(true, DATE_FROM);
        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, true);
    }

    // =========================================================
    // applyAgeCoefficient = false → принудительно отключает
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsFalse_resolvesToFalse() {
        var request = buildRequest(false);
        setupMocks(false, BigDecimal.ONE);

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(false, DATE_FROM);
        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    @Test
    void calculate_whenAgeCoefficientDisabled_ageCoefficientInResultIsOne() {
        var request = buildRequest(false);
        setupMocks(false, BigDecimal.ONE);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void calculate_whenAgeCoefficientEnabled_ageCoefficientInResultMatchesAge() {
        var request = buildRequest(true);
        BigDecimal ageCoeff = new BigDecimal("1.30");
        setupMocks(true, ageCoeff);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(ageCoeff);
    }

    // =========================================================
    // Корректность расчёта
    // =========================================================

    @Test
    void calculate_returnsCountryDefaultMode() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.00"));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("COUNTRY_DEFAULT");
    }

    @Test
    void calculate_returnsCorrectAge() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.00"));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.age()).isEqualTo(35);
    }

    @Test
    void calculate_doesNotPassCountryCoefficientToFormula() {
        // В COUNTRY_DEFAULT countryCoefficient не участвует в basePremium
        // (передаётся BigDecimal.ONE в buildRiskDetails)
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.00"));

        strategy.calculate(request);

        verify(shared).buildRiskDetails(any(), any(), any(),
                eq(BigDecimal.ONE), // countryCoeff == ONE
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
        // CountryDefaultDayPremiumService
        var defaultPremium = new CountryDefaultDayPremiumService.DefaultPremiumResult(
                "FR", new BigDecimal("8.00"), "EUR", "a");
        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.of(defaultPremium));
        when(countryDefaultDayPremiumService.calculateBasePremium(
                any(), any(), any(), anyInt()))
                .thenReturn(new BigDecimal("106.40")); // 8.00 * 1.00 * 0.95 * 14

        // Country
        var country = mock(CountryEntity.class);
        when(country.getRiskCoefficient()).thenReturn(new BigDecimal("1.3"));
        when(country.getNameEn()).thenReturn("Thailand");
        when(countryRepository.findActiveByIsoCode(eq("TH"), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        // CalculationConfigService
        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        // SharedCalculationComponents
        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");
        when(shared.calculateAge(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);
        when(shared.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);
        when(shared.getDurationCoefficient(eq(14L), eq(DATE_FROM))).thenReturn(new BigDecimal("0.95"));
        when(shared.calculateAdditionalRisks(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(BigDecimal.ZERO, List.of()));
        when(shared.calculateBundleDiscount(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));
        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        // CalculationStepsBuilder
        when(stepsBuilder.buildCountryDefaultSteps(any(), any(), any(), any(),
                anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }
}
