package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Тесты ValidationError — акцент на иммутабельности (п. 5.2 плана рефакторинга).
 */
@DisplayName("ValidationError Tests")
class ValidationErrorTest {

    // ── Базовое создание ─────────────────────────────────────────────────────

    @Test
    @DisplayName("should create error with field and message")
    void shouldCreateBasicError() {
        var error = new ValidationError("testField", "Error message");

        assertThat(error.getField()).isEqualTo("testField");
        assertThat(error.getMessage()).isEqualTo("Error message");
        assertThat(error.getSeverity()).isEqualTo(ValidationError.Severity.ERROR);
    }

    @Test
    @DisplayName("should create error with custom severity")
    void shouldCreateErrorWithCustomSeverity() {
        var error = new ValidationError(
                "field", "message", "CODE_001", ValidationError.Severity.WARNING);

        assertThat(error.getSeverity()).isEqualTo(ValidationError.Severity.WARNING);
        assertThat(error.getErrorCode()).isEqualTo("CODE_001");
    }

    // ── Фабричные методы ─────────────────────────────────────────────────────

    @Test
    @DisplayName("warning() factory should create warning")
    void shouldCreateWarningViaFactory() {
        var error = ValidationError.warning("field", "Warning message");

        assertThat(error.getSeverity()).isEqualTo(ValidationError.Severity.WARNING);
        assertThat(error.getMessage()).isEqualTo("Warning message");
    }

    @Test
    @DisplayName("critical() factory should create critical error")
    void shouldCreateCriticalViaFactory() {
        var error = ValidationError.critical("field", "Critical error");

        assertThat(error.getSeverity()).isEqualTo(ValidationError.Severity.CRITICAL);
        assertThat(error.getMessage()).isEqualTo("Critical error");
    }

    // ── Иммутабельность ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Immutability (refactoring 5.2)")
    class ImmutabilityTests {

        @Test
        @DisplayName("withParameter() should return NEW instance, not mutate original")
        void withParameterShouldReturnNewInstance() {
            var original = ValidationError.error("age", "Age invalid");

            var withParam = original.withParameter("min", 0);

            assertThat(withParam).isNotSameAs(original);
        }

        @Test
        @DisplayName("original should remain unchanged after withParameter()")
        void originalShouldBeUnchangedAfterWithParameter() {
            var original = ValidationError.error("age", "Age invalid");

            original.withParameter("min", 0);  // результат намеренно игнорируем

            // Оригинал не должен содержать параметр
            assertThat(original.getParameters()).doesNotContainKey("min");
        }

        @Test
        @DisplayName("withParameter() chain should accumulate all parameters")
        void chainShouldAccumulateAllParameters() {
            var error = ValidationError.error("age", "Age must be between {min} and {max}")
                    .withParameter("min", 0)
                    .withParameter("max", 80);

            assertThat(error.getParameters())
                    .containsEntry("min", 0)
                    .containsEntry("max", 80);
        }

        @Test
        @DisplayName("each step of the chain is a new independent instance")
        void eachStepInChainIsIndependent() {
            var base  = ValidationError.error("field", "msg");
            var step1 = base.withParameter("a", 1);
            var step2 = step1.withParameter("b", 2);

            assertThat(base.getParameters()).isEmpty();
            assertThat(step1.getParameters()).containsOnlyKeys("a");
            assertThat(step2.getParameters()).containsOnlyKeys("a", "b");
        }

        @Test
        @DisplayName("getParameters() should return unmodifiable map")
        void getParametersShouldReturnUnmodifiableMap() {
            var error = ValidationError.error("field", "msg")
                    .withParameter("key", "value");

            Map<String, Object> params = error.getParameters();

            // Попытка изменить карту должна бросить исключение
            assertThatThrownBy(() -> params.put("newKey", "newValue"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("newly created error has empty unmodifiable parameters")
        void newErrorHasEmptyUnmodifiableParameters() {
            var error = ValidationError.error("field", "msg");

            Map<String, Object> params = error.getParameters();

            assertThat(params).isEmpty();
            assertThatThrownBy(() -> params.put("k", "v"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("withParameter() preserves all fields from original")
        void withParameterPreservesAllFields() {
            var original = new ValidationError(
                    "myField", "myMessage", "ERR_001", ValidationError.Severity.WARNING);

            var extended = original.withParameter("extra", 42);

            assertThat(extended.getField()).isEqualTo("myField");
            assertThat(extended.getMessage()).isEqualTo("myMessage");
            assertThat(extended.getErrorCode()).isEqualTo("ERR_001");
            assertThat(extended.getSeverity()).isEqualTo(ValidationError.Severity.WARNING);
            assertThat(extended.getParameters()).containsEntry("extra", 42);
        }
    }

    // ── toString ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("toString() should format error properly")
    void shouldFormatToString() {
        var error = ValidationError.error("testField", "Test message");

        assertThat(error.toString())
                .contains("ERROR")
                .contains("testField")
                .contains("Test message");
    }
}