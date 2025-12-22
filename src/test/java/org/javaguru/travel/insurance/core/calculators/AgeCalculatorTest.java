package org.javaguru.travel.insurance.core.calculators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Упрощённые тесты AgeCalculator
 * Фокус: бизнес-логика, а не implementation details
 */
@DisplayName("AgeCalculator")
class AgeCalculatorTest {

    private final AgeCalculator calculator = new AgeCalculator();

    // ========== CORE BUSINESS LOGIC ==========

    @ParameterizedTest(name = "age {0} → coefficient {1}")
    @CsvSource({
            "3, 1.1",    // infants
            "10, 0.9",   // children
            "25, 1.0",   // young adults
            "35, 1.1",   // adults
            "45, 1.3",   // middle-aged
            "55, 1.6",   // senior
            "65, 2.0",   // elderly
            "75, 2.5"    // very elderly
    })
    @DisplayName("should return correct coefficient for age")
    void shouldReturnCorrectCoefficientForAge(int age, String expectedCoefficient) {
        assertEquals(new BigDecimal(expectedCoefficient), calculator.getAgeCoefficient(age));
    }

    @Test
    @DisplayName("should calculate age correctly")
    void shouldCalculateAge() {
        LocalDate birth = LocalDate.of(2000, 1, 1);
        LocalDate ref = LocalDate.of(2020, 1, 1);

        assertEquals(20, calculator.calculateAge(birth, ref));
    }

    @Test
    @DisplayName("should calculate age and coefficient together")
    void shouldCalculateAgeAndCoefficient() {
        LocalDate birth = LocalDate.of(1990, 1, 1);
        LocalDate ref = LocalDate.of(2020, 1, 1);

        var result = calculator.calculateAgeAndCoefficient(birth, ref);

        assertEquals(30, result.age());
        assertEquals(new BigDecimal("1.0"), result.coefficient());
        assertNotNull(result.description());
    }

    // ========== VALIDATION ==========

    @ParameterizedTest
    @ValueSource(ints = {-1, 81, 100})
    @DisplayName("should reject invalid ages")
    void shouldRejectInvalidAges(int age) {
        assertThrows(IllegalArgumentException.class, () -> calculator.getAgeCoefficient(age));
    }

    @Test
    @DisplayName("should reject null birth date")
    void shouldRejectNullBirthDate() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateAge(null, LocalDate.now()));
    }

    @Test
    @DisplayName("should reject future birth date")
    void shouldRejectFutureBirthDate() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateAge(LocalDate.now().plusDays(1), LocalDate.now()));
    }
}