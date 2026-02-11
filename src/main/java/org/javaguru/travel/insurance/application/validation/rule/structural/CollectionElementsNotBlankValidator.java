package org.javaguru.travel.insurance.application.validation.rule.structural;

import org.javaguru.travel.insurance.application.validation.FieldValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;

import java.util.List;
import java.util.function.Function;

/**
 * Проверяет что элементы коллекции не пустые
 * ОБНОВЛЕНО: теперь ИГНОРИРУЕТ пустые и null элементы (по требованию task_95)
 */
public class CollectionElementsNotBlankValidator<T> extends FieldValidationRule<T, List<String>> {

    public CollectionElementsNotBlankValidator(String fieldName,
                                               Function<T, List<String>> fieldExtractor) {
        super(fieldName, fieldExtractor, 35);
    }

    @Override
    protected ValidationResult validateField(List<String> fieldValue, ValidationContext context) {
        if (fieldValue == null || fieldValue.isEmpty()) {
            return success(); // Пустота проверяется другим валидатором
        }

        // ✅ ОБНОВЛЕНИЕ: больше НЕ проверяем пустые элементы
        // Они игнорируются согласно требованиям task_95
        // Старая логика закомментирована для истории:

        /*
        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (int i = 0; i < fieldValue.size(); i++) {
            String element = fieldValue.get(i);
            if (element == null || element.trim().isEmpty()) {
                resultBuilder.addError(
                        ValidationError.error(
                                        getFieldName() + "[" + i + "]",
                                        String.format("Element at index %d in %s must not be empty!",
                                                i, getFieldName())
                                )
                                .withParameter("field", getFieldName())
                                .withParameter("index", i)
                );
            }
        }

        return resultBuilder.build();
        */

        // Теперь всегда успех - пустые элементы игнорируются
        return success();
    }
}