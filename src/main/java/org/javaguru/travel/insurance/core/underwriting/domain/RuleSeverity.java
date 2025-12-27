package org.javaguru.travel.insurance.core.underwriting.domain;

/**
 * Уровень серьёзности нарушения правила
 */
public enum RuleSeverity {

    /**
     * Правило пройдено успешно
     */
    PASS,

    /**
     * Предупреждение (не блокирует одобрение)
     */
    WARNING,

    /**
     * Требуется ручная проверка
     */
    REVIEW_REQUIRED,

    /**
     * Блокирующее нарушение (автоматический отказ)
     */
    BLOCKING
}