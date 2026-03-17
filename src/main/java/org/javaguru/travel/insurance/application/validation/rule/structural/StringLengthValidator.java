package org.javaguru.travel.insurance.application.validation.rule.structural;

import org.javaguru.travel.insurance.application.validation.AbstractFieldValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;

import java.util.function.Function;

/**
 * Проверяет длину строки.
 *
 * РЕФАКТОРИНГ (п. 4.4): Убран ручной null-guard:
 *   БЫЛО:
 *     if (fieldValue == null) {
 *         return success(); // null проверяется другим валидатором
 *     }
 *   СТАЛО: skipIfNull=true в конструкторе AbstractFieldValidator.
 */
public class StringLengthValidator<T> extends AbstractFieldValidator<T, String> {

    private final Integer minLength;
    private final Integer maxLength;

    public StringLengthValidator(String fieldName,
                                 Integer minLength,
                                 Integer maxLength,
                                 Function<T, String> fieldExtractor) {
        super(fieldName, fieldExtractor, 30, true); // skipIfNull=true
        this.minLength = minLength;
        this.maxLength = maxLength;
    }

    @Override
    protected ValidationResult doValidateField(String fieldValue, ValidationContext context) {
        // При skipIfNull=true этот метод вызывается только с ненулевым значением

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