package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.function.Function;

/**
 * Проверяет что строковое поле не пустое (после trim)
 */
public class NotBlankValidator<T> extends FieldValidationRule<T, String> {

    public NotBlankValidator(String fieldName, Function<T, String> fieldExtractor) {
        super(fieldName, fieldExtractor, 20); // Order = 20
    }

    @Override
    protected ValidationResult validateField(String fieldValue, ValidationContext context) {
        if (fieldValue == null || fieldValue.trim().isEmpty()) {
            return ValidationResult.failure(
                    ValidationError.error(
                            getFieldName(),
                            "Field " + getFieldName() + " must not be empty!"
                    ).withParameter("field", getFieldName())
            );
        }
        return success();
    }
}