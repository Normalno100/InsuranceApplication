package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.core.DateTimeService;
import org.javaguru.travel.insurance.core.domain.Country;
import org.javaguru.travel.insurance.core.domain.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.core.domain.RiskType;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MedicalRiskPremiumCalculatorTest {

    private AgeCalculator ageCalculator;
    private DateTimeService dateTimeService;
    private MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void setUp() {
        ageCalculator = mock(AgeCalculator.class);
        dateTimeService = mock(DateTimeService.class);
        calculator = new MedicalRiskPremiumCalculator(ageCalculator, dateTimeService);
    }

    private TravelCalculatePremiumRequestV2 buildRequest(
            String limitLevel,
            LocalDate birth,
            LocalDate from,
            LocalDate to,
            String country,
            List<String> risks
    ) {
        TravelCalculatePremiumRequestV2 r = new TravelCalculatePremiumRequestV2();
        r.setMedicalRiskLimitLevel(limitLevel);
        r.setPersonBirthDate(birth);
        r.setAgreementDateFrom(from);
        r.setAgreementDateTo(to);
        r.setCountryIsoCode(country);
        r.setSelectedRisks(risks);
        return r;
    }

    // =====================================================================
    // 1. ТЕСТЫ ФОРМУЛЫ
    // =====================================================================

    @Test
    @DisplayName("Formula without additional risks - multiplication check")
    void testFormulaWithoutAdditionalRisks() {

        when(ageCalculator.calculateAge(any(), any())).thenReturn(30);
        when(ageCalculator.getAgeCoefficient(30)).thenReturn(BigDecimal.valueOf(1.2));

        // 5 дней
        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(5L);

        TravelCalculatePremiumRequestV2 request = buildRequest(
                "5000",
                LocalDate.of(1990, 1, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 6),
                "ES",
                List.of()
        );

        BigDecimal dailyRate = MedicalRiskLimitLevel.LEVEL_5000.getDailyRate();
        BigDecimal countryCoeff = Country.SPAIN.getRiskCoefficient();

        BigDecimal expected = dailyRate
                .multiply(BigDecimal.valueOf(1.2))
                .multiply(countryCoeff)
                .multiply(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(5))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal result = calculator.calculatePremium(request);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Formula with additional risk - verifies (1 + SUM)")
    void testFormulaAdditionalRisks() {

        when(ageCalculator.calculateAge(any(), any())).thenReturn(40);
        when(ageCalculator.getAgeCoefficient(40)).thenReturn(BigDecimal.valueOf(1.1));

        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

        TravelCalculatePremiumRequestV2 request = buildRequest(
                "10000",
                LocalDate.of(1985, 5, 5),
                LocalDate.of(2025, 3, 1),
                LocalDate.of(2025, 3, 11),
                "DE",
                List.of(RiskType.SPORT_ACTIVITIES.getCode())
        );

        BigDecimal base = MedicalRiskLimitLevel.LEVEL_10000.getDailyRate();
        BigDecimal age = BigDecimal.valueOf(1.1);
        BigDecimal country = Country.GERMANY.getRiskCoefficient();
        BigDecimal risk = RiskType.SPORT_ACTIVITIES.getCoefficient();

        BigDecimal expected = base
                .multiply(age)
                .multiply(country)
                .multiply(BigDecimal.ONE.add(risk))
                .multiply(BigDecimal.valueOf(10))
                .setScale(2);

        assertEquals(expected, calculator.calculatePremium(request));
    }

    // =====================================================================
    // 2. ТЕСТЫ ОКРУГЛЕНИЯ
    // =====================================================================

    @Test
    @DisplayName("Rounding HALF_UP - 2 decimals")
    void testRoundingHalfUp() {

        when(ageCalculator.calculateAge(any(), any())).thenReturn(27);
        when(ageCalculator.getAgeCoefficient(27)).thenReturn(new BigDecimal("1.333333"));

        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(1L);

        TravelCalculatePremiumRequestV2 request = buildRequest(
                "20000",
                LocalDate.of(1998, 2, 2),
                LocalDate.of(2025, 2, 2),
                LocalDate.of(2025, 2, 3),
                "FR",
                List.of()
        );

        BigDecimal res = calculator.calculatePremium(request);
        assertEquals(res.scale(), 2);
    }

    @Test
    @DisplayName("Rounding - third digit verification")
    void testRoundingThirdDigit() {

        when(ageCalculator.calculateAge(any(), any())).thenReturn(50);
        when(ageCalculator.getAgeCoefficient(50)).thenReturn(new BigDecimal("1.276"));

        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(3L);

        TravelCalculatePremiumRequestV2 request = buildRequest(
                "5000",
                LocalDate.of(1975, 10, 1),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 1, 4),
                "IT",
                List.of()
        );

        BigDecimal result = calculator.calculatePremium(request);
        assertEquals(2, result.scale());
    }

    // =====================================================================
    // 3. КОМБИНАЦИИ ПАРАМЕТРОВ (30 тестов)
    // =====================================================================

    @Nested
    @DisplayName("Parameter combinations (30 tests)")
    class CombinationTests {

        @Test
        void combo1() { combo("5000", "ES", 20, 1.0, 1, List.of()); }

        @Test
        void combo2() { combo("5000", "ES", 60, 1.4, 7, List.of()); }

        @Test
        void combo3() { combo("10000", "DE", 30, 1.2, 3, List.of()); }

        @Test
        void combo4() { combo("10000", "DE", 50, 1.3, 10, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo5() { combo("20000", "FR", 18, 0.9, 14, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo6() { combo("20000", "IT", 25, 1.0, 21, List.of("SPORT_ACTIVITIES", "EXTREME_SPORT")); }

        @Test
        void combo7() { combo("5000", "AT", 35, 1.1, 2, List.of()); }

        @Test
        void combo8() { combo("5000", "NL", 70, 1.5, 5, List.of("EXTREME_SPORT")); }

        @Test
        void combo9() { combo("10000", "ES", 33, 1.1, 9, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo10() { combo("20000", "DE", 45, 1.2, 6, List.of()); }

        @Test
        void combo11() { combo("5000", "BE", 55, 1.3, 30, List.of()); }

        @Test
        void combo12() { combo("10000", "CH", 37, 1.1, 1, List.of()); }

        @Test
        void combo13() { combo("20000", "ES", 41, 1.2, 8, List.of("EXTREME_SPORT")); }

        @Test
        void combo14() { combo("10000", "DE", 22, 1.0, 11, List.of("SPORT_ACTIVITIES", "EXTREME_SPORT")); }

        @Test
        void combo15() { combo("5000", "ES", 49, 1.2, 17, List.of()); }

        @Test
        void combo16() { combo("5000", "DE", 71, 1.5, 4, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo17() { combo("20000", "SE", 29, 1.0, 7, List.of()); }

        @Test
        void combo18() { combo("10000", "NO", 52, 1.3, 12, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo19() { combo("5000", "DK", 26, 1.0, 1, List.of()); }

        @Test
        void combo20() { combo("20000", "ES", 64, 1.4, 18, List.of("EXTREME_SPORT")); }

        @Test
        void combo21() { combo("10000", "JP", 23, 1.0, 2, List.of()); }

        @Test
        void combo22() { combo("20000", "DE", 57, 1.3, 13, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo23() { combo("5000", "KR", 19, 0.9, 9, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo24() { combo("10000", "ES", 31, 1.1, 16, List.of()); }

        @Test
        void combo25() { combo("20000", "AU", 48, 1.2, 22, List.of("EXTREME_SPORT")); }

        @Test
        void combo26() { combo("5000", "ES", 72, 1.5, 27, List.of()); }

        @Test
        void combo27() { combo("10000", "DE", 45, 1.2, 10, List.of("SPORT_ACTIVITIES")); }

        @Test
        void combo28() { combo("20000", "NZ", 36, 1.1, 2, List.of()); }

        @Test
        void combo29() { combo("5000", "CA", 28, 1.0, 14, List.of()); }

        @Test
        void combo30() { combo("20000", "ES", 62, 1.4, 3, List.of()); }

        private void combo(String level, String country, int age, double ageCoeff, int days, List<String> risks) {
            when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
            when(ageCalculator.getAgeCoefficient(age)).thenReturn(BigDecimal.valueOf(ageCoeff));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn((long) days);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    level,
                    LocalDate.of(1990, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 1).plusDays(days),
                    country,
                    risks
            );

            BigDecimal result = calculator.calculatePremium(req);
            assertNotNull(result);
            assertTrue(result.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    // =====================================================================
    // 4. ТЕСТЫ ДЕТАЛЕЙ РАСЧЕТА (10 тестов)
    // =====================================================================

    @Nested
    @DisplayName("Calculation details (10 tests)")
    class DetailsTests {

        @Test
        void detailsContainsBaseRate() {
            mockAge(30, 1.1);
            mockDays(5);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "5000",
                    LocalDate.of(1990, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 6),
                    "ES",
                    List.of()
            );

            var result = calculator.calculatePremiumWithDetails(req);
            assertEquals(MedicalRiskLimitLevel.LEVEL_5000.getDailyRate(), result.baseRate());
        }

        @Test
        void detailsAgeCorrect() {
            mockAge(44, 1.2);
            mockDays(5);

            TravelCalculatePremiumRequestV2 req = buildRequest("10000",
                    LocalDate.of(1981, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 6),
                    "DE", List.of());

            var result = calculator.calculatePremiumWithDetails(req);
            assertEquals(44, result.age());
        }

        @Test
        void detailsCountryCoefficientCorrect() {
            mockAge(20, 1.0);
            mockDays(3);

            TravelCalculatePremiumRequestV2 req = buildRequest("5000",
                    LocalDate.of(2005, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 4),
                    "ES", List.of());

            var res = calculator.calculatePremiumWithDetails(req);
            assertEquals(Country.SPAIN.getRiskCoefficient(), res.countryCoefficient());
        }

        @Test
        void detailsRiskPremiumsContainMandatory() {
            mockAge(25, 1.0);
            mockDays(1);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "5000",
                    LocalDate.of(2000, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 2),
                    "ES",
                    List.of()
            );

            var res = calculator.calculatePremiumWithDetails(req);

            assertTrue(res.riskDetails().stream()
                    .anyMatch(r -> r.riskCode().equals(RiskType.TRAVEL_MEDICAL.getCode())));
        }

        @Test
        void detailsRiskPremiumsIncludeAdditional() {
            mockAge(30, 1.0);
            mockDays(2);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "5000",
                    LocalDate.of(1995, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 3),
                    "ES",
                    List.of(RiskType.SPORT_ACTIVITIES.getCode())
            );

            var res = calculator.calculatePremiumWithDetails(req);

            assertTrue(res.riskDetails().stream()
                    .anyMatch(r -> r.riskCode().equals(RiskType.SPORT_ACTIVITIES.getCode())));
        }

        @Test
        void detailsContainsCalculationSteps() {
            mockAge(31, 1.1);
            mockDays(2);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "10000",
                    LocalDate.of(1994, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 3),
                    "DE",
                    List.of()
            );

            var res = calculator.calculatePremiumWithDetails(req);
            assertFalse(res.calculationSteps().isEmpty());
        }

        @Test
        void totalCoefficientCorrect() {
            mockAge(30, 1.2);
            mockDays(3);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "5000",
                    LocalDate.of(1995, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 4),
                    "DE",
                    List.of()
            );

            var res = calculator.calculatePremiumWithDetails(req);

            BigDecimal expected = BigDecimal.valueOf(1.2)
                    .multiply(Country.GERMANY.getRiskCoefficient())
                    .multiply(BigDecimal.ONE);

            assertEquals(expected, res.totalCoefficient());
        }

        @Test
        void daysCorrect() {
            mockAge(33, 1.1);
            mockDays(10);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "20000",
                    LocalDate.of(1990, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 11),
                    "FR", List.of());

            var res = calculator.calculatePremiumWithDetails(req);
            assertEquals(10, res.days());
        }

        @Test
        void detailsPremiumPositive() {
            mockAge(45, 1.3);
            mockDays(7);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "10000",
                    LocalDate.of(1980, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 8),
                    "ES",
                    List.of());

            var res = calculator.calculatePremiumWithDetails(req);
            assertTrue(res.premium().compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        void riskDetailPremiumCalculated() {
            mockAge(25, 1.0);
            mockDays(1);

            TravelCalculatePremiumRequestV2 req = buildRequest(
                    "5000",
                    LocalDate.of(2000, 1, 1),
                    LocalDate.of(2025, 1, 1),
                    LocalDate.of(2025, 1, 2),
                    "ES",
                    List.of(RiskType.SPORT_ACTIVITIES.getCode())
            );

            var result = calculator.calculatePremiumWithDetails(req);

            var sportDetail = result.riskDetails()
                    .stream()
                    .filter(r -> r.riskCode().equals(RiskType.SPORT_ACTIVITIES.getCode()))
                    .findFirst()
                    .orElseThrow();

            assertTrue(sportDetail.premium().compareTo(BigDecimal.ZERO) > 0);
        }
    }

    // =====================================================================
    // UTILS
    // =====================================================================

    private void mockAge(int age, double coeff) {
        when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
        when(ageCalculator.getAgeCoefficient(age)).thenReturn(BigDecimal.valueOf(coeff));

        // Добавляем мок для calculateAgeAndCoefficient(...)
        AgeCalculator.AgeCalculationResult ageResult =
                new AgeCalculator.AgeCalculationResult(
                        age,
                        BigDecimal.valueOf(coeff),
                        "age group description"
                );

        when(ageCalculator.calculateAgeAndCoefficient(any(), any())).thenReturn(ageResult);
    }


    private void mockDays(long days) {
        when(dateTimeService.getDaysBetween(any(), any())).thenReturn(days);
    }
}
