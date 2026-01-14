package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

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