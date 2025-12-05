package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;

import java.util.Optional;

/**
 * Интерфейс для правил валидации запросов версии 2
 */
public interface ValidationRuleV2 {

    /**
     * Выполняет валидацию запроса V2
     *
     * @param request запрос для валидации
     * @return Optional с ошибкой валидации, если валидация не прошла,
     *         или Optional.empty() если валидация успешна
     */
    Optional<ValidationError> validate(TravelCalculatePremiumRequestV2 request);
}