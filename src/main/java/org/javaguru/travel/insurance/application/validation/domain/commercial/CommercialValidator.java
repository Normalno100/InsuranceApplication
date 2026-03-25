package org.javaguru.travel.insurance.application.validation.domain.commercial;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.CompositeValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.rule.reference.CurrencySupportValidator;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Доменный валидатор для коммерческих параметров страховки.
 *
 * task_136: Часть рефакторинга по доменному принципу.
 *
 * Проверяемые аспекты:
 *   - currency: поддерживается ли указанная валюта (EUR, USD, GBP, CHF, JPY)
 *
 * Примечание: Промо-код не валидируется на этом уровне — его применение
 * выполняется в PromoCodeService (включает проверку лимитов, дат и суммы).
 * Корпоративный флаг (isCorporate) и количество персон (personsCount)
 * являются опциональными и не требуют дополнительной валидации структуры.
 *
 * Используется как Spring-компонент, инжектируется в
 * TravelCalculatePremiumRequestValidator (оркестратор).
 */
@Component
public class CommercialValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public CommercialValidator() {
        this.compositeValidator = buildCompositeValidator();
    }

    /**
     * Валидирует коммерческие параметры запроса.
     *
     * @param request запрос на расчёт премии
     * @param context контекст валидации
     * @return список ошибок, пустой если валидация прошла
     */
    public List<ValidationError> validate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        ValidationResult result = compositeValidator.validate(request, context);
        return result.isValid() ? List.of() : result.getErrors();
    }

    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator() {
        return CompositeValidator.<TravelCalculatePremiumRequest>builder("CommercialValidator")
                // Проверка поддерживаемой валюты
                .addRule(new CurrencySupportValidator())

                .stopOnCriticalError(true)
                .build();
    }
}