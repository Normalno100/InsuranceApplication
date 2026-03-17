package org.javaguru.travel.insurance.application.validation;

import org.javaguru.travel.insurance.application.validation.rule.business.*;
import org.javaguru.travel.insurance.application.validation.rule.reference.*;
import org.javaguru.travel.insurance.application.validation.rule.structural.*;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Главный валидатор для TravelCalculatePremiumRequest.
 *
 * РЕФАКТОРИНГ (п. 4.4): Устранение дублирования null-проверок в валидаторах.
 *
 * ПОДХОД:
 *   1. Валидаторы одного поля (DateInPastValidator, IsoCodeValidator,
 *      StringLengthValidator) наследуются от AbstractFieldValidator
 *      с skipIfNull=true — ручные null-guard убраны из их кода.
 *
 *   2. Валидаторы двух полей (DateRangeValidator, TripDurationValidator,
 *      AgeValidator, AgreementDateFromNotTooFarValidator) оборачиваются
 *      в ConditionalValidator.when(...) прямо здесь — условие проверяет
 *      ненулевость всех нужных полей перед вызовом правила.
 *
 *   3. FutureTripWarningValidator уже работает корректно (skipIfNull-аналог
 *      через явную проверку dateFrom == null → success()); оставлен без изменений.
 *
 * РЕЗУЛЬТАТ: ~10 дублирующихся null-guard'ов удалены из валидаторов.
 *   Логика "пропустить если null" централизована в двух местах:
 *   - AbstractFieldValidator.validateField() — для single-field валидаторов
 *   - ConditionalValidator.when() — для multi-field валидаторов
 */
@Component
public class TravelCalculatePremiumRequestValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;
    private final ReferenceDataPort referenceDataPort;

    public TravelCalculatePremiumRequestValidator(
            CountryRepository countryRepository,
            MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository,
            RiskTypeRepository riskRepository,
            ReferenceDataPort referenceDataPort) {

        this.referenceDataPort = referenceDataPort;

        this.compositeValidator = buildCompositeValidator(
                countryRepository,
                medicalRiskLimitLevelRepository,
                riskRepository,
                referenceDataPort
        );
    }

    public List<ValidationError> validate(TravelCalculatePremiumRequest request) {
        ValidationContext context = new ValidationContext();
        ValidationResult result = compositeValidator.validate(request, context);

        if (result.isValid()) {
            return List.of();
        }

        return result.getErrors();
    }

    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator(
            CountryRepository countryRepository,
            MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository,
            RiskTypeRepository riskRepository,
            ReferenceDataPort referenceDataPort) {

        return CompositeValidator.<TravelCalculatePremiumRequest>builder(
                        "TravelCalculatePremiumRequestValidator"
                )
                // ==========================================
                // LEVEL 1: STRUCTURAL VALIDATION (Order: 10-99)
                // ==========================================

                // personFirstName
                .addRule(new NotNullValidator<>("personFirstName",
                        TravelCalculatePremiumRequest::getPersonFirstName))
                .addRule(new NotBlankValidator<>("personFirstName",
                        TravelCalculatePremiumRequest::getPersonFirstName))
                // StringLengthValidator теперь с skipIfNull=true → null-guard не нужен
                .addRule(new StringLengthValidator<>("personFirstName", 1, 100,
                        TravelCalculatePremiumRequest::getPersonFirstName))

                // personLastName
                .addRule(new NotNullValidator<>("personLastName",
                        TravelCalculatePremiumRequest::getPersonLastName))
                .addRule(new NotBlankValidator<>("personLastName",
                        TravelCalculatePremiumRequest::getPersonLastName))
                .addRule(new StringLengthValidator<>("personLastName", 1, 100,
                        TravelCalculatePremiumRequest::getPersonLastName))

                // personBirthDate
                .addRule(new DateNotNullValidator<>("personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate))

                // agreementDateFrom
                .addRule(new DateNotNullValidator<>("agreementDateFrom",
                        TravelCalculatePremiumRequest::getAgreementDateFrom))

                // agreementDateTo
                .addRule(new DateNotNullValidator<>("agreementDateTo",
                        TravelCalculatePremiumRequest::getAgreementDateTo))

                // countryIsoCode
                .addRule(new NotNullValidator<>("countryIsoCode",
                        TravelCalculatePremiumRequest::getCountryIsoCode))
                .addRule(new NotBlankValidator<>("countryIsoCode",
                        TravelCalculatePremiumRequest::getCountryIsoCode))
                // IsoCodeValidator теперь с skipIfNull=true → null-guard не нужен
                .addRule(new IsoCodeValidator<>("countryIsoCode", 2,
                        TravelCalculatePremiumRequest::getCountryIsoCode))

                // medicalRiskLimitLevel (условная обязательность по режиму расчёта)
                .addRule(new ConditionalMedicalRiskLimitLevelValidator())

                // selectedRisks (опциональное поле)
                .addRule(new CollectionElementsNotBlankValidator<>("selectedRisks",
                        TravelCalculatePremiumRequest::getSelectedRisks))

                // ==========================================
                // LEVEL 2: BUSINESS RULES (Order: 100-199)
                // ==========================================

                // РЕФАКТОРИНГ 4.4: DateInPastValidator теперь с skipIfNull=true —
                // null-guard удалён из кода валидатора.
                .addRule(new DateInPastValidator<>("personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate))         // 110

                // РЕФАКТОРИНГ 4.4: DateRangeValidator обёрнут в ConditionalValidator —
                // активируется только когда обе даты ненулевые.
                // Ручной null-guard "if (dateFrom == null || dateTo == null)" убран из
                // DateRangeValidator — здесь он теперь не нужен.
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null
                                && req.getAgreementDateTo() != null,
                        new DateRangeValidator()                                     // 120
                ))

                // РЕФАКТОРИНГ 4.4: AgeValidator обёрнут в ConditionalValidator —
                // активируется только когда обе даты ненулевые.
                .addRule(ConditionalValidator.when(
                        req -> req.getPersonBirthDate() != null
                                && req.getAgreementDateFrom() != null,
                        new AgeValidator()                                           // 130
                ))

                // РЕФАКТОРИНГ 4.4: TripDurationValidator обёрнут в ConditionalValidator.
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null
                                && req.getAgreementDateTo() != null,
                        new TripDurationValidator()                                  // 140
                ))

                // РЕФАКТОРИНГ 4.4: AgreementDateFromNotTooFarValidator обёрнут
                // в ConditionalValidator — dateFrom != null гарантирован.
                .addRule(ConditionalValidator.when(
                        req -> req.getAgreementDateFrom() != null,
                        new AgreementDateFromNotTooFarValidator()                   // 145
                ))

                // FutureTripWarningValidator — имеет собственный null-guard
                // (if dateFrom == null → success()), оставлен без изменений.
                .addRule(new FutureTripWarningValidator())                           // 150

                .addRule(new MandatoryRisksValidator())                             // 160
                .addRule(new DuplicateRisksValidator())                             // 165

                // ==========================================
                // LEVEL 3: REFERENCE DATA (Order: 200-299)
                // ==========================================

                // РЕФАКТОРИНГ 4.4: CountryExistenceValidator обёрнут в ConditionalValidator —
                // активируется только когда код страны и дата ненулевые.
                .addRule(ConditionalValidator.when(
                        req -> Objects.nonNull(req.getCountryIsoCode())
                                && Objects.nonNull(req.getAgreementDateFrom()),
                        new CountryExistenceValidator(referenceDataPort)           // 210
                ))

                // РЕФАКТОРИНГ 4.4: MedicalRiskLimitLevelExistenceValidator уже содержит
                // проверку useCountryDefaultPremium и null — оставлен без изменений,
                // т.к. логика сложнее простого null-guard.
                .addRule(new MedicalRiskLimitLevelExistenceValidator(referenceDataPort)) // 220

                // РЕФАКТОРИНГ 4.4: RiskTypeExistenceValidator обёрнут в ConditionalValidator —
                // активируется только когда есть выбранные риски и дата.
                .addRule(ConditionalValidator.when(
                        req -> req.getSelectedRisks() != null
                                && !req.getSelectedRisks().isEmpty()
                                && req.getAgreementDateFrom() != null,
                        new RiskTypeExistenceValidator(referenceDataPort)          // 230
                ))

                .addRule(new RiskTypeNotMandatoryValidator(referenceDataPort))     // 240
                .addRule(new CurrencySupportValidator())                           // 250

                .stopOnCriticalError(true)
                .build();
    }
}