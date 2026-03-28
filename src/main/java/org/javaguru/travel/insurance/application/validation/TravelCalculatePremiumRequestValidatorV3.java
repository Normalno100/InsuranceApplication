package org.javaguru.travel.insurance.application.validation;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.v3.InsuredPerson;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.application.validation.domain.commercial.CommercialValidator;
import org.javaguru.travel.insurance.application.validation.domain.coverage.CoverageValidator;
import org.javaguru.travel.insurance.application.validation.domain.person.PersonValidator;
import org.javaguru.travel.insurance.application.validation.domain.risks.SelectedRisksValidator;
import org.javaguru.travel.insurance.application.validation.domain.trip.TripValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Валидатор для V3 запроса с поддержкой нескольких застрахованных персон.
 *
 * task_135: Создан для нового REST-эндпоинта v3.
 *
 * ОТЛИЧИЕ ОТ V2:
 *   V2: валидирует одну персону из плоских полей запроса.
 *   V3: валидирует список persons[], для каждой персоны адресуя ошибки с индексом:
 *       persons[0].personBirthDate — Must not be empty
 *
 * ПОРЯДОК ВАЛИДАЦИИ:
 *   1. Проверка наличия списка persons (не null, не пустой)
 *   2. Для каждой персоны — PersonValidator (адаптированный)
 *   3. TripValidator — даты, страна (общие параметры)
 *   4. CoverageValidator — уровень покрытия
 *   5. SelectedRisksValidator — риски
 *   6. CommercialValidator — валюта
 *
 * СТОП ПРИ КРИТИЧНЫХ ОШИБКАХ:
 *   Если любой доменный валидатор вернул CRITICAL-ошибку,
 *   дальнейшая оркестрация прекращается.
 */
@Slf4j
@Component
public class TravelCalculatePremiumRequestValidatorV3 {

    private final PersonValidator personValidator;
    private final TripValidator tripValidator;
    private final CoverageValidator coverageValidator;
    private final SelectedRisksValidator selectedRisksValidator;
    private final CommercialValidator commercialValidator;

    @Autowired
    public TravelCalculatePremiumRequestValidatorV3(
            PersonValidator personValidator,
            TripValidator tripValidator,
            CoverageValidator coverageValidator,
            SelectedRisksValidator selectedRisksValidator,
            CommercialValidator commercialValidator) {

        this.personValidator = personValidator;
        this.tripValidator = tripValidator;
        this.coverageValidator = coverageValidator;
        this.selectedRisksValidator = selectedRisksValidator;
        this.commercialValidator = commercialValidator;
    }

    /**
     * Валидирует V3 запрос с несколькими застрахованными персонами.
     *
     * @param request V3 запрос
     * @return список ошибок; пустой если ошибок нет
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequestV3 request) {
        ValidationContext context = new ValidationContext();
        List<ValidationError> allErrors = new ArrayList<>();

        // ── 1. Проверка наличия списка персон ─────────────────────────────
        if (request.getPersons() == null || request.getPersons().isEmpty()) {
            allErrors.add(ValidationError.critical(
                    "persons",
                    "Field persons must not be null or empty!"
            ));
            return allErrors;
        }

        // ── 2. Валидация каждой персоны (PersonValidator) ─────────────────
        for (int i = 0; i < request.getPersons().size(); i++) {
            InsuredPerson person = request.getPersons().get(i);
            TravelCalculatePremiumRequest personRequest = adaptPersonToRequest(person, request);

            List<ValidationError> personErrors = personValidator.validate(personRequest, context);

            // Переименовываем поля с индексом персоны: personFirstName → persons[0].personFirstName
            for (ValidationError error : personErrors) {
                allErrors.add(prefixWithIndex(error, i));
            }

            if (hasCriticalErrors(personErrors)) {
                log.debug("PersonValidator returned critical errors for person[{}], stopping validation", i);
                return allErrors;
            }
        }

        // ── 3. TripValidator ──────────────────────────────────────────────
        TravelCalculatePremiumRequest tripRequest = adaptTripToRequest(request);
        List<ValidationError> tripErrors = tripValidator.validate(tripRequest, context);
        allErrors.addAll(tripErrors);

        if (hasCriticalErrors(tripErrors)) {
            log.debug("TripValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 4. CoverageValidator ──────────────────────────────────────────
        List<ValidationError> coverageErrors = coverageValidator.validate(tripRequest, context);
        allErrors.addAll(coverageErrors);

        if (hasCriticalErrors(coverageErrors)) {
            log.debug("CoverageValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 5. SelectedRisksValidator ─────────────────────────────────────
        List<ValidationError> risksErrors = selectedRisksValidator.validate(tripRequest, context);
        allErrors.addAll(risksErrors);

        if (hasCriticalErrors(risksErrors)) {
            log.debug("SelectedRisksValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 6. CommercialValidator ────────────────────────────────────────
        List<ValidationError> commercialErrors = commercialValidator.validate(tripRequest, context);
        allErrors.addAll(commercialErrors);

        return allErrors;
    }

    /**
     * Адаптирует данные персоны + общие параметры запроса в V2 TravelCalculatePremiumRequest
     * для использования доменными валидаторами.
     */
    private TravelCalculatePremiumRequest adaptPersonToRequest(
            InsuredPerson person,
            TravelCalculatePremiumRequestV3 request) {

        return TravelCalculatePremiumRequest.builder()
                .personFirstName(person.getPersonFirstName())
                .personLastName(person.getPersonLastName())
                .personBirthDate(person.getPersonBirthDate())
                .applyAgeCoefficient(person.getApplyAgeCoefficient())
                // Дата начала нужна для проверки возраста
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                // Остальные поля не нужны для PersonValidator
                .countryIsoCode("ES")           // заглушка — не валидируется в PersonValidator
                .medicalRiskLimitLevel("50000") // заглушка
                .build();
    }

    /**
     * Адаптирует общие параметры V3 запроса в V2 TravelCalculatePremiumRequest
     * для валидации Trip/Coverage/Risks/Commercial.
     */
    private TravelCalculatePremiumRequest adaptTripToRequest(TravelCalculatePremiumRequestV3 request) {
        return TravelCalculatePremiumRequest.builder()
                // Персональные данные — берём первую персону для контекста
                .personFirstName(request.getPersons() != null && !request.getPersons().isEmpty()
                        ? request.getPersons().get(0).getPersonFirstName() : null)
                .personLastName(request.getPersons() != null && !request.getPersons().isEmpty()
                        ? request.getPersons().get(0).getPersonLastName() : null)
                .personBirthDate(request.getPersons() != null && !request.getPersons().isEmpty()
                        ? request.getPersons().get(0).getPersonBirthDate() : null)
                // Параметры поездки
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                .countryIsoCode(request.getCountryIsoCode())
                // Покрытие
                .medicalRiskLimitLevel(request.getMedicalRiskLimitLevel())
                .useCountryDefaultPremium(request.getUseCountryDefaultPremium())
                // Риски и коммерческие параметры
                .selectedRisks(request.getSelectedRisks())
                .currency(request.getCurrency())
                .promoCode(request.getPromoCode())
                .personsCount(request.getPersonsCount())
                .isCorporate(request.getIsCorporate())
                .build();
    }

    /**
     * Создаёт новую ошибку с полем, адресованным с индексом персоны.
     * Например: "personFirstName" → "persons[0].personFirstName"
     */
    private ValidationError prefixWithIndex(ValidationError error, int personIndex) {
        String prefixedField = "persons[" + personIndex + "]." + error.getField();
        return new ValidationError(
                prefixedField,
                error.getMessage(),
                error.getErrorCode(),
                error.getSeverity()
        );
    }

    /**
     * Проверяет наличие CRITICAL-ошибок в списке.
     */
    private boolean hasCriticalErrors(List<ValidationError> errors) {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == ValidationError.Severity.CRITICAL);
    }
}