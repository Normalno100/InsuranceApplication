package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.List;
import java.util.function.Function;

/**
 * Проверяет что все элементы коллекции не пустые
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
    }
}