package org.javaguru.travel.insurance.application.validation.domain.person;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.CompositeValidator;
import org.javaguru.travel.insurance.application.validation.ConditionalValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.rule.business.AgeValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.DateInPastValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.DateNotNullValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotBlankValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotNullValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.StringLengthValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Доменный валидатор для персональных данных застрахованного.
 *
 * task_136: Часть рефакторинга по доменному принципу.
 *
 * Проверяемые поля:
 *   - personFirstName (NotNull, NotBlank, Length 1-100)
 *   - personLastName  (NotNull, NotBlank, Length 1-100)
 *   - personBirthDate (NotNull, DateInPast, Age 0-80 лет)
 *
 * Используется как Spring-компонент, инжектируется в
 * TravelCalculatePremiumRequestValidator (оркестратор).
 */
@Component
public class PersonValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public PersonValidator() {
        this.compositeValidator = buildCompositeValidator();
    }

    /**
     * Валидирует персональные данные запроса.
     *
     * @param request запрос на расчёт премии
     * @param context контекст валидации (может сохранить personAge)
     * @return список ошибок, пустой если валидация прошла
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        ValidationResult result = compositeValidator.validate(request, context);
        return result.isValid() ? List.of() : result.getErrors();
    }

    /**
     * Проверяет наличие критических ошибок персональных данных.
     * Используется оркестратором для принятия решения о продолжении валидации.
     */
    public boolean hasCriticalErrors(List<ValidationError> errors) {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == ValidationError.Severity.CRITICAL);
    }

    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator() {
        return CompositeValidator.<TravelCalculatePremiumRequest>builder("PersonValidator")
                // ── personFirstName ────────────────────────────────────
                .addRule(new NotNullValidator<>("personFirstName",
                        TravelCalculatePremiumRequest::getPersonFirstName))
                .addRule(new NotBlankValidator<>("personFirstName",
                        TravelCalculatePremiumRequest::getPersonFirstName))
                .addRule(new StringLengthValidator<>("personFirstName", 1, 100,
                        TravelCalculatePremiumRequest::getPersonFirstName))

                // ── personLastName ─────────────────────────────────────
                .addRule(new NotNullValidator<>("personLastName",
                        TravelCalculatePremiumRequest::getPersonLastName))
                .addRule(new NotBlankValidator<>("personLastName",
                        TravelCalculatePremiumRequest::getPersonLastName))
                .addRule(new StringLengthValidator<>("personLastName", 1, 100,
                        TravelCalculatePremiumRequest::getPersonLastName))

                // ── personBirthDate ────────────────────────────────────
                .addRule(new DateNotNullValidator<>("personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate))
                .addRule(new DateInPastValidator<>("personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate))
                // AgeValidator требует оба поля ненулевыми
                .addRule(ConditionalValidator.when(
                        req -> req.getPersonBirthDate() != null
                                && req.getAgreementDateFrom() != null,
                        new AgeValidator()
                ))

                .stopOnCriticalError(true)
                .build();
    }
}