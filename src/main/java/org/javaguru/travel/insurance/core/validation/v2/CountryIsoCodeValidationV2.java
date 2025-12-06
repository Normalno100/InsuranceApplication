package org.javaguru.travel.insurance.core.validation.v2;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация кода страны для запросов V2
 * Проверяет:
 * 1. Что код страны не пустой
 * 2. Что страна существует в базе данных
 * 3. Что страна активна на дату начала поездки
 */
@Component
@RequiredArgsConstructor
public class CountryIsoCodeValidationV2 implements ValidationRuleV2 {

    private final CountryRepository countryRepository;

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {
        // Проверка на null или пустую строку
        if (request.getCountryIsoCode() == null || request.getCountryIsoCode().trim().isEmpty()) {
            return Optional.of(new ValidationError("countryIsoCode", "Must not be empty!"));
        }

        String isoCode = request.getCountryIsoCode().trim().toUpperCase();

        // Проверка существования страны в БД
        var countryOpt = countryRepository.findActiveByIsoCode(
                isoCode,
                request.getAgreementDateFrom()
        );

        if (countryOpt.isEmpty()) {
            return Optional.of(new ValidationError(
                    "countryIsoCode",
                    "Country with ISO code '" + isoCode + "' not found or not active!"
            ));
        }

        return Optional.empty();
    }
}