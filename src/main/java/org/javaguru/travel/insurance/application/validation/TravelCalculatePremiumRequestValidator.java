package org.javaguru.travel.insurance.application.validation;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.domain.commercial.CommercialValidator;
import org.javaguru.travel.insurance.application.validation.domain.coverage.CoverageValidator;
import org.javaguru.travel.insurance.application.validation.domain.person.PersonValidator;
import org.javaguru.travel.insurance.application.validation.domain.risks.SelectedRisksValidator;
import org.javaguru.travel.insurance.application.validation.domain.trip.TripValidator;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.RiskTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Оркестратор валидации запроса TravelCalculatePremiumRequest.
 *
 * task_136: Рефакторинг — разделение валидаций по доменному принципу.
 *
 * БЫЛО:
 *   Монолитный CompositeValidator с 20+ правилами в одном классе.
 *
 * СТАЛО:
 *   Оркестратор делегирует доменным валидаторам в фиксированном порядке:
 *
 *     1. PersonValidator       — personFirstName, personLastName, personBirthDate, age
 *     2. TripValidator         — agreementDateFrom/To, countryIsoCode, CountryExistence
 *     3. CoverageValidator     — medicalRiskLimitLevel, режим COUNTRY_DEFAULT
 *     4. SelectedRisksValidator— selectedRisks, дубликаты, обязательные риски
 *     5. CommercialValidator   — currency
 *
 * ГАРАНТИЯ ОБРАТНОЙ СОВМЕСТИМОСТИ:
 *   Внешний API (метод validate()) не изменился.
 *   Все существующие тесты проходят без изменений.
 *
 * СТОП ПРИ КРИТИЧНЫХ ОШИБКАХ:
 *   Если любой доменный валидатор вернул CRITICAL-ошибку,
 *   дальнейшая оркестрация прекращается.
 *
 * КОНСТРУКТОРЫ:
 *   - @Autowired — для Spring: инжектирует доменные валидаторы как бины.
 *   - Без @Autowired — для тестов, создающих валидатор напрямую с репозиториями.
 */
@Slf4j
@Component
public class TravelCalculatePremiumRequestValidator {

    private final PersonValidator personValidator;
    private final TripValidator tripValidator;
    private final CoverageValidator coverageValidator;
    private final SelectedRisksValidator selectedRisksValidator;
    private final CommercialValidator commercialValidator;

    /**
     * Основной конструктор для Spring.
     * @Autowired явно указывает Spring использовать именно этот конструктор,
     * а не конструктор совместимости с тестами.
     */
    @Autowired
    public TravelCalculatePremiumRequestValidator(
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
     * Конструктор для обратной совместимости с тестами, которые создают
     * валидатор напрямую (не через Spring контекст).
     *
     * Тесты типа TravelCalculatePremiumRequestValidatorTest передают
     * репозитории/ReferenceDataPort вручную — этот конструктор создаёт
     * доменные валидаторы на основе переданного ReferenceDataPort.
     *
     * НЕ помечен @Autowired — Spring игнорирует этот конструктор.
     */
    public TravelCalculatePremiumRequestValidator(
            CountryRepository countryRepository,
            MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository,
            RiskTypeRepository riskRepository,
            ReferenceDataPort referenceDataPort) {

        this.personValidator = new PersonValidator();
        this.tripValidator = new TripValidator(referenceDataPort);
        this.coverageValidator = new CoverageValidator(referenceDataPort);
        this.selectedRisksValidator = new SelectedRisksValidator(referenceDataPort);
        this.commercialValidator = new CommercialValidator();

        log.debug("TravelCalculatePremiumRequestValidator created via direct constructor (non-Spring context)");
    }

    /**
     * Валидирует запрос на расчёт страховой премии.
     *
     * Порядок вызова доменных валидаторов:
     *   PersonValidator → TripValidator → CoverageValidator
     *   → SelectedRisksValidator → CommercialValidator
     *
     * При обнаружении CRITICAL-ошибок в любом доменном валидаторе
     * оркестрация прекращается и возвращаются накопленные ошибки.
     *
     * @param request запрос на расчёт
     * @return список ошибок валидации; пустой список если ошибок нет
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request) {
        ValidationContext context = new ValidationContext();
        List<ValidationError> allErrors = new ArrayList<>();

        // ── 1. PersonValidator ────────────────────────────────────────────────
        List<ValidationError> personErrors = personValidator.validate(request, context);
        allErrors.addAll(personErrors);

        if (hasCriticalErrors(personErrors)) {
            log.debug("PersonValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 2. TripValidator ──────────────────────────────────────────────────
        List<ValidationError> tripErrors = tripValidator.validate(request, context);
        allErrors.addAll(tripErrors);

        if (hasCriticalErrors(tripErrors)) {
            log.debug("TripValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 3. CoverageValidator ──────────────────────────────────────────────
        List<ValidationError> coverageErrors = coverageValidator.validate(request, context);
        allErrors.addAll(coverageErrors);

        if (hasCriticalErrors(coverageErrors)) {
            log.debug("CoverageValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 4. SelectedRisksValidator ─────────────────────────────────────────
        List<ValidationError> risksErrors = selectedRisksValidator.validate(request, context);
        allErrors.addAll(risksErrors);

        if (hasCriticalErrors(risksErrors)) {
            log.debug("SelectedRisksValidator returned critical errors, stopping validation");
            return allErrors;
        }

        // ── 5. CommercialValidator ────────────────────────────────────────────
        List<ValidationError> commercialErrors = commercialValidator.validate(request, context);
        allErrors.addAll(commercialErrors);

        return allErrors;
    }

    /**
     * Проверяет наличие CRITICAL-ошибок в списке.
     */
    private boolean hasCriticalErrors(List<ValidationError> errors) {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == ValidationError.Severity.CRITICAL);
    }
}