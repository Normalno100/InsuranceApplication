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
 * Проверяет что длительность поездки не превышает 365 дней
 */
public class TripDurationValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final long MAX_TRIP_DURATION_DAYS = 365;

    public TripDurationValidator() {
        super("TripDurationValidator", 140); // Order = 140
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate dateFrom = request.getAgreementDateFrom();
        LocalDate dateTo = request.getAgreementDateTo();

        // Если даты null или некорректны, пропускаем
        if (dateFrom == null || dateTo == null || dateTo.isBefore(dateFrom)) {
            return success();
        }

        long tripDuration = ChronoUnit.DAYS.between(dateFrom, dateTo) + 1; // включительно

        // Сохраняем длительность в контекст
        context.setAttribute("tripDuration", tripDuration);

        if (tripDuration > MAX_TRIP_DURATION_DAYS) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    "agreementDateTo",
                                    String.format("Trip duration must not exceed %d days!",
                                            MAX_TRIP_DURATION_DAYS)
                            )
                            .withParameter("tripDuration", tripDuration)
                            .withParameter("maxDuration", MAX_TRIP_DURATION_DAYS)
                            .withParameter("dateFrom", dateFrom)
                            .withParameter("dateTo", dateTo)
            );
        }

        return success();
    }
}