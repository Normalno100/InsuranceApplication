package org.javaguru.travel.insurance.core;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.validation.ValidationRule;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Главный валидатор запросов на расчет премии
 * Использует набор правил валидации (ValidationRule) для проверки полей запроса
 *
 * Архитектура:
 * - Каждое правило валидации инкапсулировано в отдельном классе
 * - Правила автоматически инжектятся через Spring DI
 * - Валидатор применяет все правила и собирает ошибки
 */
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class TravelCalculatePremiumRequestValidator {

    /**
     * Список всех правил валидации
     * Spring автоматически инжектит все бины, реализующие ValidationRule
     */
    private final List<ValidationRule> validationRules;

    /**
     * Валидирует запрос, применяя все зарегистрированные правила
     *
     * @param request запрос для валидации
     * @return список ошибок валидации (пустой список если ошибок нет)
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request) {
        return validationRules.stream()
                .map(rule -> rule.validate(request))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}