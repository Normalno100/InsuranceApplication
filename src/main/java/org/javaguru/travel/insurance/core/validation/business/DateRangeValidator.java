package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;

/**
 * Проверяет что agreementDateTo >= agreementDateFrom
 */
public class DateRangeValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    public DateRangeValidator() {
        super("DateRangeValidator", 120); // Order = 120
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate dateFrom = request.getAgreementDateFrom();
        LocalDate dateTo = request.getAgreementDateTo();

        // Если какая-то из дат null, пропускаем проверку
        // (null проверяется другими валидаторами)
        if (dateFrom == null || dateTo == null) {
            return success();
        }

        if (dateTo.isBefore(dateFrom)) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    "agreementDateTo",
                                    "agreementDateTo must be greater than or equal to agreementDateFrom!"
                            )
                            .withParameter("dateFrom", dateFrom)
                            .withParameter("dateTo", dateTo)
            );
        }

        return success();
    }
}