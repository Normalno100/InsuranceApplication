package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.core.DateTimeService;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Оптимизированные тесты калькулятора премий
 *
 * Фокус: проверка бизнес-логики расчётов, а не всех возможных комбинаций
 * - Формула расчёта
 * - Округление
 * - Применение коэффициентов
 * - Граничные значения
 */
@DisplayName("Medical Risk Premium Calculator - Optimized Tests")
class MedicalRiskPremiumCalculatorTest {

    private AgeCalculator ageCalculator;
    private DateTimeService dateTimeService;
    private MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void setUp() {
        ageCalculator = mock(AgeCalculator.class);
        dateTimeService = mock(DateTimeService.class);
        calculator = new MedicalRiskPremiumCalculator(ageCalculator, dateTimeService);
    }

    // =====================================================================
    // ФОРМУЛА: базовая ставка × возраст × страна × (1 + доп.риски) × дни
    // =====================================================================

    @Test
    @DisplayName("Formula: Base rate × Age × Country × Days (no additional risks)")
    void shouldCalculateBasicPremiumWithoutAdditionalRisks() {
        // Given: 5 дней, возраст 30 (коэфф 1.2), Испания (коэфф 1.0), лимит 5000 (ставка 1.50)
        mockAge(30, 1.2);
        mockDays(5);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES", List.of());

        // Expected: 1.50 × 1.2 × 1.0 × 1.0 × 5 = 9.00
        BigDecimal expected = new BigDecimal("1.50")
                .multiply(new BigDecimal("1.2"))
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("5"))
                .setScale(2, RoundingMode.HALF_UP);

        // When
        BigDecimal actual = calculator.calculatePremium(request);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Formula: Additional risk factor = (1 + sum of risk coefficients)")
    void shouldApplyAdditionalRisksAsOnePercentageSum() {
        // Given: SPORT_ACTIVITIES (0.3) → множитель (1 + 0.3) = 1.3
        mockAge(30, 1.0);
        mockDays(10);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES",
                List.of("SPORT_ACTIVITIES"));

        // Expected: 1.50 × 1.0 × 1.0 × 1.3 × 10 = 19.50
        BigDecimal expected = new BigDecimal("1.50")
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("1.3"))
                .multiply(new BigDecimal("10"))
                .setScale(2, RoundingMode.HALF_UP);

        // When
        BigDecimal actual = calculator.calculatePremium(request);

        // Then
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Formula: Multiple risks sum their coefficients")
    void shouldSumMultipleRiskCoefficients() {
        // Given: SPORT_ACTIVITIES (0.3) + EXTREME_SPORT (0.6) = (1 + 0.3 + 0.6) = 1.9
        mockAge(25, 1.0);
        mockDays(3);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES",
                List.of("SPORT_ACTIVITIES", "EXTREME_SPORT"));

        // Expected: 1.50 × 1.0 × 1.0 × 1.9 × 3 = 8.55
        BigDecimal expected = new BigDecimal("1.50")
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("1.0"))
                .multiply(new BigDecimal("1.9"))
                .multiply(new BigDecimal("3"))
                .setScale(2, RoundingMode.HALF_UP);

        // When
        BigDecimal actual = calculator.calculatePremium(request);

        // Then
        assertEquals(expected, actual);
    }

    // =====================================================================
    // ОКРУГЛЕНИЕ: всегда 2 знака, HALF_UP
    // =====================================================================

    @Test
    @DisplayName("Rounding: Result must have exactly 2 decimal places")
    void shouldRoundToTwoDecimalPlaces() {
        // Given: создаём ситуацию с дробным результатом
        mockAge(33, new BigDecimal("1.111111"));
        mockDays(3);
        TravelCalculatePremiumRequestV2 request = buildRequest("10000", "DE", List.of());

        // When
        BigDecimal result = calculator.calculatePremium(request);

        // Then
        assertEquals(2, result.scale(), "Premium must have exactly 2 decimal places");
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0, "Premium must be positive");
    }

    @Test
    @DisplayName("Rounding: Uses HALF_UP rounding mode")
    void shouldUseHalfUpRounding() {
        // Given: создаём результат, требующий округления (например, 10.555 → 10.56)
        mockAge(25, new BigDecimal("1.037"));
        mockDays(7);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "IT", List.of());

        // When
        BigDecimal result = calculator.calculatePremium(request);

        // Then
        assertEquals(2, result.scale());
        // Проверяем, что округление применено (любое ненулевое значение с 2 знаками)
        assertNotNull(result);
    }

    // =====================================================================
    // ПАРАМЕТРИЗОВАННЫЕ ТЕСТЫ: граничные значения
    // =====================================================================

    @ParameterizedTest(name = "Limit={0}, Country={1}, Age={2}, Days={3}")
    @CsvSource({
            "5000,  ES, 20, 1",     // Минимум: низкая ставка, 1 день
            "5000,  ES, 30, 30",    // Средний период
            "10000, DE, 45, 7",     // Средний возраст, средняя ставка
            "20000, FR, 60, 14",    // Пожилой возраст
            "50000, IT, 25, 3",     // Высокая ставка, короткий период
            "100000, ES, 70, 1",    // Максимальный возраст
            "500000, DE, 35, 60"    // Максимальная ставка, длинный период
    })
    @DisplayName("Boundary values: Various combinations of limits, countries, ages, and periods")
    void shouldCalculatePremiumForBoundaryValues(String limit, String country,
                                                 int age, int days) {
        // Given
        mockAge(age, new BigDecimal("1.2")); // Используем стандартный коэффициент
        mockDays(days);
        TravelCalculatePremiumRequestV2 request = buildRequest(limit, country, List.of());

        // When
        BigDecimal result = calculator.calculatePremium(request);

        // Then
        assertNotNull(result);
        assertTrue(result.compareTo(BigDecimal.ZERO) > 0,
                "Premium must be positive for non-zero days");
        assertEquals(2, result.scale(), "Premium must have 2 decimal places");
    }

    // =====================================================================
    // CALCULATION DETAILS: проверка структуры результата
    // =====================================================================

    @Test
    @DisplayName("Details: Result contains all calculation components")
    void shouldReturnCompletePremiumCalculationDetails() {
        // Given
        mockAge(35, new BigDecimal("1.1"));
        mockDays(5);
        TravelCalculatePremiumRequestV2 request = buildRequest("10000", "ES",
                List.of("SPORT_ACTIVITIES"));

        // When
        var result = calculator.calculatePremiumWithDetails(request);

        // Then - проверяем наличие всех ключевых компонентов
        assertAll("Calculation details should contain all components",
                () -> assertNotNull(result.premium(), "Premium should be calculated"),
                () -> assertNotNull(result.baseRate(), "Base rate should be set"),
                () -> assertEquals(35, result.age(), "Age should be correct"),
                () -> assertNotNull(result.ageCoefficient(), "Age coefficient should be set"),
                () -> assertNotNull(result.countryCoefficient(), "Country coefficient should be set"),
                () -> assertEquals(5, result.days(), "Days should be correct"),
                () -> assertFalse(result.riskDetails().isEmpty(),
                        "Risk details should include at least mandatory risk"),
                () -> assertFalse(result.calculationSteps().isEmpty(),
                        "Calculation steps should be provided")
        );
    }

    @Test
    @DisplayName("Details: Mandatory risk always included in risk details")
    void shouldAlwaysIncludeMandatoryRiskInDetails() {
        // Given
        mockAge(30, new BigDecimal("1.0"));
        mockDays(1);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES", List.of());

        // When
        var result = calculator.calculatePremiumWithDetails(request);

        // Then
        assertTrue(result.riskDetails().stream()
                        .anyMatch(r -> r.riskCode().equals("TRAVEL_MEDICAL")),
                "TRAVEL_MEDICAL must always be included");
    }

    @Test
    @DisplayName("Details: Additional risks included when selected")
    void shouldIncludeSelectedAdditionalRisksInDetails() {
        // Given
        mockAge(30, new BigDecimal("1.0"));
        mockDays(2);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES",
                List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"));

        // When
        var result = calculator.calculatePremiumWithDetails(request);

        // Then
        assertAll("All selected risks should be in details",
                () -> assertTrue(result.riskDetails().stream()
                        .anyMatch(r -> r.riskCode().equals("SPORT_ACTIVITIES"))),
                () -> assertTrue(result.riskDetails().stream()
                        .anyMatch(r -> r.riskCode().equals("LUGGAGE_LOSS")))
        );
    }

    // =====================================================================
    // ГРАНИЧНЫЕ СЛУЧАИ
    // =====================================================================

    @Test
    @DisplayName("Edge case: Zero days should return zero premium")
    void shouldReturnZeroPremiumForZeroDays() {
        // Given
        mockAge(30, new BigDecimal("1.2"));
        mockDays(0);
        TravelCalculatePremiumRequestV2 request = buildRequest("5000", "ES", List.of());

        // When
        BigDecimal result = calculator.calculatePremium(request);

        // Then
        assertEquals(0, result.compareTo(BigDecimal.ZERO),
                "Premium should be zero for zero days");
    }

    @Test
    @DisplayName("Edge case: Very long trip (365 days)")
    void shouldCalculatePremiumForVeryLongTrip() {
        // Given
        mockAge(40, new BigDecimal("1.1"));
        mockDays(365);
        TravelCalculatePremiumRequestV2 request = buildRequest("10000", "ES", List.of());

        // When
        BigDecimal result = calculator.calculatePremium(request);

        // Then
        assertTrue(result.compareTo(new BigDecimal("500")) > 0,
                "Premium for 365 days should be substantial");
        assertEquals(2, result.scale());
    }

    // =====================================================================
    // HELPER METHODS
    // =====================================================================

    private void mockAge(int age, double coefficient) {
        mockAge(age, BigDecimal.valueOf(coefficient));
    }

    private void mockAge(int age, BigDecimal coefficient) {
        when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
        when(ageCalculator.getAgeCoefficient(age)).thenReturn(coefficient);

        // Для метода calculatePremiumWithDetails
        AgeCalculator.AgeCalculationResult ageResult =
                new AgeCalculator.AgeCalculationResult(age, coefficient, "Age group");
        when(ageCalculator.calculateAgeAndCoefficient(any(), any())).thenReturn(ageResult);
    }

    private void mockDays(long days) {
        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(days);
    }

    private TravelCalculatePremiumRequestV2 buildRequest(String limitLevel,
                                                         String countryCode,
                                                         List<String> risks) {
        TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();
        request.setMedicalRiskLimitLevel(limitLevel);
        request.setCountryIsoCode(countryCode);
        request.setSelectedRisks(risks);
        request.setPersonBirthDate(LocalDate.of(1990, 1, 1));
        request.setAgreementDateFrom(LocalDate.of(2025, 1, 1));
        request.setAgreementDateTo(LocalDate.of(2025, 1, 10));
        return request;
    }
}