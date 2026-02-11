package org.javaguru.travel.insurance.application.validation.rule.reference;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Проверяет что страна существует и активна на дату начала поездки
 */
public class CountryExistenceValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final ReferenceDataPort referenceDataPort;

    public CountryExistenceValidator(ReferenceDataPort referenceDataPort) {
        super("CountryExistenceValidator", 210); // Order = 210 (reference)
        this.referenceDataPort = referenceDataPort;
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

        // Используем Domain Port вместо Repository
        CountryCode code = new CountryCode(countryIsoCode);
        Optional<Country> countryOpt = referenceDataPort.findCountry(code, agreementDateFrom);

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