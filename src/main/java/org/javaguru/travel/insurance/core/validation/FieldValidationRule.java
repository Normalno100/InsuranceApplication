package org.javaguru.travel.insurance.core.validation;

import java.util.function.Function;

/**
 * Базовый класс для валидации одного поля объекта
 *
 * @param <T> тип объекта
 * @param <F> тип поля
 */
public abstract class FieldValidationRule<T, F> extends AbstractValidationRule<T> {

    private final String fieldName;
    private final Function<T, F> fieldExtractor;

    protected FieldValidationRule(String fieldName, Function<T, F> fieldExtractor) {
        this(fieldName, fieldExtractor, 100);
    }

    protected FieldValidationRule(String fieldName, Function<T, F> fieldExtractor, int order) {
        super(fieldName + "Validator", order);
        this.fieldName = fieldName;
        this.fieldExtractor = fieldExtractor;
    }

    @Override
    protected ValidationResult doValidate(T target, ValidationContext context) {
        F fieldValue = fieldExtractor.apply(target);
        return validateField(fieldValue, context);
    }

    /**
     * Валидация конкретного поля
     */
    protected abstract ValidationResult validateField(F fieldValue, ValidationContext context);

    /**
     * Получить имя поля
     */
    protected String getFieldName() {
        return fieldName;
    }

    /**
     * Создать ошибку для этого поля
     */
    protected ValidationResult fieldError(String message) {
        return failure(fieldName, message);
    }

    /**
     * Создать критичную ошибку для этого поля
     */
    protected ValidationResult fieldCriticalError(String message) {
        return criticalFailure(fieldName, message);
    }
}