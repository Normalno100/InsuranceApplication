package org.javaguru.travel.insurance.application.validation.domain.coverage;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.CompositeValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.rule.reference.MedicalRiskLimitLevelExistenceValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.ConditionalMedicalRiskLimitLevelValidator;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Доменный валидатор для уровня медицинского покрытия.
 *
 * task_136: Часть рефакторинга по доменному принципу.
 *
 * Проверяемые аспекты:
 *   - medicalRiskLimitLevel обязателен в режиме MEDICAL_LEVEL (не COUNTRY_DEFAULT)
 *   - medicalRiskLimitLevel существует и активен на дату начала поездки
 *
 * Используется как Spring-компонент, инжектируется в
 * TravelCalculatePremiumRequestValidator (оркестратор).
 */
@Component
public class CoverageValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public CoverageValidator(ReferenceDataPort referenceDataPort) {
        this.compositeValidator = buildCompositeValidator(referenceDataPort);
    }

    /**
     * Валидирует параметры медицинского покрытия.
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

    private CompositeValidator<TravelCalculatePremiumRequest> buildCompositeValidator(
            ReferenceDataPort referenceDataPort) {

        return CompositeValidator.<TravelCalculatePremiumRequest>builder("CoverageValidator")
                // Условная обязательность medicalRiskLimitLevel
                // (необязателен в режиме COUNTRY_DEFAULT)
                .addRule(new ConditionalMedicalRiskLimitLevelValidator())

                // Проверка существования уровня в справочнике
                .addRule(new MedicalRiskLimitLevelExistenceValidator(referenceDataPort))

                .stopOnCriticalError(true)
                .build();
    }
}