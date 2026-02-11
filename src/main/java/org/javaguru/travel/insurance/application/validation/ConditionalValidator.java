package org.javaguru.travel.insurance.application.validation;

import java.util.function.Predicate;

/**
 * Условный валидатор - выполняется только если условие истинно
 *
 * @param <T> тип объекта для валидации
 */
public class ConditionalValidator<T> implements ValidationRule<T> {

    private final Predicate<T> condition;
    private final ValidationRule<T> rule;

    public ConditionalValidator(Predicate<T> condition, ValidationRule<T> rule) {
        this.condition = condition;
        this.rule = rule;
    }

    @Override
    public ValidationResult validate(T target, ValidationContext context) {
        if (condition.test(target)) {
            return rule.validate(target, context);
        }
        return ValidationResult.success();
    }

    @Override
    public String getRuleName() {
        return "Conditional[" + rule.getRuleName() + "]";
    }

    @Override
    public int getOrder() {
        return rule.getOrder();
    }

    @Override
    public boolean isCritical() {
        return rule.isCritical();
    }

    /**
     * Фабричный метод для создания условного валидатора
     */
    public static <T> ConditionalValidator<T> when(
            Predicate<T> condition,
            ValidationRule<T> rule) {
        return new ConditionalValidator<>(condition, rule);
    }
}