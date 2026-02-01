package org.javaguru.travel.insurance.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for Coefficient Value Object
 */
@DisplayName("Coefficient Value Object Tests")
class CoefficientTest {

    @Test
    @DisplayName("Should create valid coefficient")
    void shouldCreateValidCoefficient() {
        // When
        Coefficient coefficient = new Coefficient(new BigDecimal("1.5"));

        // Then
        assertThat(coefficient.value()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("Should create coefficient from double")
    void shouldCreateCoefficientFromDouble() {
        // When
        Coefficient coefficient = Coefficient.of(1.5);

        // Then
        assertThat(coefficient.value()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("Should create coefficient from string")
    void shouldCreateCoefficientFromString() {
        // When
        Coefficient coefficient = Coefficient.of("1.5");

        // Then
        assertThat(coefficient.value()).isEqualByComparingTo(new BigDecimal("1.5"));
    }

    @Test
    @DisplayName("Should reject null coefficient value")
    void shouldRejectNullValue() {
        // When & Then
        assertThatThrownBy(() -> new Coefficient(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coefficient value cannot be null");
    }

    @Test
    @DisplayName("Should reject negative coefficient")
    void shouldRejectNegativeCoefficient() {
        // When & Then
        assertThatThrownBy(() -> new Coefficient(new BigDecimal("-0.5")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Coefficient cannot be negative");
    }

    @Test
    @DisplayName("Should accept coefficient equals to 1")
    void shouldAcceptCoefficientOne() {
        // When & Then
        assertThatNoException().isThrownBy(() -> new Coefficient(BigDecimal.ONE));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "1.0", "1.5", "2.0", "2.5", "3.0", "5.0"})
    @DisplayName("Should accept valid coefficient values")
    void shouldAcceptValidCoefficientValues(String value) {
        // When & Then
        assertThatNoException().isThrownBy(() -> Coefficient.of(value));
    }

    @Test
    @DisplayName("Should multiply coefficients")
    void shouldMultiplyCoefficients() {
        // Given
        Coefficient coef1 = Coefficient.of(1.5);
        Coefficient coef2 = Coefficient.of(2.0);

        // When
        Coefficient result = coef1.multiply(coef2);

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("3.0"));
    }

    @Test
    @DisplayName("Should add coefficients")
    void shouldAddCoefficients() {
        // Given
        Coefficient coef1 = Coefficient.of(1.5);
        Coefficient coef2 = Coefficient.of(0.5);

        // When
        Coefficient result = coef1.add(coef2);

        // Then
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("2.0"));
    }

    @Test
    @DisplayName("Should be equal to another Coefficient with same value")
    void shouldBeEqualToCoefficientWithSameValue() {
        // Given
        Coefficient coef1 = Coefficient.of(1.5);
        Coefficient coef2 = Coefficient.of("1.5");

        // When & Then
        assertThat(coef1).isEqualTo(coef2);
        assertThat(coef1.hashCode()).isEqualTo(coef2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal to Coefficient with different value")
    void shouldNotBeEqualToCoefficientWithDifferentValue() {
        // Given
        Coefficient coef1 = Coefficient.of(1.5);
        Coefficient coef2 = Coefficient.of(2.0);

        // When & Then
        assertThat(coef1).isNotEqualTo(coef2);
    }

    @Test
    @DisplayName("Should have meaningful toString")
    void shouldHaveMeaningfulToString() {
        // Given
        Coefficient coefficient = Coefficient.of(1.5);

        // When
        String str = coefficient.toString();

        // Then
        assertThat(str).contains("1.5");
    }

    @Test
    @DisplayName("Should handle very small coefficients")
    void shouldHandleVerySmallCoefficients() {
        // When & Then
        assertThatNoException().isThrownBy(() -> Coefficient.of("0.01"));
        assertThat(Coefficient.of("0.01").value()).isEqualByComparingTo(new BigDecimal("0.01"));
    }

    @Test
    @DisplayName("Should handle very large coefficients")
    void shouldHandleVeryLargeCoefficients() {
        // When & Then
        assertThatNoException().isThrownBy(() -> Coefficient.of("10.0"));
        assertThat(Coefficient.of("10.0").value()).isEqualByComparingTo(new BigDecimal("10.0"));
    }

    @Test
    @DisplayName("Should preserve precision in calculations")
    void shouldPreservePrecisionInCalculations() {
        // Given
        Coefficient coef1 = Coefficient.of("1.111");
        Coefficient coef2 = Coefficient.of("2.222");

        // When
        Coefficient result = coef1.multiply(coef2);

        // Then
        assertThat(result.value().scale()).isGreaterThan(0);
    }
}