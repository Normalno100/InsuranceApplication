package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.*;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Проверяет что дата в прошлом (например, дата рождения)
 */
public class DateInPastValidator<T> extends FieldValidationRule<T, LocalDate> {

    public DateInPastValidator(String fieldName, Function<T, LocalDate> fieldExtractor) {
        super(fieldName, fieldExtractor, 110); // Order = 110 (business)
    }

    @Override
    protected ValidationResult validateField(LocalDate fieldValue, ValidationContext context) {
        if (fieldValue == null) {
            return success(); // null проверяется другим валидатором
        }

        LocalDate now = context.getValidationDate();

        if (!fieldValue.isBefore(now)) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    getFieldName(),
                                    "Field " + getFieldName() + " must be in the past!"
                            )
                            .withParameter("field", getFieldName())
                            .withParameter("date", fieldValue)
                            .withParameter("currentDate", now)
            );
        }

        return success();
    }
}