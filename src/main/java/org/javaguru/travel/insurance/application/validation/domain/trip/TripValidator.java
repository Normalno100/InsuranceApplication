package org.javaguru.travel.insurance.application.validation.domain.trip;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.CompositeValidator;
import org.javaguru.travel.insurance.application.validation.ConditionalValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.rule.business.AgreementDateFromNotTooFarValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.DateRangeValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.FutureTripWarningValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.TripDurationValidator;
import org.javaguru.travel.insurance.application.validation.rule.reference.CountryExistenceValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.DateNotNullValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.IsoCodeValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotBlankValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.NotNullValidator;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Доменный валидатор для параметров поездки.
 *
 * task_136: Часть рефакторинга по доменному принципу.
 *
 * Проверяемые поля:
 *   - agreementDateFrom (NotNull, FutureTripWarning, NotTooFar)
 *   - agreementDateTo   (NotNull, DateRange, TripDuration)
 *   - countryIsoCode    (NotNull, NotBlank, IsoCode формат, CountryExistence)
 *
 * Используется как Spring-компонент, инжектируется в
 * TravelCalculatePremiumRequestValidator (оркестратор).
 */
@Component
public class TripValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public TripValidator(ReferenceDataPort referenceDataPort) {
        this.compositeValidator = buildCompositeValidator(referenceDataPort);
    }

    /**
     * Валидирует параметры поездки.
     *
     * @param request запрос на расчёт премии
     * @param context контекст валидации (может сохранить tripDuration)
     * @return список ошибок, пустой если валидация прошла
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        ValidationResult result = compositeValidator.validate(request, context);
        return result.isValid() ? List.of() : result.getErrors();
    }

    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator(
            ReferenceDataPort referenceDataPort) {

        return CompositeValidator.<TravelCalculatePremiumRequest>builder("TripValidator")
                // ── agreementDateFrom ──────────────────────────────────
                .addRule(new DateNotNullValidator<>("agreementDateFrom",
                        TravelCalculatePremiumRequest::getAgreementDateFrom))
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null,
                        new AgreementDateFromNotTooFarValidator()
                ))
                // FutureTripWarningValidator содержит собственный null-guard
                .addRule(new FutureTripWarningValidator())

                // ── agreementDateTo ────────────────────────────────────
                .addRule(new DateNotNullValidator<>("agreementDateTo",
                        TravelCalculatePremiumRequest::getAgreementDateTo))
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null
                                && req.getAgreementDateTo() != null,
                        new DateRangeValidator()
                ))
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null
                                && req.getAgreementDateTo() != null,
                        new TripDurationValidator()
                ))

                // ── countryIsoCode ─────────────────────────────────────
                .addRule(new NotNullValidator<>("countryIsoCode",
                        TravelCalculatePremiumRequest::getCountryIsoCode))
                .addRule(new NotBlankValidator<>("countryIsoCode",
                        TravelCalculatePremiumRequest::getCountryIsoCode))
                .addRule(new IsoCodeValidator<>("countryIsoCode", 2,
                        TravelCalculatePremiumRequest::getCountryIsoCode))
                .addRule(ConditionalValidator.when(
                        req -> Objects.nonNull(req.getCountryIsoCode())
                                && Objects.nonNull(req.getAgreementDateFrom()),
                        new CountryExistenceValidator(referenceDataPort)
                ))

                .stopOnCriticalError(true)
                .build();
    }
}