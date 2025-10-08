package org.javaguru.travel.insurance.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DateTimeService Tests")
class DateTimeServiceTest {

    private DateTimeService dateTimeService;

    @BeforeEach
    void setUp() {
        dateTimeService = new DateTimeService();
    }

    @Nested
    @DisplayName("Basic Scenarios")
    class BasicScenarios {

        @Test
        @DisplayName("Should return 0 days when dates are the same")
        void shouldReturnZeroDaysForSameDates() {
            LocalDate date = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date, date);

            assertEquals(0L, daysBetween);
        }

        @Test
        @DisplayName("Should return positive days when date2 is after date1")
        void shouldReturnPositiveDaysWhenDate2IsAfterDate1() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 10);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(9L, daysBetween);
        }

        @Test
        @DisplayName("Should return negative days when date1 is after date2")
        void shouldReturnNegativeDaysWhenDate1IsAfterDate2() {
            LocalDate date1 = LocalDate.of(2023, 1, 10);
            LocalDate date2 = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(-9L, daysBetween);
        }

        @Test
        @DisplayName("Should return 1 day for consecutive dates")
        void shouldReturnOneDayForConsecutiveDates() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 2);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }
    }

    @Nested
    @DisplayName("Different Time Periods")
    class DifferentTimePeriods {

        @Test
        @DisplayName("Should calculate days within same month")
        void shouldCalculateDaysWithinSameMonth() {
            LocalDate date1 = LocalDate.of(2023, 3, 10);
            LocalDate date2 = LocalDate.of(2023, 3, 25);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(15L, daysBetween);
        }

        @Test
        @DisplayName("Should calculate days across different months")
        void shouldCalculateDaysAcrossDifferentMonths() {
            LocalDate date1 = LocalDate.of(2023, 1, 31);
            LocalDate date2 = LocalDate.of(2023, 3, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(29L, daysBetween); // 31 Jan + 28 Feb (2023 не високосный)
        }

        @Test
        @DisplayName("Should calculate days across different years")
        void shouldCalculateDaysAcrossDifferentYears() {
            LocalDate date1 = LocalDate.of(2022, 12, 31);
            LocalDate date2 = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should calculate one week (7 days)")
        void shouldCalculateOneWeek() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 8);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(7L, daysBetween);
        }

        @Test
        @DisplayName("Should calculate one month (30 days)")
        void shouldCalculateOneMonth() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 31);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(30L, daysBetween);
        }

        @Test
        @DisplayName("Should calculate one year (365 days)")
        void shouldCalculateOneYear() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2024, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(365L, daysBetween);
        }

        @Test
        @DisplayName("Should calculate multiple years")
        void shouldCalculateMultipleYears() {
            LocalDate date1 = LocalDate.of(2020, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            // 2020 (366) + 2021 (365) + 2022 (365) = 1096
            assertEquals(1096L, daysBetween);
        }
    }

    @Nested
    @DisplayName("Leap Year Scenarios")
    class LeapYearScenarios {

        @Test
        @DisplayName("Should calculate leap year correctly (366 days)")
        void shouldCalculateLeapYear() {
            LocalDate date1 = LocalDate.of(2020, 1, 1);
            LocalDate date2 = LocalDate.of(2021, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(366L, daysBetween);
        }

        @Test
        @DisplayName("Should handle February 29 in leap year")
        void shouldHandleFebruary29InLeapYear() {
            LocalDate date1 = LocalDate.of(2020, 2, 28);
            LocalDate date2 = LocalDate.of(2020, 3, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(2L, daysBetween); // 28 Feb -> 29 Feb -> 1 Mar
        }

        @Test
        @DisplayName("Should calculate from Feb 29 to next year")
        void shouldCalculateFromFebruary29ToNextYear() {
            LocalDate date1 = LocalDate.of(2020, 2, 29);
            LocalDate date2 = LocalDate.of(2021, 2, 28);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(365L, daysBetween);
        }

        @Test
        @DisplayName("Should handle February in non-leap year")
        void shouldHandleFebruaryInNonLeapYear() {
            LocalDate date1 = LocalDate.of(2023, 2, 28);
            LocalDate date2 = LocalDate.of(2023, 3, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween); // 28 Feb -> 1 Mar (нет 29 Feb)
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle very large time span (100+ years)")
        void shouldHandleVeryLargeTimeSpan() {
            LocalDate date1 = LocalDate.of(1900, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            // Примерно 123 года * 365 дней + високосные года
            assertTrue(daysBetween > 44900); // примерная проверка
            assertTrue(daysBetween < 45000);
        }

        @Test
        @DisplayName("Should handle dates at start of year")
        void shouldHandleDatesAtStartOfYear() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 2);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle dates at end of year")
        void shouldHandleDatesAtEndOfYear() {
            LocalDate date1 = LocalDate.of(2023, 12, 30);
            LocalDate date2 = LocalDate.of(2023, 12, 31);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle year transition (Dec 31 -> Jan 1)")
        void shouldHandleYearTransition() {
            LocalDate date1 = LocalDate.of(2022, 12, 31);
            LocalDate date2 = LocalDate.of(2023, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle current date")
        void shouldHandleCurrentDate() {
            LocalDate today = LocalDate.now();
            LocalDate tomorrow = today.plusDays(1);

            long daysBetween = dateTimeService.getDaysBetween(today, tomorrow);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle far future dates")
        void shouldHandleFarFutureDates() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2123, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            // Примерно 100 лет * 365 дней + високосные
            assertTrue(daysBetween > 36500);
            assertTrue(daysBetween < 36600);
        }
    }

    @Nested
    @DisplayName("Parameterized Tests")
    class ParameterizedTests {

        @ParameterizedTest(name = "From {0} to {1} should be {2} days")
        @CsvSource({
                "2023-01-01, 2023-01-01, 0",   // Same date
                "2023-01-01, 2023-01-02, 1",   // 1 day
                "2023-01-01, 2023-01-10, 9",   // 9 days
                "2023-01-01, 2023-01-31, 30",  // 30 days
                "2023-01-01, 2023-02-01, 31",  // 31 days
                "2023-01-01, 2023-12-31, 364", // Almost year
                "2023-01-01, 2024-01-01, 365"  // Full year
        })
        @DisplayName("Should calculate days correctly for various date ranges")
        void shouldCalculateDaysForVariousRanges(LocalDate from, LocalDate to, long expected) {
            long daysBetween = dateTimeService.getDaysBetween(from, to);

            assertEquals(expected, daysBetween);
        }

        @ParameterizedTest(name = "Negative: From {0} to {1} should be {2} days")
        @CsvSource({
                "2023-01-10, 2023-01-01, -9",   // Reverse order
                "2023-02-01, 2023-01-01, -31",  // Month back
                "2024-01-01, 2023-01-01, -365"  // Year back
        })
        @DisplayName("Should calculate negative days when date1 > date2")
        void shouldCalculateNegativeDays(LocalDate from, LocalDate to, long expected) {
            long daysBetween = dateTimeService.getDaysBetween(from, to);

            assertEquals(expected, daysBetween);
        }

        @ParameterizedTest(name = "Month {0} should have correct days")
        @CsvSource({
                "1, 31",   // January
                "2, 28",   // February (non-leap)
                "3, 31",   // March
                "4, 30",   // April
                "5, 31",   // May
                "6, 30",   // June
                "7, 31",   // July
                "8, 31",   // August
                "9, 30",   // September
                "10, 31",  // October
                "11, 30",  // November
                "12, 31"   // December
        })
        @DisplayName("Should correctly handle different month lengths")
        void shouldHandleDifferentMonthLengths(int month, int expectedDays) {
            LocalDate startOfMonth = LocalDate.of(2023, month, 1);
            LocalDate endOfMonth = startOfMonth.plusMonths(1);

            long daysBetween = dateTimeService.getDaysBetween(startOfMonth, endOfMonth);

            assertEquals(expectedDays, daysBetween);
        }
    }

    @Nested
    @DisplayName("Special Dates")
    class SpecialDates {

        @Test
        @DisplayName("Should handle century transition (1999 -> 2000)")
        void shouldHandleCenturyTransition() {
            LocalDate date1 = LocalDate.of(1999, 12, 31);
            LocalDate date2 = LocalDate.of(2000, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle millennium transition (2000 -> 2001)")
        void shouldHandleMillenniumTransition() {
            LocalDate date1 = LocalDate.of(2000, 12, 31);
            LocalDate date2 = LocalDate.of(2001, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(1L, daysBetween);
        }

        @Test
        @DisplayName("Should handle year 2000 (leap year)")
        void shouldHandleYear2000() {
            LocalDate date1 = LocalDate.of(2000, 1, 1);
            LocalDate date2 = LocalDate.of(2001, 1, 1);

            long daysBetween = dateTimeService.getDaysBetween(date1, date2);

            assertEquals(366L, daysBetween); // 2000 был високосным
        }
    }

    @Nested
    @DisplayName("Symmetry Tests")
    class SymmetryTests {

        @Test
        @DisplayName("Should return opposite sign when dates are swapped")
        void shouldReturnOppositeSignWhenDatesAreSwapped() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 10);

            long forward = dateTimeService.getDaysBetween(date1, date2);
            long backward = dateTimeService.getDaysBetween(date2, date1);

            assertEquals(forward, -backward);
        }

        @Test
        @DisplayName("Should return same absolute value when dates are swapped")
        void shouldReturnSameAbsoluteValueWhenDatesAreSwapped() {
            LocalDate date1 = LocalDate.of(2023, 1, 1);
            LocalDate date2 = LocalDate.of(2023, 1, 10);

            long forward = dateTimeService.getDaysBetween(date1, date2);
            long backward = dateTimeService.getDaysBetween(date2, date1);

            assertEquals(Math.abs(forward), Math.abs(backward));
        }
    }
}