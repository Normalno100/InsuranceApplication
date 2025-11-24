package org.javaguru.travel.insurance.core.calculators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;


import static org.junit.jupiter.api.Assertions.*;

class AgeCalculatorTest {

    private final AgeCalculator calculator = new AgeCalculator();

    // --------------------------------------------------------------------------------
    // calculateAge() tests
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("calculateAge: normal case")
    void testCalculateAgeNormal() {
        assertEquals(20,
                calculator.calculateAge(
                        LocalDate.of(2000, 1, 1),
                        LocalDate.of(2020, 1, 1))
        );
    }

    @Test
    @DisplayName("calculateAge: referenceDate = null → should use now()")
    void testCalculateAgeReferenceNull() {
        LocalDate birth = LocalDate.now().minusYears(30);
        assertEquals(30, calculator.calculateAge(birth, null));
    }

    @Test
    @DisplayName("calculateAge: birthDate null → throw")
    void testCalculateAgeBirthNull() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateAge(null, LocalDate.now()));
    }

    @Test
    @DisplayName("calculateAge: birth date in future → throw")
    void testCalculateAgeFutureBirth() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateAge(LocalDate.now().plusDays(2), LocalDate.now()));
    }

    @Test
    @DisplayName("calculateAge: exact birthday → age should match")
    void testCalculateAgeExactBirthday() {
        LocalDate birth = LocalDate.of(2000, 5, 10);
        LocalDate ref = LocalDate.of(2020, 5, 10);
        assertEquals(20, calculator.calculateAge(birth, ref));
    }

    // --------------------------------------------------------------------------------
    // getAgeCoefficient() tests
    // --------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("getAgeCoefficient: negative age → throw")
    @ValueSource(ints = {-1, -10, -100})
    void testCoefficientNegative(int age) {
        assertThrows(IllegalArgumentException.class, () -> calculator.getAgeCoefficient(age));
    }

    @ParameterizedTest
    @DisplayName("getAgeCoefficient: age > 80 → throw")
    @ValueSource(ints = {81, 90, 150})
    void testCoefficientTooOld(int age) {
        assertThrows(IllegalArgumentException.class, () -> calculator.getAgeCoefficient(age));
    }

    @ParameterizedTest
    @DisplayName("getAgeCoefficient: boundary values")
    @CsvSource({
            "0, 1.1",
            "5, 1.1",
            "6, 0.9",
            "17, 0.9",
            "18, 1.0",
            "30, 1.0",
            "31, 1.1",
            "40, 1.1",
            "41, 1.3",
            "50, 1.3",
            "51, 1.6",
            "60, 1.6",
            "61, 2.0",
            "70, 2.0",
            "71, 2.5",
            "80, 2.5"
    })
    void testCoefficientBoundaries(int age, String expectedCoefficient) {
        assertEquals(new BigDecimal(expectedCoefficient),
                calculator.getAgeCoefficient(age));
    }

    // --------------------------------------------------------------------------------
    // getAgeGroupDescription tests
    // --------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("getAgeGroupDescription: boundary values")
    @CsvSource({
            "0, Infants and toddlers",
            "5, Infants and toddlers",
            "6, Children and teenagers",
            "17, Children and teenagers",
            "18, Young adults",
            "30, Young adults",
            "31, Adults",
            "40, Adults",
            "41, Middle-aged",
            "50, Middle-aged",
            "51, Senior",
            "60, Senior",
            "61, Elderly",
            "70, Elderly",
            "71, Very elderly",
            "80, Very elderly"
    })
    void testDescriptionBoundaries(int age, String expectedDescription) {
        assertEquals(expectedDescription,
                calculator.getAgeGroupDescription(age));
    }

    // --------------------------------------------------------------------------------
    // isAgeValid tests
    // --------------------------------------------------------------------------------

    @ParameterizedTest
    @DisplayName("isAgeValid: valid ages 0..80")
    @ValueSource(ints = {0, 1, 20, 50, 80})
    void testIsAgeValidTrue(int age) {
        assertTrue(calculator.isAgeValid(age));
    }

    @ParameterizedTest
    @DisplayName("isAgeValid: invalid ages <0 or >80")
    @ValueSource(ints = {-5, -1, 81, 100})
    void testIsAgeValidFalse(int age) {
        assertFalse(calculator.isAgeValid(age));
    }

    // --------------------------------------------------------------------------------
    // calculateAgeAndCoefficient tests
    // --------------------------------------------------------------------------------

    @Test
    @DisplayName("calculateAgeAndCoefficient: normal case")
    void testCalcAgeAndCoefficientNormal() {
        AgeCalculator.AgeCalculationResult r =
                calculator.calculateAgeAndCoefficient(
                        LocalDate.of(1990, 1, 1),
                        LocalDate.of(2020, 1, 1)
                );

        assertEquals(30, r.age());
        assertEquals(new BigDecimal("1.0"), r.coefficient());
        assertEquals("Young adults", r.description());
    }

    @ParameterizedTest
    @DisplayName("calculateAgeAndCoefficient: boundary cases")
    @CsvSource({
            "2000-01-01, 2005-01-01, 1.1, Infants and toddlers",
            "2000-01-01, 2010-01-01, 0.9, Children and teenagers",
            "2000-01-01, 2020-01-01, 1.0, Young adults",
            "1980-01-01, 2020-01-01, 1.1, Adults",
            "1950-01-01, 2020-01-01, 2.0, Elderly"
    })
    void testCalcAgeAndCoefficientBoundaries(
            String birthStr,
            String refStr,
            String expectedCoeff,
            String expectedDesc
    ) {
        LocalDate birth = LocalDate.parse(birthStr);
        LocalDate ref = LocalDate.parse(refStr);

        AgeCalculator.AgeCalculationResult r =
                calculator.calculateAgeAndCoefficient(birth, ref);

        assertEquals(new BigDecimal(expectedCoeff), r.coefficient());
        assertEquals(expectedDesc, r.description());
    }

}
