package org.javaguru.travel.insurance.core.validation.field;

import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация поля agreementDateFrom
 * Проверяет, что дата начала поездки не null
 */
@Component
public class AgreementDateFromValidation implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequest request) {
        return (request.getAgreementDateFrom() == null)
                ? Optional.of(new ValidationError("agreementDateFrom", "Must not be empty!"))
                : Optional.empty();
    }
}