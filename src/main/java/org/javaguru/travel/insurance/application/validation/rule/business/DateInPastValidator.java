package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractFieldValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;

import java.time.LocalDate;
import java.util.function.Function;

/**
 * Проверяет что дата в прошлом (например, дата рождения).
 *
 * РЕФАКТОРИНГ (п. 4.4): Убран ручной null-guard:
 *   БЫЛО:
 *     if (fieldValue == null) {
 *         return success(); // null проверяется другим валидатором
 *     }
 *   СТАЛО: skipIfNull=true в конструкторе — AbstractFieldValidator
 *          автоматически возвращает success() при null.
 */
public class DateInPastValidator<T> extends AbstractFieldValidator<T, LocalDate> {

    public DateInPastValidator(String fieldName, Function<T, LocalDate> fieldExtractor) {
        super(fieldName, fieldExtractor, 110, true); // skipIfNull=true
    }

    @Override
    protected ValidationResult doValidateField(LocalDate fieldValue, ValidationContext context) {
        // При skipIfNull=true этот метод вызывается только с ненулевым значением
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