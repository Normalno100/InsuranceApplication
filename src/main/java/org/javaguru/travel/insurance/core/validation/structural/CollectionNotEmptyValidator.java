package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.Collection;
import java.util.function.Function;

/**
 * Проверяет что коллекция не пустая (если она присутствует)
 */
public class CollectionNotEmptyValidator<T> extends FieldValidationRule<T, Collection<?>> {

    private final boolean mandatory;

    public CollectionNotEmptyValidator(String fieldName,
                                       Function<T, Collection<?>> fieldExtractor,
                                       boolean mandatory) {
        super(fieldName, fieldExtractor, 25);
        this.mandatory = mandatory;
    }

    @Override
    protected ValidationResult validateField(Collection<?> fieldValue, ValidationContext context) {
        if (fieldValue == null) {
            if (mandatory) {
                return ValidationResult.failure(
                        ValidationError.error(
                                getFieldName(),
                                "Field " + getFieldName() + " must not be null!"
                        )
                );
            }
            return success();
        }

        if (fieldValue.isEmpty()) {
            return ValidationResult.failure(
                    ValidationError.error(
                            getFieldName(),
                            "Field " + getFieldName() + " must not be empty!"
                    )
            );
        }

        return success();
    }
}