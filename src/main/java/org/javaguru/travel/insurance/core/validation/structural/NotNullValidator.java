package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.function.Function;

/**
 * Проверяет что поле не null
 */
public class NotNullValidator<T> extends FieldValidationRule<T, Object> {

    public NotNullValidator(String fieldName, Function<T, Object> fieldExtractor) {
        super(fieldName, fieldExtractor, 10); // Order = 10
    }

    @Override
    protected ValidationResult validateField(Object fieldValue, ValidationContext context) {
        if (fieldValue == null) {
            return ValidationResult.failure(
                    ValidationError.critical(
                            getFieldName(),
                            "Field " + getFieldName() + " must not be null!"
                    ).withParameter("field", getFieldName())
            );
        }
        return success();
    }

    @Override
    public boolean isCritical() {
        return true; // Null поля - критичная ошибка
    }
}