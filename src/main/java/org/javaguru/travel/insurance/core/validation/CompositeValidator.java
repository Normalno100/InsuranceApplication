package org.javaguru.travel.insurance.core.validation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Композитный валидатор - выполняет несколько правил валидации
 *
 * @param <T> тип объекта для валидации
 */
public class CompositeValidator<T> implements ValidationRule<T> {

    private final String name;
    private final List<ValidationRule<T>> rules;
    private final boolean stopOnCriticalError;

    private CompositeValidator(String name, List<ValidationRule<T>> rules,
                               boolean stopOnCriticalError) {
        this.name = name;
        this.rules = new ArrayList<>(rules);
        // Сортируем правила по порядку выполнения
        this.rules.sort(Comparator.comparingInt(ValidationRule::getOrder));
        this.stopOnCriticalError = stopOnCriticalError;
    }

    @Override
    public ValidationResult validate(T target, ValidationContext context) {
        if (target == null) {
            return ValidationResult.failure(
                    ValidationError.critical("target", "Validation target cannot be null")
            );
        }

        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (ValidationRule<T> rule : rules) {
            try {
                ValidationResult ruleResult = rule.validate(target, context);

                if (ruleResult.hasErrors()) {
                    resultBuilder.addErrors(ruleResult.getErrors());

                    // Прерываем выполнение если есть критичные ошибки
                    if (stopOnCriticalError &&
                            (ruleResult.hasCriticalErrors() || rule.isCritical())) {
                        break;
                    }
                }

            } catch (Exception e) {
                // Если правило упало с ошибкой - это критично
                resultBuilder.addError(
                        ValidationError.critical(
                                "validation.error",
                                "Validation rule failed: " + rule.getRuleName() + " - " + e.getMessage()
                        )
                );

                if (stopOnCriticalError) {
                    break;
                }
            }
        }

        return resultBuilder.build();
    }

    @Override
    public String getRuleName() {
        return name;
    }

    /**
     * Builder для создания композитного валидатора
     */
    public static <T> Builder<T> builder(String name) {
        return new Builder<>(name);
    }

    public static class Builder<T> {
        private final String name;
        private final List<ValidationRule<T>> rules = new ArrayList<>();
        private boolean stopOnCriticalError = true;

        private Builder(String name) {
            this.name = name;
        }

        /**
         * Добавить правило валидации
         */
        public Builder<T> addRule(ValidationRule<T> rule) {
            this.rules.add(rule);
            return this;
        }

        /**
         * Добавить несколько правил
         */
        public Builder<T> addRules(List<ValidationRule<T>> rules) {
            this.rules.addAll(rules);
            return this;
        }

        /**
         * Остановить валидацию при критичной ошибке (по умолчанию true)
         */
        public Builder<T> stopOnCriticalError(boolean stop) {
            this.stopOnCriticalError = stop;
            return this;
        }

        /**
         * Создать композитный валидатор
         */
        public CompositeValidator<T> build() {
            if (rules.isEmpty()) {
                throw new IllegalStateException(
                        "CompositeValidator must have at least one rule"
                );
            }
            return new CompositeValidator<>(name, rules, stopOnCriticalError);
        }
    }
}