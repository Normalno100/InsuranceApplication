package org.javaguru.travel.insurance.core.validation.field;

import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация поля personFirstName
 * Проверяет, что имя не null и не пустое
 */
@Component
public class PersonFirstNameValidation implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequest request) {
        return (request.getPersonFirstName() == null || request.getPersonFirstName().isEmpty())
                ? Optional.of(new ValidationError("personFirstName", "Must not be empty!"))
                : Optional.empty();
    }
}