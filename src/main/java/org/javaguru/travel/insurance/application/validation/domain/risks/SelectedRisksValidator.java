package org.javaguru.travel.insurance.application.validation.domain.risks;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.CompositeValidator;
import org.javaguru.travel.insurance.application.validation.ConditionalValidator;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.rule.business.DuplicateRisksValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.MandatoryRisksValidator;
import org.javaguru.travel.insurance.application.validation.rule.reference.RiskTypeExistenceValidator;
import org.javaguru.travel.insurance.application.validation.rule.reference.RiskTypeNotMandatoryValidator;
import org.javaguru.travel.insurance.application.validation.rule.structural.CollectionElementsNotBlankValidator;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Доменный валидатор для выбранных рисков.
 *
 * task_136: Часть рефакторинга по доменному принципу.
 *
 * Проверяемые аспекты:
 *   - элементы списка selectedRisks не пустые (игнорируются null/blank)
 *   - TRAVEL_MEDICAL не передаётся явно (добавляется автоматически)
 *   - нет дубликатов в списке рисков
 *   - все риски существуют и активны на дату начала поездки
 *   - риски не являются обязательными (обязательные добавляются автоматически)
 *
 * Используется как Spring-компонент, инжектируется в
 * TravelCalculatePremiumRequestValidator (оркестратор).
 */
@Component
public class SelectedRisksValidator {

    private final CompositeValidator<TravelCalculatePremiumRequest> compositeValidator;

    public SelectedRisksValidator(ReferenceDataPort referenceDataPort) {
        this.compositeValidator = buildCompositeValidator(referenceDataPort);
    }

    /**
     * Валидирует список выбранных рисков.
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

        return CompositeValidator.<TravelCalculatePremiumRequest>builder("SelectedRisksValidator")
                // Структурная проверка элементов коллекции
                .addRule(new CollectionElementsNotBlankValidator<>("selectedRisks",
                        TravelCalculatePremiumRequest::getSelectedRisks))

                // Обязательный TRAVEL_MEDICAL не должен передаваться явно
                .addRule(new MandatoryRisksValidator())

                // Проверка дубликатов
                .addRule(new DuplicateRisksValidator())

                // Риски существуют в справочнике (только если есть риски и дата)
                .addRule(ConditionalValidator.when(
                        req -> req.getSelectedRisks() != null
                                && !req.getSelectedRisks().isEmpty()
                                && req.getAgreementDateFrom() != null,
                        new RiskTypeExistenceValidator(referenceDataPort)
                ))

                // Выбранные риски не являются обязательными
                .addRule(new RiskTypeNotMandatoryValidator(referenceDataPort))

                .stopOnCriticalError(true)
                .build();
    }
}