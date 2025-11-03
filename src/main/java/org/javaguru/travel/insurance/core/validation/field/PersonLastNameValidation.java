package org.javaguru.travel.insurance.core.validation.field;

import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация поля personLastName
 * Проверяет, что фамилия не null и не пустая
 */
@Component
public class PersonLastNameValidation implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequest request) {
        return (request.getPersonLastName() == null || request.getPersonLastName().isEmpty())
                ? Optional.of(new ValidationError("personLastName", "Must not be empty!"))
                : Optional.empty();
    }
}