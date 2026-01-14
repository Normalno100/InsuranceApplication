package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.validation.*;

/**
 * Базовый класс для справочных валидаторов с общими методами
 */
public abstract class AbstractReferenceDataValidator<T> extends AbstractValidationRule<T> {

    protected AbstractReferenceDataValidator(String ruleName) {
        super(ruleName, 200); // Order = 200 по умолчанию для reference валидаторов
    }

    protected AbstractReferenceDataValidator(String ruleName, int order) {
        super(ruleName, order);
    }

    /**
     * Создать ошибку "не найдено"
     */
    protected ValidationResult notFoundError(String field, String identifier, String type) {
        return ValidationResult.failure(
                ValidationError.error(
                                field,
                                String.format("%s with identifier '%s' not found!", type, identifier)
                        )
                        .withParameter("field", field)
                        .withParameter("identifier", identifier)
                        .withParameter("type", type)
        );
    }

    /**
     * Создать ошибку "не активно"
     */
    protected ValidationResult notActiveError(String field, String identifier, String type) {
        return ValidationResult.failure(
                ValidationError.error(
                                field,
                                String.format("%s with identifier '%s' is not active!", type, identifier)
                        )
                        .withParameter("field", field)
                        .withParameter("identifier", identifier)
                        .withParameter("type", type)
        );
    }
}