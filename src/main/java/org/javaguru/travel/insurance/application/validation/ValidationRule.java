package org.javaguru.travel.insurance.application.validation;

/**
 * Базовый интерфейс для всех правил валидации
 *
 * @param <T> тип объекта для валидации
 */
public interface ValidationRule<T> {

    /**
     * Валидирует объект
     *
     * @param target объект для валидации
     * @param context контекст валидации
     * @return результат валидации
     */
    ValidationResult validate(T target, ValidationContext context);

    /**
     * Уникальное имя правила (для логирования и отладки)
     * Рекомендуемый формат: "ClassName" или "field.rule"
     *
     * @return имя правила
     */
    String getRuleName();

    /**
     * Порядок выполнения правила (меньше = раньше)
     *
     * Рекомендуемые диапазоны:
     * - 1-99:   Structural validation
     * - 100-199: Business rule
     * - 200-299: Reference data
     * - 300+:    Complex validation
     *
     * @return порядок выполнения
     */
    default int getOrder() {
        return 100;
    }

    /**
     * Признак критичности правила
     * Если true и валидация не прошла, дальнейшая валидация прерывается
     *
     * @return true если правило критично
     */
    default boolean isCritical() {
        return false;
    }
}