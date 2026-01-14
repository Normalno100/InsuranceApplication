package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Проверяет что страна существует и активна на дату начала поездки
 */
public class CountryExistenceValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final CountryRepository countryRepository;

    public CountryExistenceValidator(CountryRepository countryRepository) {
        super("CountryExistenceValidator", 210); // Order = 210 (reference)
        this.countryRepository = countryRepository;
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        String countryIsoCode = request.getCountryIsoCode();
        LocalDate agreementDateFrom = request.getAgreementDateFrom();

        // Если поля null или некорректны, пропускаем
        if (countryIsoCode == null || agreementDateFrom == null) {
            return success();
        }

        Optional<CountryEntity> countryOpt =
                countryRepository.findActiveByIsoCode(countryIsoCode, agreementDateFrom);

        if (countryOpt.isEmpty()) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    "countryIsoCode",
                                    String.format("Country with ISO code '%s' not found or not active on %s!",
                                            countryIsoCode, agreementDateFrom)
                            )
                            .withParameter("countryIsoCode", countryIsoCode)
                            .withParameter("agreementDateFrom", agreementDateFrom)
            );
        }

        // Сохраняем найденную страну в контекст для других валидаторов/сервисов
        context.setAttribute("country", countryOpt.get());

        return success();
    }
}