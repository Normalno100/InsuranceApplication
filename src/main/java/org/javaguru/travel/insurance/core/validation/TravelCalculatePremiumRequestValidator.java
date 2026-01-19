package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.structural.*;
import org.javaguru.travel.insurance.core.validation.business.*;
import org.javaguru.travel.insurance.core.validation.reference.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Главный валидатор для TravelCalculatePremiumRequest
 * Использует композитный подход с модульными валидаторами
 */
@Component
public class TravelCalculatePremiumRequestValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public TravelCalculatePremiumRequestValidator(
            CountryRepository countryRepository,
            MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository,
            RiskTypeRepository riskRepository) {

        this.compositeValidator = buildCompositeValidator(
                countryRepository,
                medicalRiskLimitLevelRepository,
                riskRepository
        );
    }

    /**
     * Валидация запроса
     *
     * @param request запрос на расчёт премии
     * @return список ошибок валидации (пустой если валидация успешна)
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request) {
        ValidationContext context = new ValidationContext();

        org.javaguru.travel.insurance.core.validation.ValidationResult result =
                compositeValidator.validate(request, context);

        if (result.isValid()) {
            return List.of();
        }

        // Конвертируем ValidationError из validation package в DTO
        return result.getErrors().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Построение композитного валидатора со всеми правилами
     */
    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator(
            CountryRepository countryRepository,
            MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository,
            RiskTypeRepository riskRepository) {

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
                .addRule(new IsoCodeValidator<>("countryIsoCode", 2,
                        TravelCalculatePremiumRequest::getCountryIsoCode))

                // medicalRiskLimitLevel
                .addRule(new NotNullValidator<>("medicalRiskLimitLevel",
                        TravelCalculatePremiumRequest::getMedicalRiskLimitLevel))
                .addRule(new NotBlankValidator<>("medicalRiskLimitLevel",
                        TravelCalculatePremiumRequest::getMedicalRiskLimitLevel))

                // selectedRisks (опциональное поле, но если есть - элементы не пустые)
                .addRule(new CollectionElementsNotBlankValidator<>("selectedRisks",
                        TravelCalculatePremiumRequest::getSelectedRisks))

                // ==========================================
                // LEVEL 2: BUSINESS RULES (Order: 100-199)
                // ==========================================

                .addRule(new DateInPastValidator<>("personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate))     // 110
                .addRule(new DateRangeValidator())                              // 120
                .addRule(new AgeValidator())                                    // 130
                .addRule(new TripDurationValidator())                           // 140
                .addRule(new AgreementDateFromNotTooFarValidator())             // 145
                .addRule(new FutureTripWarningValidator())                      // 150
                .addRule(new MandatoryRisksValidator())                         // 160
                .addRule(new DuplicateRisksValidator())                         // 165

                // ==========================================
                // LEVEL 3: REFERENCE DATA (Order: 200-299)
                // ==========================================

                .addRule(new CountryExistenceValidator(countryRepository))                      // 210
                .addRule(new MedicalRiskLimitLevelExistenceValidator(
                        medicalRiskLimitLevelRepository))                                           // 220
                .addRule(new RiskTypeExistenceValidator(riskRepository))                        // 230
                .addRule(new RiskTypeNotMandatoryValidator(riskRepository))                     // 240
                .addRule(new CurrencySupportValidator())                                        // 250

                // Останавливаем валидацию на критичных ошибках
                .stopOnCriticalError(true)
                .build();
    }

    /**
     * Конвертирует ValidationError из validation package в DTO
     */
    private ValidationError convertToDto(
            org.javaguru.travel.insurance.core.validation.ValidationError error) {

        ValidationError dto = new ValidationError();
        dto.setField(error.getField());
        dto.setMessage(error.getMessage());

        // Можно добавить errorCode если нужен для i18n
        // dto.setErrorCode(error.getErrorCode());

        return dto;
    }
}