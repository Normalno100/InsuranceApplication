package org.javaguru.travel.insurance.core.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ValidationResult Tests")
class ValidationResultTest {

    @Test
    @DisplayName("success() should create valid result")
    void shouldCreateSuccessResult() {
        var result = ValidationResult.success();

        assertThat(result.isValid()).isTrue();
        assertThat(result.hasErrors()).isFalse();
        assertThat(result.getErrors()).isEmpty();
    }

    @Test
    @DisplayName("failure() should create invalid result with errors")
    void shouldCreateFailureResult() {
        var error = ValidationError.error("field1", "Error message");
        var result = ValidationResult.failure(error);

        assertThat(result.isValid()).isFalse();
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("field1");
    }

    @Test
    @DisplayName("failure() should create result with multiple errors")
    void shouldCreateFailureWithMultipleErrors() {
        var errors = List.of(
                ValidationError.error("field1", "Error 1"),
                ValidationError.error("field2", "Error 2")
        );
        var result = ValidationResult.failure(errors);

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors()).hasSize(2);
    }

    @Test
    @DisplayName("merge() should combine two results")
    void shouldMergeTwoResults() {
        var result1 = ValidationResult.failure(
                ValidationError.error("field1", "Error 1")
        );
        var result2 = ValidationResult.failure(
                ValidationError.error("field2", "Error 2")
        );

        var merged = result1.merge(result2);

        assertThat(merged.hasErrors()).isTrue();
        assertThat(merged.getErrors()).hasSize(2);
        assertThat(merged.isValid()).isFalse();
    }

    @Test
    @DisplayName("merge() should preserve success when merging with success")
    void shouldPreserveSuccessWhenMerging() {
        var result1 = ValidationResult.success();
        var result2 = ValidationResult.success();

        var merged = result1.merge(result2);

        assertThat(merged.isValid()).isTrue();
        assertThat(merged.hasErrors()).isFalse();
    }

    @Test
    @DisplayName("hasCriticalErrors() should detect critical errors")
    void shouldDetectCriticalErrors() {
        var result = ValidationResult.failure(
                ValidationError.critical("field1", "Critical error")
        );

        assertThat(result.hasCriticalErrors()).isTrue();
    }

    @Test
    @DisplayName("hasCriticalErrors() should return false for non-critical errors")
    void shouldNotDetectCriticalWhenOnlyErrors() {
        var result = ValidationResult.failure(
                ValidationError.error("field1", "Normal error")
        );

        assertThat(result.hasCriticalErrors()).isFalse();
    }

    @Test
    @DisplayName("builder should accumulate errors")
    void shouldAccumulateErrorsWithBuilder() {
        var result = ValidationResult.builder()
                .addError(ValidationError.error("field1", "Error 1"))
                .addError(ValidationError.error("field2", "Error 2"))
                .addError(ValidationError.warning("field3", "Warning"))
                .build();

        assertThat(result.getErrors()).hasSize(3);
    }

    @Test
    @DisplayName("builder should support conditional errors")
    void shouldSupportConditionalErrors() {
        boolean condition1 = true;
        boolean condition2 = false;

        var result = ValidationResult.builder()
                .addErrorIf(condition1, ValidationError.error("field1", "Error 1"))
                .addErrorIf(condition2, ValidationError.error("field2", "Error 2"))
                .build();

        assertThat(result.getErrors()).hasSize(1);
        assertThat(result.getErrors().get(0).getField()).isEqualTo("field1");
    }

    @Test
    @DisplayName("builder should return success when no errors")
    void shouldReturnSuccessWhenNoErrors() {
        var result = ValidationResult.builder().build();

        assertThat(result.isValid()).isTrue();
        assertThat(result.hasErrors()).isFalse();
    }
}