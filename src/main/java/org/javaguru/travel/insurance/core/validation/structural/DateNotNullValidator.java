package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Специализированный валидатор для дат (не null)
 */
public class DateNotNullValidator<T> extends FieldValidationRule<T, LocalDate> {

    public DateNotNullValidator(String fieldName, Function<T, LocalDate> fieldExtractor) {
        super(fieldName, fieldExtractor, 10);
    }

    @Override
    protected ValidationResult validateField(LocalDate fieldValue, ValidationContext context) {
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
        return true;
    }
}