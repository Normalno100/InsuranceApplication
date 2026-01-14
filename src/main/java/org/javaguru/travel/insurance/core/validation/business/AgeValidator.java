package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.time.Period;

/**
 * Проверяет что возраст в допустимых пределах (0-80 лет)
 */
public class AgeValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 80;

    public AgeValidator() {
        super("AgeValidator", 130); // Order = 130
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        LocalDate birthDate = request.getPersonBirthDate();
        LocalDate agreementDateFrom = request.getAgreementDateFrom();

        // Если даты null, пропускаем
        if (birthDate == null || agreementDateFrom == null) {
            return success();
        }

        int age = Period.between(birthDate, agreementDateFrom).getYears();

        // Сохраняем возраст в контекст для других валидаторов
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