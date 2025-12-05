package org.javaguru.travel.insurance.core.validation.field;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация кода страны из запроса V2
 * Проверяет существование страны в базе данных
 */
@Component
@RequiredArgsConstructor
public class CountryIsoCodeValidation implements ValidationRule {

    private final CountryRepository countryRepository;

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequest request) {
        // Эта валидация не применяется к базовому запросу V1
        // Только для V2, где есть countryIsoCode
        return Optional.empty();
    }
}