package org.javaguru.travel.insurance.core;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Валидатор запросов версии 2 на основе данных из БД
 *
 * Использует набор правил валидации (ValidationRuleV2), которые проверяют:
 * - Существование стран в БД
 * - Существование уровней медицинского покрытия
 * - Существование типов рисков
 * - Активность всех справочников на дату поездки
 */
@Component
@RequiredArgsConstructor
public class TravelCalculatePremiumRequestValidatorV2Impl {

    /**
     * Список всех правил валидации для V2
     * Spring автоматически инжектит все бины, реализующие ValidationRuleV2
     */
    private final List<ValidationRuleV2> validationRules;

    /**
     * Валидирует запрос V2, применяя все зарегистрированные правила
     *
     * @param request запрос для валидации
     * @return список ошибок валидации (пустой список если ошибок нет)
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {
        return validationRules.stream()
                .map(rule -> rule.validate(request))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}