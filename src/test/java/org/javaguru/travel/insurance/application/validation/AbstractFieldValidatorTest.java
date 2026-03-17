package org.javaguru.travel.insurance.application.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для AbstractFieldValidator — проверяем механизм skipIfNull.
 */
@DisplayName("AbstractFieldValidator — skipIfNull behavior")
class AbstractFieldValidatorTest {

    private final ValidationContext context = new ValidationContext();

    // ── Test DTO ─────────────────────────────────────────────────────────────

    record TestDto(String value) {}

    // ── Конкретный валидатор для тестов ──────────────────────────────────────

    /**
     * Простой валидатор: проверяет что строка содержит "valid".
     * Используется для проверки поведения skipIfNull.
     */
    static class TestStringValidator extends AbstractFieldValidator<TestDto, String> {

        private boolean doValidateCalled = false;

        TestStringValidator(boolean skipIfNull) {
            super("value", TestDto::value, 10, skipIfNull);
        }

        @Override
        protected ValidationResult doValidateField(String fieldValue, ValidationContext context) {
            doValidateCalled = true;
            if (!"valid".equals(fieldValue)) {
                return ValidationResult.failure(
                        ValidationError.error("value", "Must contain 'valid'")
                );
            }
            return success();
        }

        boolean isDoValidateCalled() {
            return doValidateCalled;
        }
    }

    // ── Тесты skipIfNull=true ─────────────────────────────────────────────────

    @Nested
    @DisplayName("skipIfNull=true")
    class SkipIfNullEnabled {

        @Test
        @DisplayName("should return success() without calling doValidateField() when value is null")
        void shouldSkipValidationWhenNull() {
            var validator = new TestStringValidator(true);
            var dto = new TestDto(null);

            var result = validator.validate(dto, context);

            assertThat(result.isValid()).isTrue();
            assertThat(validator.isDoValidateCalled())
                    .as("doValidateField() should NOT be called when value is null and skipIfNull=true")
                    .isFalse();
        }

        @Test
        @DisplayName("should call doValidateField() and return error when value is non-null and invalid")
        void shouldCallDoValidateWhenNonNullInvalidValue() {
            var validator = new TestStringValidator(true);
            var dto = new TestDto("invalid_value");

            var result = validator.validate(dto, context);

            assertThat(result.isValid()).isFalse();
            assertThat(validator.isDoValidateCalled()).isTrue();
            assertThat(result.getErrors().get(0).getField()).isEqualTo("value");
        }

        @Test
        @DisplayName("should call doValidateField() and return success when value is non-null and valid")
        void shouldCallDoValidateWhenNonNullValidValue() {
            var validator = new TestStringValidator(true);
            var dto = new TestDto("valid");

            var result = validator.validate(dto, context);

            assertThat(result.isValid()).isTrue();
            assertThat(validator.isDoValidateCalled()).isTrue();
        }
    }

    // ── Тесты skipIfNull=false ─────────────────────────────────────────────────

    @Nested
    @DisplayName("skipIfNull=false")
    class SkipIfNullDisabled {

        @Test
        @DisplayName("should call doValidateField() even when value is null")
        void shouldCallDoValidateEvenWhenNull() {
            var validator = new TestStringValidator(false);
            var dto = new TestDto(null);

            validator.validate(dto, context);

            assertThat(validator.isDoValidateCalled())
                    .as("doValidateField() SHOULD be called when skipIfNull=false, even for null")
                    .isTrue();
        }

        @Test
        @DisplayName("should call doValidateField() for non-null value")
        void shouldCallDoValidateForNonNullValue() {
            var validator = new TestStringValidator(false);
            var dto = new TestDto("valid");

            var result = validator.validate(dto, context);

            assertThat(result.isValid()).isTrue();
            assertThat(validator.isDoValidateCalled()).isTrue();
        }
    }

    // ── Проверка реальных валидаторов ─────────────────────────────────────────

    @Nested
    @DisplayName("Real validators — null returns success()")
    class RealValidatorsNullBehavior {

        @Test
        @DisplayName("DateInPastValidator: null → success() (no null-guard in code)")
        void dateInPastValidatorNullReturnsSuccess() {
            var validator = new org.javaguru.travel.insurance.application.validation.rule.business
                    .DateInPastValidator<>(
                    "personBirthDate",
                    dto -> null // всегда null
            );

            // Используем анонимный объект как T
            record Req() {}
            var validator2 = new org.javaguru.travel.insurance.application.validation.rule.business
                    .DateInPastValidator<org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest>(
                    "personBirthDate",
                    req -> req.getPersonBirthDate()
            );

            var request = org.javaguru.travel.insurance.application.dto
                    .TravelCalculatePremiumRequest.builder()
                    .personBirthDate(null) // null
                    .build();

            var result = validator2.validate(request, context);

            assertThat(result.isValid())
                    .as("DateInPastValidator with null birthDate should return success()")
                    .isTrue();
        }

        @Test
        @DisplayName("IsoCodeValidator: null → success() (no null-guard in code)")
        void isoCodeValidatorNullReturnsSuccess() {
            record Dto(String code) {}
            var validator = new org.javaguru.travel.insurance.application.validation.rule.structural
                    .IsoCodeValidator<Dto>("code", 2, Dto::code);

            var result = validator.validate(new Dto(null), context);

            assertThat(result.isValid())
                    .as("IsoCodeValidator with null value should return success()")
                    .isTrue();
        }

        @Test
        @DisplayName("StringLengthValidator: null → success() (no null-guard in code)")
        void stringLengthValidatorNullReturnsSuccess() {
            record Dto(String name) {}
            var validator = new org.javaguru.travel.insurance.application.validation.rule.structural
                    .StringLengthValidator<Dto>("name", 1, 100, Dto::name);

            var result = validator.validate(new Dto(null), context);

            assertThat(result.isValid())
                    .as("StringLengthValidator with null value should return success()")
                    .isTrue();
        }
    }
}