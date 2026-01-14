package org.javaguru.travel.insurance.core.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Результат валидации
 */
public class ValidationResult {

    private final boolean valid;
    private final List<ValidationError> errors;

    private ValidationResult(boolean valid, List<ValidationError> errors) {
        this.valid = valid;
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }

    /**
     * Успешная валидация
     */
    public static ValidationResult success() {
        return new ValidationResult(true, Collections.emptyList());
    }

    /**
     * Валидация с одной ошибкой
     */
    public static ValidationResult failure(ValidationError error) {
        return new ValidationResult(false, List.of(error));
    }

    /**
     * Валидация с несколькими ошибками
     */
    public static ValidationResult failure(List<ValidationError> errors) {
        return new ValidationResult(false, errors);
    }

    /**
     * Проверка успешности валидации
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * Получить список ошибок (неизменяемый)
     */
    public List<ValidationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Проверка наличия ошибок
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Проверка наличия критичных ошибок
     */
    public boolean hasCriticalErrors() {
        return errors.stream()
                .anyMatch(e -> e.getSeverity() == ValidationError.Severity.CRITICAL);
    }

    /**
     * Объединить с другим результатом
     */
    public ValidationResult merge(ValidationResult other) {
        if (other == null) {
            return this;
        }

        List<ValidationError> mergedErrors = new ArrayList<>(this.errors);
        mergedErrors.addAll(other.errors);

        boolean mergedValid = this.valid && other.valid;

        return new ValidationResult(mergedValid, mergedErrors);
    }

    /**
     * Создать builder для постепенного добавления ошибок
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder для создания результата валидации
     */
    public static class Builder {
        private final List<ValidationError> errors = new ArrayList<>();

        public Builder addError(ValidationError error) {
            this.errors.add(error);
            return this;
        }

        public Builder addErrors(List<ValidationError> errors) {
            this.errors.addAll(errors);
            return this;
        }

        public Builder addErrorIf(boolean condition, ValidationError error) {
            if (condition) {
                this.errors.add(error);
            }
            return this;
        }

        public ValidationResult build() {
            if (errors.isEmpty()) {
                return success();
            }
            return failure(errors);
        }
    }
}