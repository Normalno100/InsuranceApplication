package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;

/**
 * Проверяет что agreementDateTo >= agreementDateFrom.
 *
 * РЕФАКТОРИНГ (п. 4.4): Убраны ручные null-гварды на обоих полях.
 *   БЫЛО:
 *     if (dateFrom == null || dateTo == null) {
 *         return success();
 *     }
 *   СТАЛО: ConditionalValidator.when() — валидатор активируется
 *   только при ненулевых значениях обоих полей.
 *
 * Так как DateRangeValidator работает с двумя полями запроса (не с одним
 * полем через fieldExtractor), он не наследуется от AbstractFieldValidator.
 * Вместо этого CompositeValidator оборачивает его в ConditionalValidator:
 *
 *   .addRule(ConditionalValidator.when(
 *       req -> req.getAgreementDateFrom() != null && req.getAgreementDateTo() != null,
 *       new DateRangeValidator()
 *   ))
 *
 * Это устраняет null-guard внутри правила — валидатор видит только
 * запросы с обоими заполненными датами.
 *
 * @see org.javaguru.travel.insurance.application.validation.TravelCalculatePremiumRequestValidator
 */
public class DateRangeValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    public DateRangeValidator() {
        super("DateRangeValidator", 120);
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        // Null-guard удалён: ConditionalValidator в TravelCalculatePremiumRequestValidator
        // гарантирует что этот метод вызывается только когда обе даты не null.
        // Для безопасности оставляем минимальную защиту на случай прямого вызова:
        LocalDate dateFrom = request.getAgreementDateFrom();
        LocalDate dateTo = request.getAgreementDateTo();

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