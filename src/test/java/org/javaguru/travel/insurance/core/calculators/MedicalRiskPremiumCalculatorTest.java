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
 * Тесты MedicalRiskPremiumCalculator — фасада паттерна Strategy (task_115).
 *
 * Ответственность фасада: выбрать правильную стратегию и делегировать вызов.
 * Детальная логика расчёта формулы проверяется в:
 *   - MedicalLevelPremiumStrategyTest
 *   - CountryDefaultPremiumStrategyTest
 *
 * ВАЖНО: фасад зависит только от трёх бинов:
 *   MedicalLevelPremiumStrategy, CountryDefaultPremiumStrategy, CountryDefaultDayPremiumService.
 * Прямых зависимостей от репозиториев нет — они скрыты внутри стратегий.
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

        // дефолт: дефолтной ставки нет → всегда MEDICAL_LEVEL
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
        // hasDefaultDayPremium замокан как false в @BeforeEach -> fallback

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
    // ПРОВЕРКА ВЫЗОВА hasDefaultDayPremium
    // =====================================================

    @Test
    void shouldNotCallHasDefaultPremiumWhenFlagIsNull() {
        calculator.calculatePremiumWithDetails(standardAdultRequest());

        verify(countryDefaultDayPremiumService, never()).hasDefaultDayPremium(anyString(), any());
    }

    @Test
    void shouldNotCallHasDefaultPremiumWhenFlagIsFalse() {
        var request = standardAdultRequest();
        request.setUseCountryDefaultPremium(false);

        calculator.calculatePremiumWithDetails(request);

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
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // =====================================================

    /** Минимальный stub PremiumCalculationResult для тестов фасада */
    private MedicalRiskPremiumCalculator.PremiumCalculationResult stubResult(
            BigDecimal premium,
            MedicalRiskPremiumCalculator.CalculationMode mode) {

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("2.50"),
                35,
                new BigDecimal("1.10"),
                "Adults",
                new BigDecimal("1.20"),
                "Spain",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                new BigDecimal("1.32"),
                14,
                new BigDecimal("50000"),
                List.of(),
                new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO),
                List.of(),
                mode,
                mode == MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT
                        ? new BigDecimal("2.50") : null,
                null,
                "EUR"
        );
    }
}