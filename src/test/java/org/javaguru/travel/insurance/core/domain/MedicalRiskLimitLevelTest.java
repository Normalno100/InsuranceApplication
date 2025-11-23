package org.javaguru.travel.insurance.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MedicalRiskLimitLevel Tests")
class MedicalRiskLimitLevelTest {

    @Nested
    @DisplayName("Enum Values - Basic Checks")
    class EnumValues {

        @Test
        @DisplayName("Should have 7 coverage levels")
        void shouldHave7CoverageLevels() {
            MedicalRiskLimitLevel[] levels = MedicalRiskLimitLevel.values();

            assertEquals(7, levels.length, "Should have exactly 7 coverage levels");
        }

        @Test
        @DisplayName("All levels should have non-null properties")
        void allLevelsShouldHaveNonNullProperties() {
            for (MedicalRiskLimitLevel level : MedicalRiskLimitLevel.values()) {
                assertAll("Level " + level.name() + " properties",
                        () -> assertNotNull(level.getCode(), "Code should not be null"),
                        () -> assertNotNull(level.getCoverage(), "Coverage should not be null"),
                        () -> assertNotNull(level.getDailyRate(), "Daily rate should not be null")
                );
            }
        }

        @Test
        @DisplayName("All levels should have positive values")
        void allLevelsShouldHavePositiveValues() {
            for (MedicalRiskLimitLevel level : MedicalRiskLimitLevel.values()) {
                assertAll("Level " + level.name() + " positive values",
                        () -> assertTrue(level.getCoverage().compareTo(BigDecimal.ZERO) > 0,
                                "Coverage should be positive"),
                        () -> assertTrue(level.getDailyRate().compareTo(BigDecimal.ZERO) > 0,
                                "Daily rate should be positive")
                );
            }
        }

        @Test
        @DisplayName("Coverage amounts should be in ascending order")
        void coverageAmountsShouldBeInAscendingOrder() {
            MedicalRiskLimitLevel[] levels = MedicalRiskLimitLevel.values();

            for (int i = 1; i < levels.length; i++) {
                assertTrue(
                        levels[i].getCoverage().compareTo(levels[i-1].getCoverage()) > 0,
                        String.format("Coverage of %s should be greater than %s",
                                levels[i].name(), levels[i-1].name())
                );
            }
        }

        @Test
        @DisplayName("Daily rates should be in ascending order")
        void dailyRatesShouldBeInAscendingOrder() {
            MedicalRiskLimitLevel[] levels = MedicalRiskLimitLevel.values();

            for (int i = 1; i < levels.length; i++) {
                assertTrue(
                        levels[i].getDailyRate().compareTo(levels[i-1].getDailyRate()) > 0,
                        String.format("Daily rate of %s should be greater than %s",
                                levels[i].name(), levels[i-1].name())
                );
            }
        }
    }

    @Nested
    @DisplayName("Find By Code - Success Cases")
    class FindByCodeSuccess {

        @ParameterizedTest(name = "Code {0} should return {1}")
        @CsvSource({
                "5000,    LEVEL_5000",
                "10000,   LEVEL_10000",
                "20000,   LEVEL_20000",
                "50000,   LEVEL_50000",
                "100000,  LEVEL_100000",
                "200000,  LEVEL_200000",
                "500000,  LEVEL_500000"
        })
        @DisplayName("Should find level by valid code")
        void shouldFindLevelByValidCode(String code, String expectedName) {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.fromCode(code);

            assertNotNull(level);
            assertEquals(expectedName, level.name());
            assertEquals(code, level.getCode());
        }

        @Test
        @DisplayName("Should find LEVEL_5000 by code")
        void shouldFindLevel5000() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.fromCode("5000");

            assertAll(
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_5000, level),
                    () -> assertEquals("5000", level.getCode()),
                    () -> assertEquals(new BigDecimal("5000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("1.50"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("Should find LEVEL_100000 by code")
        void shouldFindLevel100000() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.fromCode("100000");

            assertAll(
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_100000, level),
                    () -> assertEquals("100000", level.getCode()),
                    () -> assertEquals(new BigDecimal("100000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("7.00"), level.getDailyRate())
            );
        }
    }

    @Nested
    @DisplayName("Find By Code - Error Cases")
    class FindByCodeError {

        @ParameterizedTest
        @ValueSource(strings = {"0", "1000", "3000", "15000", "999999", "invalid"})
        @DisplayName("Should throw exception for invalid code")
        void shouldThrowExceptionForInvalidCode(String invalidCode) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> MedicalRiskLimitLevel.fromCode(invalidCode)
            );

            assertTrue(exception.getMessage().contains(invalidCode),
                    "Exception message should contain the invalid code");
        }

        @Test
        @DisplayName("Should throw exception for null code")
        void shouldThrowExceptionForNullCode() {
            assertThrows(
                    Exception.class,
                    () -> MedicalRiskLimitLevel.fromCode(null)
            );
        }

        @Test
        @DisplayName("Should throw exception for empty code")
        void shouldThrowExceptionForEmptyCode() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MedicalRiskLimitLevel.fromCode("")
            );
        }
    }

    @Nested
    @DisplayName("Find By Amount - Success Cases")
    class FindByAmountSuccess {

        @ParameterizedTest(name = "Amount {0} should return {1}")
        @CsvSource({
                "1000,    LEVEL_5000",
                "5000,    LEVEL_5000",
                "5001,    LEVEL_10000",
                "10000,   LEVEL_10000",
                "15000,   LEVEL_20000",
                "20000,   LEVEL_20000",
                "45000,   LEVEL_50000",
                "50000,   LEVEL_50000",
                "75000,   LEVEL_100000",
                "100000,  LEVEL_100000",
                "150000,  LEVEL_200000",
                "200000,  LEVEL_200000",
                "300000,  LEVEL_500000",
                "500000,  LEVEL_500000"
        })
        @DisplayName("Should find correct level for various amounts")
        void shouldFindCorrectLevelForAmount(BigDecimal amount, String expectedName) {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(amount);

            assertNotNull(level);
            assertEquals(expectedName, level.name());
            assertTrue(level.getCoverage().compareTo(amount) >= 0,
                    String.format("Coverage %s should be >= requested amount %s",
                            level.getCoverage(), amount));
        }

        @Test
        @DisplayName("Should return LEVEL_5000 for minimum amount")
        void shouldReturnLevel5000ForMinimumAmount() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(new BigDecimal("1"));

            assertEquals(MedicalRiskLimitLevel.LEVEL_5000, level);
        }

        @Test
        @DisplayName("Should return LEVEL_500000 for very large amount")
        void shouldReturnLevel500000ForVeryLargeAmount() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(
                    new BigDecimal("10000000")
            );

            // Для сумм больше максимального покрытия возвращается максимальный уровень
            assertEquals(MedicalRiskLimitLevel.LEVEL_500000, level);
            assertEquals(new BigDecimal("500000"), level.getCoverage());
        }

        @Test
        @DisplayName("Should return exact level when amount equals coverage")
        void shouldReturnExactLevelWhenAmountEqualsCoverage() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(
                    new BigDecimal("50000")
            );

            assertEquals(MedicalRiskLimitLevel.LEVEL_50000, level);
            assertEquals(new BigDecimal("50000"), level.getCoverage());
        }
    }

    @Nested
    @DisplayName("Specific Level Properties")
    class SpecificLevelProperties {

        @Test
        @DisplayName("LEVEL_5000 should have correct properties")
        void level5000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_5000;

            assertAll(
                    () -> assertEquals("5000", level.getCode()),
                    () -> assertEquals(new BigDecimal("5000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("1.50"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_10000 should have correct properties")
        void level10000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_10000;

            assertAll(
                    () -> assertEquals("10000", level.getCode()),
                    () -> assertEquals(new BigDecimal("10000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("2.00"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_20000 should have correct properties")
        void level20000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_20000;

            assertAll(
                    () -> assertEquals("20000", level.getCode()),
                    () -> assertEquals(new BigDecimal("20000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("3.00"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_50000 should have correct properties")
        void level50000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_50000;

            assertAll(
                    () -> assertEquals("50000", level.getCode()),
                    () -> assertEquals(new BigDecimal("50000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("4.50"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_100000 should have correct properties")
        void level100000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_100000;

            assertAll(
                    () -> assertEquals("100000", level.getCode()),
                    () -> assertEquals(new BigDecimal("100000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("7.00"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_200000 should have correct properties")
        void level200000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_200000;

            assertAll(
                    () -> assertEquals("200000", level.getCode()),
                    () -> assertEquals(new BigDecimal("200000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("12.00"), level.getDailyRate())
            );
        }

        @Test
        @DisplayName("LEVEL_500000 should have correct properties")
        void level500000ShouldHaveCorrectProperties() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.LEVEL_500000;

            assertAll(
                    () -> assertEquals("500000", level.getCode()),
                    () -> assertEquals(new BigDecimal("500000"), level.getCoverage()),
                    () -> assertEquals(new BigDecimal("20.00"), level.getDailyRate())
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle amount exactly at boundary")
        void shouldHandleAmountExactlyAtBoundary() {
            // Граничные значения
            assertAll(
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_5000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("5000"))),
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_10000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("5000.01"))),
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_10000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("10000"))),
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_20000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("10000.01")))
            );
        }

        @Test
        @DisplayName("Should handle decimal amounts")
        void shouldHandleDecimalAmounts() {
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(
                    new BigDecimal("15999.99")
            );

            assertEquals(MedicalRiskLimitLevel.LEVEL_20000, level);
        }

        @Test
        @DisplayName("Should return max level for amounts exceeding maximum coverage")
        void shouldReturnMaxLevelForAmountsExceedingMaximumCoverage() {
            // Для сумм превышающих максимальное покрытие, возвращается максимальный уровень
            assertAll(
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_500000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("600000"))),
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_500000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("1000000"))),
                    () -> assertEquals(MedicalRiskLimitLevel.LEVEL_500000,
                            MedicalRiskLimitLevel.findByAmount(new BigDecimal("999999999")))
            );
        }

        @Test
        @DisplayName("Code should be case sensitive")
        void codeShouldBeCaseSensitive() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MedicalRiskLimitLevel.fromCode("LEVEL_50000")
            );
        }

        @Test
        @DisplayName("All codes should be unique")
        void allCodesShouldBeUnique() {
            MedicalRiskLimitLevel[] levels = MedicalRiskLimitLevel.values();
            long uniqueCodes = java.util.Arrays.stream(levels)
                    .map(MedicalRiskLimitLevel::getCode)
                    .distinct()
                    .count();

            assertEquals(levels.length, uniqueCodes, "All codes should be unique");
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicValidation {

        @Test
        @DisplayName("Higher coverage should have higher daily rate")
        void higherCoverageShouldHaveHigherDailyRate() {
            MedicalRiskLimitLevel[] levels = MedicalRiskLimitLevel.values();

            for (int i = 1; i < levels.length; i++) {
                MedicalRiskLimitLevel lower = levels[i-1];
                MedicalRiskLimitLevel higher = levels[i];

                assertTrue(
                        higher.getCoverage().compareTo(lower.getCoverage()) > 0 &&
                                higher.getDailyRate().compareTo(lower.getDailyRate()) > 0,
                        String.format("Higher coverage (%s) should have higher rate than lower coverage (%s)",
                                higher.name(), lower.name())
                );
            }
        }

        @Test
        @DisplayName("Daily rate should be reasonable percentage of coverage")
        void dailyRateShouldBeReasonablePercentageOfCoverage() {
            for (MedicalRiskLimitLevel level : MedicalRiskLimitLevel.values()) {
                BigDecimal ratePercentage = level.getDailyRate()
                        .divide(level.getCoverage(), 6, java.math.RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));

                assertTrue(
                        ratePercentage.compareTo(new BigDecimal("0.001")) >= 0 &&
                                ratePercentage.compareTo(new BigDecimal("1")) <= 0,
                        String.format("Daily rate should be reasonable percentage of coverage for %s (was %s%%)",
                                level.name(), ratePercentage)
                );
            }
        }

        @Test
        @DisplayName("Should find most cost-effective level for requested amount")
        void shouldFindMostCostEffectiveLevelForRequestedAmount() {
            BigDecimal requestedAmount = new BigDecimal("45000");
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(requestedAmount);

            // Должен выбрать LEVEL_50000 (минимальный уровень, покрывающий запрос)
            assertEquals(MedicalRiskLimitLevel.LEVEL_50000, level);
            assertTrue(level.getCoverage().compareTo(requestedAmount) >= 0);

            // Проверка что это минимальный подходящий уровень
            // (только для сумм в пределах максимального покрытия)
            if (requestedAmount.compareTo(new BigDecimal("500000")) <= 0) {
                for (MedicalRiskLimitLevel otherLevel : MedicalRiskLimitLevel.values()) {
                    if (otherLevel.getCoverage().compareTo(requestedAmount) >= 0) {
                        assertTrue(
                                level.getCoverage().compareTo(otherLevel.getCoverage()) <= 0,
                                "Should select minimum sufficient coverage"
                        );
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Scenario: Budget traveler (minimal coverage)")
        void scenarioBudgetTraveler() {
            BigDecimal requestedAmount = new BigDecimal("3000");
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(requestedAmount);

            assertEquals(MedicalRiskLimitLevel.LEVEL_5000, level);
            assertEquals(new BigDecimal("1.50"), level.getDailyRate());
        }

        @Test
        @DisplayName("Scenario: Standard EU travel")
        void scenarioStandardEUTravel() {
            BigDecimal requestedAmount = new BigDecimal("30000");
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(requestedAmount);

            assertEquals(MedicalRiskLimitLevel.LEVEL_50000, level);
            assertEquals(new BigDecimal("4.50"), level.getDailyRate());
        }

        @Test
        @DisplayName("Scenario: USA travel (expensive healthcare)")
        void scenarioUSATravel() {
            BigDecimal requestedAmount = new BigDecimal("100000");
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(requestedAmount);

            assertEquals(MedicalRiskLimitLevel.LEVEL_100000, level);
            assertEquals(new BigDecimal("7.00"), level.getDailyRate());
        }

        @Test
        @DisplayName("Scenario: Premium worldwide coverage")
        void scenarioPremiumWorldwide() {
            BigDecimal requestedAmount = new BigDecimal("500000");
            MedicalRiskLimitLevel level = MedicalRiskLimitLevel.findByAmount(requestedAmount);

            assertEquals(MedicalRiskLimitLevel.LEVEL_500000, level);
            assertEquals(new BigDecimal("20.00"), level.getDailyRate());
        }
    }
}