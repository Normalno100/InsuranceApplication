package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Проверяет что agreementDateFrom не слишком далеко в будущем
 * (например, не более 1 года)
 */
public class AgreementDateFromNotTooFarValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final long MAX_DAYS_IN_FUTURE = 365;

    public AgreementDateFromNotTooFarValidator() {
        super("AgreementDateFromNotTooFarValidator", 145);
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate dateFrom = request.getAgreementDateFrom();

        if (dateFrom == null) {
            return success();
        }

        LocalDate now = context.getValidationDate();
        long daysInFuture = ChronoUnit.DAYS.between(now, dateFrom);

        if (daysInFuture > MAX_DAYS_IN_FUTURE) {
            return ValidationResult.failure(
                    ValidationError.warning(
                                    "agreementDateFrom",
                                    String.format(
                                            "Agreement start date is more than %d days in the future. " +
                                                    "Please verify the date is correct.",
                                            MAX_DAYS_IN_FUTURE
                                    )
                            )
                            .withParameter("dateFrom", dateFrom)
                            .withParameter("currentDate", now)
                            .withParameter("daysInFuture", daysInFuture)
                            .withParameter("maxDaysInFuture", MAX_DAYS_IN_FUTURE)
            );
        }

        return success();
    }
}