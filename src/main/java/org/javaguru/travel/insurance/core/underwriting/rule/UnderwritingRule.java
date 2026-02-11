package org.javaguru.travel.insurance.core.underwriting.rule;

import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

/**
 * Базовый интерфейс для всех правил андеррайтинга
 */
public interface UnderwritingRule {

    /**
     * Проверяет заявку на соответствие правилу
     *
     * @param request заявка на страхование
     * @return результат проверки правила
     */
    RuleResult evaluate(TravelCalculatePremiumRequest request);

    /**
     * Название правила (для логирования и отчётов)
     */
    String getRuleName();

    /**
     * Порядок выполнения правила (меньше = выше приоритет)
     * Блокирующие правила должны выполняться первыми
     */
    default int getOrder() {
        return 100;
    }
}