package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
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
 * Тесты CountryDefaultPremiumStrategy — расчёт премии в режиме COUNTRY_DEFAULT.
 *
 * ФОРМУЛА:
 *   ПРЕМИЯ = DefaultDayPremium × AgeCoeff × DurationCoeff
 *            × (1 + Σ riskCoeffs) × Days − BundleDiscount
 *
 * CountryCoeff НЕ применяется — уже "запечён" в DefaultDayPremium.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CountryDefaultPremiumStrategyTest extends BaseTestFixture {

    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private CountryRepository countryRepository;
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;

    @InjectMocks
    private CountryDefaultPremiumStrategy strategy;

    private static final BigDecimal DEFAULT_DAY_PREMIUM = new BigDecimal("2.50");
    private static final BigDecimal AGE_COEFF           = new BigDecimal("1.10");
    private static final BigDecimal DURATION_COEFF      = BigDecimal.ONE;
    private static final int        DAYS                = 14;

    @BeforeEach
    void setUp() {
        // дефолтная ставка страны
        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.of(defaultPremiumResult()));

        // страна
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.of(spainLowRisk()));

        // возраст
        when(shared.calculateAge(any(), any()))
                .thenReturn(ageResult35Years());

        // дни
        when(shared.calculateDays(any(), any())).thenReturn((long) DAYS);
        when(shared.getDurationCoefficient(anyLong(), any())).thenReturn(DURATION_COEFF);

        // доп. риски — нет
        when(shared.calculateAdditionalRisks(any(), anyInt(), any()))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(
                        BigDecimal.ZERO, List.of()));

        // базовая премия (DefaultDayPremium × AgeCoeff × DurationCoeff × Days)
        when(countryDefaultDayPremiumService.calculateBasePremium(any(), any(), any(), anyInt()))
                .thenReturn(new BigDecimal("38.50")); // 2.50 × 1.10 × 1.0 × 14

        // скидка — нет
        when(shared.calculateBundleDiscount(any(), any(), any()))
                .thenReturn(new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));

        // детали рисков
        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        // шаги расчёта
        when(stepsBuilder.buildCountryDefaultSteps(any(), any(), any(), any(), anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }

    // =====================================================
    // РЕЖИМ РАСЧЁТА
    // =====================================================

    @Test
    void shouldReturnCountryDefaultMode() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.calculationMode())
                .isEqualTo(MedicalRiskPremiumCalculator.CalculationMode.COUNTRY_DEFAULT);
    }

    // =====================================================
    // БАЗОВАЯ СТАВКА И ПРЕМИЯ
    // =====================================================

    @Test
    void shouldUseDefaultDayPremiumAsBaseRate() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.baseRate()).isEqualByComparingTo(DEFAULT_DAY_PREMIUM);
    }

    @Test
    void shouldCalculatePositivePremium() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.premium())
                .isNotNull()
                .isPositive()
                .hasScaleOf(2);
    }

    @Test
    void shouldPopulateCountryDefaultDayPremiumFields() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.countryDefaultDayPremium()).isEqualByComparingTo(DEFAULT_DAY_PREMIUM);
        assertThat(result.countryDefaultDayPremiumForInfo()).isEqualByComparingTo(DEFAULT_DAY_PREMIUM);
        assertThat(result.countryDefaultCurrency()).isEqualTo("EUR");
    }

    // =====================================================
    // КОЭФФИЦИЕНТЫ
    // =====================================================

    @Test
    void shouldApplyAgeCoefficient() {
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.ageCoefficient()).isEqualByComparingTo(AGE_COEFF);
        assertThat(result.age()).isEqualTo(35);
    }

    @Test
    void shouldApplyDurationCoefficient() {
        when(shared.getDurationCoefficient(anyLong(), any()))
                .thenReturn(new BigDecimal("1.2"));

        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.durationCoefficient()).isEqualByComparingTo("1.2");
    }

    @Test
    void shouldPopulateCountryInfoFromCountryEntity() {
        var result = strategy.calculate(standardAdultRequest());

        // countryCoefficient присутствует для инфо, но НЕ применяется в формуле
        assertThat(result.countryCoefficient()).isEqualByComparingTo(spainLowRisk().getRiskCoefficient());
        assertThat(result.countryName()).isEqualTo(spainLowRisk().getNameEn());
    }

    @Test
    void shouldNotApplyCountryCoeffToFormula() {
        // Меняем countryCoeff страны — не должно влиять на итоговую премию
        var highRiskCountry = spainLowRisk();
        highRiskCountry.setRiskCoefficient(new BigDecimal("5.00")); // очень высокий
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.of(highRiskCountry));

        BigDecimal premiumWithHighCountryCoeff = strategy.calculate(standardAdultRequest()).premium();

        // Сбрасываем на стандартный
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.of(spainLowRisk()));

        BigDecimal premiumWithNormalCountryCoeff = strategy.calculate(standardAdultRequest()).premium();

        assertThat(premiumWithHighCountryCoeff)
                .isEqualByComparingTo(premiumWithNormalCountryCoeff);
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

        // Пересчитываем basePremium с учётом доп. рисков
        BigDecimal basePremium = new BigDecimal("38.50");
        BigDecimal expectedWithRisks = basePremium
                .multiply(BigDecimal.ONE.add(baggageCoeff))
                .setScale(2, java.math.RoundingMode.HALF_UP);

        var result = strategy.calculate(requestWithSelectedRisks("TRAVEL_BAGGAGE"));

        assertThat(result.additionalRisksCoefficient()).isEqualByComparingTo(baggageCoeff);
        assertThat(result.premium()).isEqualByComparingTo(expectedWithRisks);
    }

    // =====================================================
    // СКИДКА
    // =====================================================

    @Test
    void shouldApplyBundleDiscount() {
        BigDecimal discount = new BigDecimal("5.00");
        when(shared.calculateBundleDiscount(any(), any(), any()))
                .thenReturn(new MedicalRiskPremiumCalculator.BundleDiscountResult(null, discount));

        BigDecimal premiumBeforeDiscount = new BigDecimal("38.50");
        BigDecimal expectedPremium = premiumBeforeDiscount.subtract(discount);

        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.premium()).isEqualByComparingTo(expectedPremium);
    }

    @Test
    void shouldReturnZeroDiscountWhenNoBundleApplies() {
        // @BeforeEach уже настраивает discount = ZERO
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

        assertThat(result.days()).isEqualTo(DAYS);
    }

    @Test
    void shouldReturnNullCoverageAmount() {
        // coverageAmount не применимо в COUNTRY_DEFAULT
        var result = strategy.calculate(standardAdultRequest());

        assertThat(result.coverageAmount()).isNull();
    }

    @Test
    void shouldDelegateStepsToBuilder() {
        strategy.calculate(standardAdultRequest());

        verify(stepsBuilder).buildCountryDefaultSteps(
                any(), any(), any(), any(), anyLong(), any(), any(), any());
    }

    // =====================================================
    // NEGATIVE SCENARIOS
    // =====================================================

    @Test
    void shouldFailWhenDefaultDayPremiumNotFound() {
        when(countryDefaultDayPremiumService.findDefaultDayPremium(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.calculate(standardAdultRequest()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Country default day premium not found");
    }

    @Test
    void shouldFailWhenCountryNotFound() {
        when(countryRepository.findActiveByIsoCode(anyString(), any()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.calculate(standardAdultRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country not found");
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЙ МЕТОД
    // =====================================================

    private CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremiumResult() {
        return new CountryDefaultDayPremiumService.DefaultPremiumResult(
                "ES",
                DEFAULT_DAY_PREMIUM,
                "EUR",
                "Spain default rate"
        );
    }
}
