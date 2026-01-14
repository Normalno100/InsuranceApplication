package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.function.Function;

/**
 * Проверяет длину строки
 */
public class StringLengthValidator<T> extends FieldValidationRule<T, String> {

    private final Integer minLength;
    private final Integer maxLength;

    public StringLengthValidator(String fieldName,
                                 Integer minLength,
                                 Integer maxLength,
                                 Function<T, String> fieldExtractor) {
        super(fieldName, fieldExtractor, 30);
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected ValidationResult validateField(String fieldValue, ValidationContext context) {
        if (fieldValue == null) {
            return success(); // null проверяется другим валидатором
        }

        int length = fieldValue.length();

        if (minLength != null && length < minLength) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    getFieldName(),
                                    String.format("Field %s must be at least %d characters long!",
                                            getFieldName(), minLength)
                            )
                            .withParameter("field", getFieldName())
                            .withParameter("minLength", minLength)
                            .withParameter("actualLength", length)
            );
        }

        if (maxLength != null && length > maxLength) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    getFieldName(),
                                    String.format("Field %s must be at most %d characters long!",
                                            getFieldName(), maxLength)
                            )
                            .withParameter("field", getFieldName())
                            .withParameter("maxLength", maxLength)
                            .withParameter("actualLength", length)
            );
        }

        return success();
    }
}