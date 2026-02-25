package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты MedicalLevelPremiumStrategy — расчёт премии в режиме MEDICAL_LEVEL.
 *
 * ФОРМУЛА:
 *   ПРЕМИЯ = DailyRate × AgeCoeff × CountryCoeff × DurationCoeff
 *            × (1 + Σ riskCoeffs) × Days − BundleDiscount
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicalLevelPremiumStrategyTest extends BaseTestFixture {

    @Mock private MedicalRiskLimitLevelRepository medicalLevelRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;

    @InjectMocks
    private MedicalLevelPremiumStrategy strategy;

    @BeforeEach
    void setUp() {
        when(medicalLevelRepository.findActiveByCode(anyString(), any()))
                .thenReturn(Optional.of(medicalLevel50k()));

        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.of(spainLowRisk()));

        when(shared.calculateAge(any(), any()))
                .thenReturn(ageResult35Years());

        when(shared.calculateDays(any(), any()))
                .thenReturn(14L);

        when(shared.getDurationCoefficient(anyLong(), any()))
                .thenReturn(BigDecimal.ONE);

        when(shared.calculateAdditionalRisks(any(), anyInt(), any()))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(
                        BigDecimal.ZERO, List.of()));

        when(shared.calculateBundleDiscount(any(), any(), any()))
                .thenReturn(new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));

        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        when(stepsBuilder.buildMedicalLevelSteps(any(), any(), any(), any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());

        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.empty());
    }

    // =====================================================
    // РЕЖИМ РАСЧЁТА
    // =====================================================

    @Test
    void shouldReturnMedicalLevelMode() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL);
    }

    // =====================================================
    // ФОРМУЛА
    // =====================================================

    @Test
    void shouldCalculatePositivePremium() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.premium())
                .isNotNull()
                .isPositive()
                .hasScaleOf(2);
    }

    @Test
    void shouldUseDailyRateAsBaseRate() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.baseRate())
                .isEqualByComparingTo(medicalLevel50k().getDailyRate());
    }

    @Test
    void shouldApplyCountryCoefficient() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.countryCoefficient())
                .isEqualByComparingTo(spainLowRisk().getRiskCoefficient());
    }

    @Test
    void shouldApplyDurationCoefficient() {
        when(shared.getDurationCoefficient(anyLong(), any()))
                .thenReturn(new BigDecimal("1.2"));

        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.durationCoefficient()).isEqualByComparingTo("1.2");
    }

    @Test
    void shouldApplyAgeCoefficient() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.ageCoefficient())
                .isEqualByComparingTo(ageResult35Years().coefficient());
        assertThat(result.age()).isEqualTo(35);
    }

    @Test
    void shouldCalculatePremiumUsingFullFormula() {
        // DailyRate × AgeCoeff × CountryCoeff × DurationCoeff × Days
        BigDecimal dailyRate    = medicalLevel50k().getDailyRate();
        BigDecimal ageCoeff     = ageResult35Years().coefficient();
        BigDecimal countryCoeff = spainLowRisk().getRiskCoefficient();
        BigDecimal durationCoeff = BigDecimal.ONE;
        int days = 14;

        BigDecimal expected = dailyRate
                .multiply(ageCoeff)
                .multiply(countryCoeff)
                .multiply(durationCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.premium()).isEqualByComparingTo(expected);
    }

    // =====================================================
    // ДОПОЛНИТЕЛЬНЫЕ РИСКИ
    // =====================================================

    @Test
    void shouldApplyAdditionalRisksCoefficient() {
        BigDecimal baggageCoeff = new BigDecimal("0.05");
        when(shared.calculateAdditionalRisks(any(), anyInt(), any()))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(
                        baggageCoeff, List.of()));

        var result = strategy.calculate(requestWithSelectedRisks("TRAVEL_BAGGAGE"));

        assertThat(result.additionalRisksCoefficient()).isEqualByComparingTo(baggageCoeff);
    }

    @Test
    void shouldIncludeAdditionalRiskCoefficientInTotalCoefficient() {
        BigDecimal baggageCoeff = new BigDecimal("0.05");
        when(shared.calculateAdditionalRisks(any(), anyInt(), any()))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(
                        baggageCoeff, List.of()));

        var result = strategy.calculate(requestWithSelectedRisks("TRAVEL_BAGGAGE"));

        // totalCoeff должен включать (1 + 0.05)
        assertThat(result.totalCoefficient())
                .isEqualByComparingTo(
                        ageResult35Years().coefficient()
                                .multiply(spainLowRisk().getRiskCoefficient())
                                .multiply(BigDecimal.ONE)
                                .multiply(BigDecimal.ONE.add(baggageCoeff))
                );
    }

    // =====================================================
    // СКИДКА
    // =====================================================

    @Test
    void shouldApplyBundleDiscount() {
        BigDecimal discount = new BigDecimal("5.00");
        when(shared.calculateBundleDiscount(any(), any(), any()))
                .thenReturn(new MedicalRiskPremiumCalculator.BundleDiscountResult(null, discount));

        BigDecimal premiumWithoutDiscount = strategy.calculate(standardAdultRequest()).premium()
                .add(discount);
        BigDecimal premiumWithDiscount = strategy.calculate(standardAdultRequest()).premium();

        // premiumWithDiscount должен быть на discount меньше
        assertThat(premiumWithoutDiscount.subtract(premiumWithDiscount))
                .isEqualByComparingTo(discount);
    }

    @Test
    void shouldReturnZeroDiscountWhenNoBundleApplies() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.bundleDiscount().discountAmount())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // =====================================================
    // ПРОЧИЕ ПОЛЯ РЕЗУЛЬТАТА
    // =====================================================

    @Test
    void shouldPopulateDaysField() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.days()).isEqualTo(14);
    }

    @Test
    void shouldPopulateCoverageAmount() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.coverageAmount())
                .isEqualByComparingTo(medicalLevel50k().getCoverageAmount());
    }

    @Test
    void shouldPopulateCountryName() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.countryName()).isEqualTo(spainLowRisk().getNameEn());
    }

    @Test
    void shouldReturnNullCountryDefaultFieldsWhenNoDefaultPremiumExists() {
        // countryDefaultDayPremiumService возвращает empty → поля null
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.countryDefaultDayPremium()).isNull();
        assertThat(result.countryDefaultDayPremiumForInfo()).isNull();
        assertThat(result.countryDefaultCurrency()).isNull();
    }

    @Test
    void shouldPopulateCountryDefaultFieldsForInfoWhenPremiumExists() {
        BigDecimal defaultRate = new BigDecimal("2.50");
        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.of(new CountryDefaultDayPremiumService.DefaultPremiumResult(
                        "ES", defaultRate, "EUR", "Spain default rate")));

        var result = strategy.calculate(standardAdultRequest());

        // countryDefaultDayPremium в MEDICAL_LEVEL = null (не используется в формуле)
        assertThat(result.countryDefaultDayPremium()).isNull();
        // countryDefaultDayPremiumForInfo = defaultDayPremium (только для отображения)
        assertThat(result.countryDefaultDayPremiumForInfo()).isEqualByComparingTo(defaultRate);
        assertThat(result.countryDefaultCurrency()).isEqualTo("EUR");
    }

    @Test
    void shouldDelegateStepsToBuilder() {
        strategy.calculate(standardAdultRequest());

        verify(stepsBuilder).buildMedicalLevelSteps(
                any(), any(), any(), any(), any(), anyLong(), any(), any(), any());
    }

    // =====================================================
    // NEGATIVE SCENARIOS
    // =====================================================

    @Test
    void shouldFailWhenMedicalLevelNotFound() {
        when(medicalLevelRepository.findActiveByCode(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.calculate(standardAdultRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Medical level not found");
    }

    @Test
    void shouldFailWhenCountryNotFound() {
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.calculate(standardAdultRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country not found");
    }
}