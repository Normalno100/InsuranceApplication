package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Проверяет что длительность поездки не превышает 365 дней.
 *
 * РЕФАКТОРИНГ (п. 4.4): Убраны ручные null-гварды и избыточная проверка dateTo < dateFrom.
 *   БЫЛО:
 *     if (dateFrom == null || dateTo == null || dateTo.isBefore(dateFrom)) {
 *         return success();
 *     }
 *   СТАЛО: ConditionalValidator.when() в TravelCalculatePremiumRequestValidator
 *   гарантирует, что этот валидатор вызывается только когда:
 *   - agreementDateFrom != null
 *   - agreementDateTo != null
 *   Проверка dateTo < dateFrom выполняется DateRangeValidator'ом (порядок 120),
 *   который запускается перед TripDurationValidator'ом (порядок 140).
 *
 *   Оставляем минимальную защиту null-guard'ом для безопасного прямого вызова.
 */
public class TripDurationValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final long MAX_TRIP_DURATION_DAYS = 365;

    public TripDurationValidator() {
        super("TripDurationValidator", 140);
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate dateFrom = request.getAgreementDateFrom();
        LocalDate dateTo = request.getAgreementDateTo();

        // Минимальная защита для прямого вызова вне ConditionalValidator.
        // При нормальном использовании через TravelCalculatePremiumRequestValidator
        // ConditionalValidator гарантирует ненулевые даты.
        if (dateFrom == null || dateTo == null) {
            return success();
        }

        // dateTo < dateFrom обрабатывается DateRangeValidator'ом (order=120),
        // поэтому здесь не нужна проверка dateTo.isBefore(dateFrom).
        long tripDuration = ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
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