package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.time.Period;

/**
 * Проверяет что возраст в допустимых пределах (0-80 лет).
 *
 * РЕФАКТОРИНГ (п. 4.4): Убраны ручные null-гварды.
 *   БЫЛО:
 *     if (birthDate == null || agreementDateFrom == null) {
 *         return success();
 *     }
 *   СТАЛО: ConditionalValidator.when() в TravelCalculatePremiumRequestValidator
 *   гарантирует вызов только при ненулевых датах:
 *
 *     .addRule(ConditionalValidator.when(
 *         req -> req.getPersonBirthDate() != null && req.getAgreementDateFrom() != null,
 *         new AgeValidator()
 *     ))
 *
 * Для безопасности прямого вызова оставляем минимальную защиту внутри.
 */
public class AgeValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 80;

    public AgeValidator() {
        super("AgeValidator", 130);
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate birthDate = request.getPersonBirthDate();
        LocalDate agreementDateFrom = request.getAgreementDateFrom();

        // Минимальная защита для прямого вызова вне CompositeValidator.
        // При использовании через ConditionalValidator этот guard не срабатывает.
        if (birthDate == null || agreementDateFrom == null) {
            return success();
        }

        int age = Period.between(birthDate, agreementDateFrom).getYears();
        context.setAttribute("personAge", age);

        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        if (age < MIN_AGE) {
            resultBuilder.addError(
                    ValidationError.error(
                                    "personBirthDate",
                                    String.format("Person age must be at least %d years!", MIN_AGE)
                            )
                            .withParameter("age", age)
                            .withParameter("minAge", MIN_AGE)
            );
        }

        if (age > MAX_AGE) {
            resultBuilder.addError(
                    ValidationError.error(
                                    "personBirthDate",
                                    String.format("Person age must be at most %d years!", MAX_AGE)
                            )
                            .withParameter("age", age)
                            .withParameter("maxAge", MAX_AGE)
            );
        }

        return resultBuilder.build();
    }
}