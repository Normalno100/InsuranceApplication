package org.javaguru.travel.insurance.application.validation.rule.structural;

import org.javaguru.travel.insurance.application.validation.FieldValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;

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