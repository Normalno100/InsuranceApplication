package org.javaguru.travel.insurance.core.validation.field;

import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация поля agreementDateTo
 * Проверяет:
 * 1. Что дата окончания поездки не null
 * 2. Что дата окончания не раньше даты начала (если dateFrom заполнен)
 */
@Component
public class AgreementDateToValidation implements ValidationRule {

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequest request) {
        // Проверка на null
        if (request.getAgreementDateTo() == null) {
            return Optional.of(new ValidationError("agreementDateTo", "Must not be empty!"));
        }

        // Проверка что dateTo >= dateFrom (только если dateFrom не null)
        if (request.getAgreementDateFrom() != null
                && request.getAgreementDateTo().isBefore(request.getAgreementDateFrom())) {
            return Optional.of(new ValidationError("agreementDateTo",
                    "Must be after agreementDateFrom!"));
        }

        return Optional.empty();
    }
}