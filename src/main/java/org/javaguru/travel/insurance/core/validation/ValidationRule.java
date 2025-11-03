package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;

import java.util.Optional;

/**
 * Интерфейс для правил валидации полей запроса
 * Каждое правило отвечает за валидацию одного аспекта запроса
 */
public interface ValidationRule {

    /**
     * Выполняет валидацию запроса
     *
     * @param request запрос для валидации
     * @return Optional с ошибкой валидации, если валидация не прошла,
     *         или Optional.empty() если валидация успешна
     */
    Optional<ValidationError> validate(TravelCalculatePremiumRequest request);
}