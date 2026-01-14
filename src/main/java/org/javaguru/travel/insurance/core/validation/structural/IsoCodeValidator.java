package org.javaguru.travel.insurance.core.validation.structural;

import org.javaguru.travel.insurance.core.validation.*;

import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Проверяет формат ISO кода (например, код страны)
 */
public class IsoCodeValidator<T> extends FieldValidationRule<T, String> {

    private final int expectedLength;
    private static final Pattern ALPHA_PATTERN = Pattern.compile("^[A-Z]+$");

    public IsoCodeValidator(String fieldName,
                            int expectedLength,
                            Function<T, String> fieldExtractor) {
        super(fieldName, fieldExtractor, 40);
        this.expectedLength = expectedLength;
    }

    @Override
    protected ValidationResult validateField(String fieldValue, ValidationContext context) {
        if (fieldValue == null) {
            return success(); // null проверяется другим валидатором
        }

        if (fieldValue.length() != expectedLength) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    getFieldName(),
                                    String.format("Field %s must be exactly %d characters long!",
                                            getFieldName(), expectedLength)
                            )
                            .withParameter("field", getFieldName())
                            .withParameter("expectedLength", expectedLength)
                            .withParameter("actualLength", fieldValue.length())
            );
        }

        if (!ALPHA_PATTERN.matcher(fieldValue).matches()) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    getFieldName(),
                                    String.format("Field %s must contain only uppercase letters!",
                                            getFieldName())
                            )
                            .withParameter("field", getFieldName())
            );
        }

        return success();
    }
}