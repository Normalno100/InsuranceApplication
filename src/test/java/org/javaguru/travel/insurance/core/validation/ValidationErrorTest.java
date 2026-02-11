package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationError Tests")
class ValidationErrorTest {

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
                "field",
                "message",
                "CODE_001",
                ValidationError.Severity.WARNING
        );

        assertThat(error.getSeverity()).isEqualTo(ValidationError.Severity.WARNING);
        assertThat(error.getErrorCode()).isEqualTo("CODE_001");
    }

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

    @Test
    @DisplayName("should support parameters for i18n")
    void shouldSupportParameters() {
        var error = ValidationError.error("age", "Age must be between {min} and {max}")
                .withParameter("min", 0)
                .withParameter("max", 80);

        assertThat(error.getParameters()).containsEntry("min", 0);
        assertThat(error.getParameters()).containsEntry("max", 80);
    }

    @Test
    @DisplayName("should chain multiple parameters")
    void shouldChainMultipleParameters() {
        var error = ValidationError.error("field", "Message")
                .withParameter("param1", "value1")
                .withParameter("param2", "value2")
                .withParameter("param3", 123);

        assertThat(error.getParameters()).hasSize(3);
    }

    @Test
    @DisplayName("toString() should format error properly")
    void shouldFormatToString() {
        var error = ValidationError.error("testField", "Test message");

        String str = error.toString();

        assertThat(str).contains("ERROR");
        assertThat(str).contains("testField");
        assertThat(str).contains("Test message");
    }
}