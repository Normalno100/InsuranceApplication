package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.core.calculators.strategy.CountryDefaultPremiumStrategy;
import org.javaguru.travel.insurance.core.calculators.strategy.MedicalLevelPremiumStrategy;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты MedicalRiskPremiumCalculator — фасада паттерна Strategy.
 *
 * РЕФАКТОРИНГ (п. 4.3): Обновлены stub-методы для создания
 * PremiumCalculationResult через вложенные records.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalRiskPremiumCalculatorTest extends BaseTestFixture {

    @Mock private MedicalLevelPremiumStrategy medicalLevelStrategy;
    @Mock private CountryDefaultPremiumStrategy countryDefaultStrategy;
    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;

    @InjectMocks
    private MedicalRiskPremiumCalculator calculator;

    private MedicalRiskPremiumCalculator.PremiumCalculationResult medicalLevelResult;
    private MedicalRiskPremiumCalculator.PremiumCalculationResult countryDefaultResult;

    @BeforeEach
    void setUp() {
        medicalLevelResult   = stubResult(new BigDecimal("52.50"),
                MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
        countryDefaultResult = stubResult(new BigDecimal("35.00"),
                MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT);

        when(medicalLevelStrategy.calculate(any())).thenReturn(medicalLevelResult);
        when(countryDefaultStrategy.calculate(any())).thenReturn(countryDefaultResult);

        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(false);
    }

    // =====================================================
    // ВЫБОР СТРАТЕГИИ
    // =====================================================

    @Test
    void shouldUseMedicalLevelStrategyByDefault() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        verify(medicalLevelStrategy).calculate(any());
        verify(countryDefaultStrategy, never()).calculate(any());
        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
    }

    @Test
    void shouldUseMedicalLevelStrategyWhenFlagExplicitlyFalse() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(false);

        calculator.calculatePremiumWithDetails(request);

        verify(medicalLevelStrategy).calculate(any());
        verify(countryDefaultStrategy, never()).calculate(any());
    }

    @Test
    void shouldUseCountryDefaultStrategyWhenFlagTrueAndPremiumExists() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);
        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(true);

        var result = calculator.calculatePremiumWithDetails(request);

        verify(countryDefaultStrategy).calculate(any());
        verify(medicalLevelStrategy, never()).calculate(any());
        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT);
    }

    @Test
    void shouldFallbackToMedicalLevelWhenNoDefaultPremiumExists() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);

        var result = calculator.calculatePremiumWithDetails(request);

        verify(medicalLevelStrategy).calculate(any());
        verify(countryDefaultStrategy, never()).calculate(any());
        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
    }

    // =====================================================
    // ВОЗВРАТ ЗНАЧЕНИЯ ИЗ СТРАТЕГИИ
    // =====================================================

    @Test
    void calculatePremiumShouldReturnPremiumFromMedicalLevelStrategy() {
        BigDecimal premium = calculator.calculatePremium(standardAdultRequest());

        assertThat(premium).isEqualByComparingTo("52.50");
    }

    @Test
    void calculatePremiumShouldReturnPremiumFromCountryDefaultStrategy() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);
        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(true);

        BigDecimal premium = calculator.calculatePremium(request);

        assertThat(premium).isEqualByComparingTo("35.00");
    }

    // =====================================================
    // ПРОВЕРКА ВЛОЖЕННЫХ RECORDS (п. 4.3)
    // =====================================================

    @Test
    void shouldAccessAgeDetailsViaNestedRecord() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.ageDetails().age()).isEqualTo(35);
        assertThat(result.ageDetails().ageCoefficient()).isEqualByComparingTo("1.10");
        assertThat(result.ageDetails().ageGroupDescription()).isEqualTo("Adults");
    }

    @Test
    void shouldAccessCountryDetailsViaNestedRecord() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.countryDetails().countryName()).isEqualTo("Spain");
        assertThat(result.countryDetails().countryCoefficient()).isEqualByComparingTo("1.20");
    }

    @Test
    void shouldAccessTripDetailsViaNestedRecord() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.tripDetails().days()).isEqualTo(14);
        assertThat(result.tripDetails().coverageAmount()).isEqualByComparingTo("50000");
    }

    @Test
    void shouldAccessPayoutLimitDetailsViaNestedRecord() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.payoutLimitDetails().medicalPayoutLimit()).isEqualByComparingTo("50000");
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isEqualByComparingTo("40000");
        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isTrue();
    }

    @Test
    void shouldAccessRiskDetailsViaNestedRecord() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.riskDetails().riskPremiumDetails()).isEmpty();
        assertThat(result.riskDetails().bundleDiscount().discountAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =====================================================
    // task_117: ЛИМИТ ВЫПЛАТ
    // =====================================================

    @Test
    void medicalLevelResultShouldContainPayoutLimitFields() {
        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.payoutLimitDetails().medicalPayoutLimit()).isEqualByComparingTo("50000");
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isEqualByComparingTo("40000");
        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isTrue();
    }

    @Test
    void countryDefaultResultShouldHaveNullPayoutLimitFields() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);
        when(countryDefaultDayPremiumService.hasDefaultDayPremium(anyString(), any()))
                .thenReturn(true);

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.payoutLimitDetails().medicalPayoutLimit()).isNull();
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isNull();
        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isFalse();
    }

    @Test
    void medicalLevelResultWithNoPayoutLimitAppliedShouldHaveFalseFlag() {
        var noLimitResult = stubResultNoPayoutLimit(new BigDecimal("52.50"),
                MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
        when(medicalLevelStrategy.calculate(any())).thenReturn(noLimitResult);

        var result = calculator.calculatePremiumWithDetails(standardAdultRequest());

        assertThat(result.payoutLimitDetails().payoutLimitApplied()).isFalse();
        assertThat(result.payoutLimitDetails().appliedPayoutLimit()).isEqualByComparingTo("50000");
    }

    // =====================================================
    // ПРОВЕРКА ВЫЗОВА hasDefaultDayPremium
    // =====================================================

    @Test
    void shouldNotCallHasDefaultPremiumWhenFlagIsNull() {
        calculator.calculatePremiumWithDetails(standardAdultRequest());

        verify(countryDefaultDayPremiumService, never()).hasDefaultDayPremium(anyString(), any());
    }

    @Test
    void shouldCallHasDefaultPremiumWithCorrectArgs() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(true);

        calculator.calculatePremiumWithDetails(request);

        verify(countryDefaultDayPremiumService)
                .hasDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom());
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================

    /**
     * Создаёт stub PremiumCalculationResult через новые вложенные records.
     */
    private MedicalRiskPremiumCalculator.PremiumCalculationResult stubResult(
            BigDecimal premium,
            MedicalRiskPremiumCalculator.CalculationMode mode) {

        boolean isCountryDefault = mode == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT;

        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(
                35, new BigDecimal("1.10"), "Adults");

        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Spain",
                new BigDecimal("1.20"),
                isCountryDefault ? new BigDecimal("2.50") : null,
                null,
                "EUR");

        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                new BigDecimal("1.32"),
                isCountryDefault ? null : new BigDecimal("50000"));

        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(
                List.of(),
                new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));

        var payoutLimitDetails = isCountryDefault
                ? new MedicalRiskPremiumCalculator.PayoutLimitDetails(null, null, false)
                : new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                new BigDecimal("50000"),
                new BigDecimal("40000"),
                true);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("2.50"),
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                mode,
                List.of(),
                payoutLimitDetails);
    }

    /**
     * Stub для MEDICAL_LEVEL без применения лимита выплат.
     */
    private MedicalRiskPremiumCalculator.PremiumCalculationResult stubResultNoPayoutLimit(
            BigDecimal premium,
            MedicalRiskPremiumCalculator.CalculationMode mode) {

        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(
                35, new BigDecimal("1.10"), "Adults");

        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Spain", new BigDecimal("1.20"), null, null, "EUR");

        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14, BigDecimal.ONE, BigDecimal.ZERO, new BigDecimal("1.32"), new BigDecimal("50000"));

        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(
                List.of(),
                new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));

        var payoutLimitDetails = new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                new BigDecimal("50000"),
                new BigDecimal("50000"),  // appliedPayoutLimit == coverageAmount
                false);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("2.50"),
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                mode,
                List.of(),
                payoutLimitDetails);
    }
}