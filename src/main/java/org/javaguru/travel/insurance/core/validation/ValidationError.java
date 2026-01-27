package org.javaguru.travel.insurance.core.validation;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

/**
 * Ошибка валидации с поддержкой severity и i18n
 */
@Getter
@Setter
public class ValidationError {

    // Getters
    private final String field;
    private final String message;
    private final String errorCode;
    private final Severity severity;
    private final Map<String, Object> parameters;

    public ValidationError(String field, String message) {
        this(field, message, null, Severity.ERROR);
    }

    public ValidationError(String field, String message, String errorCode) {
        this(field, message, errorCode, Severity.ERROR);
    }

    public ValidationError(String field, String message, String errorCode, Severity severity) {
        this.field = field;
        this.message = message;
        this.errorCode = errorCode;
        this.severity = severity != null ? severity : Severity.ERROR;
        this.parameters = new HashMap<>();
    }

    /**
     * Добавить параметр для i18n
     */
    public ValidationError withParameter(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * Уровень серьёзности ошибки
     */
    public enum Severity {
        /**
         * Предупреждение - не блокирует операцию
         */
        WARNING,

        /**
         * Ошибка - блокирует операцию
         */
        ERROR,

        /**
         * Критическая ошибка - прерывает дальнейшую валидацию
         */
        CRITICAL
    }

    /**
     * Фабричные методы для удобства
     */
    public static ValidationError warning(String field, String message) {
        return new ValidationError(field, message, null, Severity.WARNING);
    }

    public static ValidationError error(String field, String message) {
        return new ValidationError(field, message, null, Severity.ERROR);
    }

    public static ValidationError critical(String field, String message) {
        return new ValidationError(field, message, null, Severity.CRITICAL);
    }

    public static ValidationError withCode(String field, String message, String errorCode) {
        return new ValidationError(field, message, errorCode, Severity.ERROR);
    }

    @Override
    public String toString() {
        return String.format("%s[%s]: %s (code: %s)",
                severity, field, message, errorCode);
    }
}