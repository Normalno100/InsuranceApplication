package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.rule.structural.IsoCodeValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotBlankValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotNullValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.StringLengthValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Structural Validators Tests")
class StructuralValidatorsTest {

    private final ValidationContext context = new ValidationContext();

    // ========== Test DTO ==========
    record TestDto(String name, Integer age) {}

    // ========== NOT NULL VALIDATOR ==========

    @Nested
    @DisplayName("NotNullValidator")
    class NotNullValidatorTest {

        @Test
        @DisplayName("should pass when value is not null")
        void shouldPassWhenNotNull() {
            var validator = new NotNullValidator<TestDto>("name", TestDto::name);

            var result = validator.validate(new TestDto("John", 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should fail when value is null")
        void shouldFailWhenNull() {
            var validator = new NotNullValidator<TestDto>("name", TestDto::name);

            var result = validator.validate(new TestDto(null, 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(1);
            assertThat(result.getErrors().get(0).getField()).isEqualTo("name");
        }

        @Test
        @DisplayName("should be critical")
        void shouldBeCritical() {
            var validator = new NotNullValidator<TestDto>("name", TestDto::name);

            assertThat(validator.isCritical()).isTrue();
        }
    }

    // ========== NOT BLANK VALIDATOR ==========

    @Nested
    @DisplayName("NotBlankValidator")
    class NotBlankValidatorTest {

        @Test
        @DisplayName("should pass when value is not blank")
        void shouldPassWhenNotBlank() {
            var validator = new NotBlankValidator<TestDto>("name", TestDto::name);

            var result = validator.validate(new TestDto("John", 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t", "\n"})
        @DisplayName("should fail when value is blank")
        void shouldFailWhenBlank(String value) {
            var validator = new NotBlankValidator<TestDto>("name", TestDto::name);

            var result = validator.validate(new TestDto(value, 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getField()).isEqualTo("name");
        }
    }

    // ========== STRING LENGTH VALIDATOR ==========

    @Nested
    @DisplayName("StringLengthValidator")
    class StringLengthValidatorTest {

        @Test
        @DisplayName("should pass when length is within bounds")
        void shouldPassWhenWithinBounds() {
            var validator = new StringLengthValidator<TestDto>("name", 2, 10, TestDto::name);

            var result = validator.validate(new TestDto("John", 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should fail when length is too short")
        void shouldFailWhenTooShort() {
            var validator = new StringLengthValidator<TestDto>("name", 5, 10, TestDto::name);

            var result = validator.validate(new TestDto("Joe", 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("at least 5");
        }

        @Test
        @DisplayName("should fail when length is too long")
        void shouldFailWhenTooLong() {
            var validator = new StringLengthValidator<TestDto>("name", 1, 5, TestDto::name);

            var result = validator.validate(new TestDto("Alexander", 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("at most 5");
        }

        @Test
        @DisplayName("should pass when null (handled by other validators)")
        void shouldPassWhenNull() {
            var validator = new StringLengthValidator<TestDto>("name", 1, 10, TestDto::name);

            var result = validator.validate(new TestDto(null, 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when exactly at min boundary")
        void shouldPassAtMinBoundary() {
            var validator = new StringLengthValidator<TestDto>("name", 4, 10, TestDto::name);

            var result = validator.validate(new TestDto("John", 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when exactly at max boundary")
        void shouldPassAtMaxBoundary() {
            var validator = new StringLengthValidator<TestDto>("name", 1, 4, TestDto::name);

            var result = validator.validate(new TestDto("John", 25), context);

            assertThat(result.isValid()).isTrue();
        }
    }

    // ========== ISO CODE VALIDATOR ==========

    @Nested
    @DisplayName("IsoCodeValidator")
    class IsoCodeValidatorTest {

        @ParameterizedTest
        @ValueSource(strings = {"ES", "DE", "FR", "US", "GB"})
        @DisplayName("should pass for valid 2-letter codes")
        void shouldPassForValidCodes(String code) {
            var validator = new IsoCodeValidator<TestDto>("name", 2, TestDto::name);

            var result = validator.validate(new TestDto(code, 25), context);

            assertThat(result.isValid()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"E", "ESP", "e5", "E S", "12"})
        @DisplayName("should fail for invalid codes")
        void shouldFailForInvalidCodes(String code) {
            var validator = new IsoCodeValidator<TestDto>("name", 2, TestDto::name);

            var result = validator.validate(new TestDto(code, 25), context);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should fail when length is wrong")
        void shouldFailWhenWrongLength() {
            var validator = new IsoCodeValidator<TestDto>("name", 2, TestDto::name);

            var result = validator.validate(new TestDto("USA", 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("2 characters");
        }

        @Test
        @DisplayName("should fail when contains lowercase")
        void shouldFailWhenLowercase() {
            var validator = new IsoCodeValidator<TestDto>("name", 2, TestDto::name);

            var result = validator.validate(new TestDto("es", 25), context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("uppercase");
        }
    }
}