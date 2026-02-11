package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;

/**
 * Выдаёт предупреждение если поездка начинается в прошлом
 * (не блокирует, но информирует)
 */
public class FutureTripWarningValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    public FutureTripWarningValidator() {
        super("FutureTripWarningValidator", 150); // Order = 150
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate dateFrom = request.getAgreementDateFrom();

        if (dateFrom == null) {
            return success();
        }

        LocalDate now = context.getValidationDate();

        if (dateFrom.isBefore(now)) {
            return ValidationResult.failure(
                    ValidationError.warning(
                                    "agreementDateFrom",
                                    "Trip start date is in the past. Are you sure this is correct?"
                            )
                            .withParameter("dateFrom", dateFrom)
                            .withParameter("currentDate", now)
            );
        }

        return success();
    }
}