package org.javaguru.travel.insurance.core.validation;

/**
 * Абстрактный базовый класс для правил валидации
 * Предоставляет общую функциональность
 *
 * @param <T> тип объекта для валидации
 */
public abstract class AbstractValidationRule<T> implements ValidationRule<T> {

    private final String ruleName;
    private final int order;
    private final boolean critical;

    protected AbstractValidationRule(String ruleName) {
        this(ruleName, 100, false);
    }

    protected AbstractValidationRule(String ruleName, int order) {
        this(ruleName, order, false);
    }

    protected AbstractValidationRule(String ruleName, int order, boolean critical) {
        this.ruleName = ruleName;
        this.order = order;
        this.critical = critical;
    }

    @Override
    public String getRuleName() {
        return ruleName;
    }

    @Override
    public int getOrder() {
        return order;
    }

    @Override
    public boolean isCritical() {
        return critical;
    }

    /**
     * Шаблонный метод для валидации
     */
    @Override
    public ValidationResult validate(T target, ValidationContext context) {
        // Pre-validation hook
        preValidate(target, context);

        // Actual validation
        ValidationResult result = doValidate(target, context);

        // Post-validation hook
        postValidate(target, context, result);

        return result;
    }

    /**
     * Метод для выполнения валидации (должен быть переопределён)
     */
    protected abstract ValidationResult doValidate(T target, ValidationContext context);

    /**
     * Hook метод - вызывается перед валидацией
     */
    protected void preValidate(T target, ValidationContext context) {
        // Override if needed
    }

    /**
     * Hook метод - вызывается после валидации
     */
    protected void postValidate(T target, ValidationContext context, ValidationResult result) {
        // Override if needed
    }

    /**
     * Вспомогательный метод для создания успешного результата
     */
    protected ValidationResult success() {
        return ValidationResult.success();
    }

    /**
     * Вспомогательный метод для создания ошибки
     */
    protected ValidationResult failure(String field, String message) {
        return ValidationResult.failure(
                new ValidationError(field, message)
        );
    }

    /**
     * Вспомогательный метод для создания критичной ошибки
     */
    protected ValidationResult criticalFailure(String field, String message) {
        return ValidationResult.failure(
                ValidationError.critical(field, message)
        );
    }
}