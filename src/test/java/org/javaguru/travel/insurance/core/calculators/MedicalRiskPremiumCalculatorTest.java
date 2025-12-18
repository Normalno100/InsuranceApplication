package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.core.DateTimeService;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Упрощённые тесты калькулятора - проверяем формулу и граничные случаи
 * Формула: базовая_ставка × коэфф_возраста × коэфф_страны × (1 + доп_риски) × дни
 */
class MedicalRiskPremiumCalculatorTest {

    private final AgeCalculator ageCalculator = mock(AgeCalculator.class);
    private final DateTimeService dateService = mock(DateTimeService.class);
    private final MedicalRiskPremiumCalculator calculator =
            new MedicalRiskPremiumCalculator(ageCalculator, dateService);

    // ========== ОСНОВНАЯ ФОРМУЛА ==========

    @Test
    void shouldCalculateBasicPremium() {
        // Базовая ставка 1.50, возраст 1.0, страна 1.0, нет рисков, 5 дней
        // Ожидается: 1.50 × 1.0 × 1.0 × 1.0 × 5 = 7.50
        mockAge(30, 1.0);
        mockDays(5);
        var request = request("5000", "ES", List.of());

        var premium = calculator.calculatePremium(request);

        assertThat(premium).isEqualByComparingTo("7.50");
    }

    @Test
    void shouldApplyAdditionalRisks() {
        // SPORT_ACTIVITIES имеет коэффициент 0.3 → множитель (1 + 0.3) = 1.3
        // 1.50 × 1.0 × 1.0 × 1.3 × 10 = 19.50
        mockAge(30, 1.0);
        mockDays(10);
        var request = request("5000", "ES", List.of("SPORT_ACTIVITIES"));

        var premium = calculator.calculatePremium(request);

        assertThat(premium).isEqualByComparingTo("19.50");
    }

    @Test
    void shouldSumMultipleRisks() {
        // SPORT (0.3) + EXTREME (0.6) = (1 + 0.9) = 1.9
        // 1.50 × 1.0 × 1.0 × 1.9 × 3 = 8.55
        mockAge(25, 1.0);
        mockDays(3);
        var request = request("5000", "ES",
                List.of("SPORT_ACTIVITIES", "EXTREME_SPORT"));

        var premium = calculator.calculatePremium(request);

        assertThat(premium).isEqualByComparingTo("8.55");
    }

    // ========== ГРАНИЧНЫЕ СЛУЧАИ ==========

    @Test
    void shouldReturnZero_whenZeroDays() {
        mockAge(30, 1.0);
        mockDays(0);
        var request = request("5000", "ES", List.of());

        var premium = calculator.calculatePremium(request);

        assertThat(premium).isEqualByComparingTo("0");
    }

    @ParameterizedTest(name = "Level={0}, Country={1}, Age={2}, Days={3}")
    @CsvSource({
            "5000,  ES, 20, 1",     // Минимум
            "10000, DE, 45, 7",     // Средний
            "100000, ES, 70, 1",    // Максимальный возраст
            "500000, DE, 35, 60"    // Длинная поездка
    })
    void shouldHandleBoundaryValues(String level, String country, int age, int days) {
        mockAge(age, 1.2);
        mockDays(days);
        var request = request(level, country, List.of());

        var premium = calculator.calculatePremium(request);

        assertThat(premium).isGreaterThan(BigDecimal.ZERO);
        assertThat(premium.scale()).isEqualTo(2);
    }

    // ========== ДЕТАЛИ РАСЧЁТА ==========

    @Test
    void shouldReturnCalculationDetails() {
        mockAge(35, new BigDecimal("1.1"));
        mockDays(5);
        var request = request("10000", "ES", List.of("SPORT_ACTIVITIES"));

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.premium()).isNotNull();
        assertThat(result.baseRate()).isNotNull();
        assertThat(result.age()).isEqualTo(35);
        assertThat(result.days()).isEqualTo(5);
        assertThat(result.riskDetails()).isNotEmpty();
        assertThat(result.calculationSteps()).isNotEmpty();
    }

    @Test
    void shouldIncludeMandatoryRisk() {
        mockAge(30, 1.0);
        mockDays(1);
        var request = request("5000", "ES", List.of());

        var result = calculator.calculatePremiumWithDetails(request);

        assertThat(result.riskDetails())
                .anyMatch(r -> r.riskCode().equals("TRAVEL_MEDICAL"));
    }

    // ========== HELPERS ==========

    private void mockAge(int age, double coefficient) {
        mockAge(age, BigDecimal.valueOf(coefficient));
    }

    private void mockAge(int age, BigDecimal coefficient) {
        when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
        when(ageCalculator.getAgeCoefficient(age)).thenReturn(coefficient);
        when(ageCalculator.calculateAgeAndCoefficient(any(), any()))
                .thenReturn(new AgeCalculator.AgeCalculationResult(
                        age, coefficient, "Age group"));
    }

    private void mockDays(long days) {
        when(dateService.getDaysBetween(any(), any())).thenReturn(days);
    }

    private TravelCalculatePremiumRequestV2 request(String level, String country,
                                                    List<String> risks) {
        var req = new TravelCalculatePremiumRequestV2();
        req.setMedicalRiskLimitLevel(level);
        req.setCountryIsoCode(country);
        req.setSelectedRisks(risks);
        req.setPersonBirthDate(LocalDate.of(1990, 1, 1));
        req.setAgreementDateFrom(LocalDate.of(2025, 1, 1));
        req.setAgreementDateTo(LocalDate.of(2025, 1, 10));
        return req;
    }
}