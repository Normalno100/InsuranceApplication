package org.javaguru.travel.insurance.application.validation;

import lombok.Getter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Ошибка валидации с поддержкой severity и i18n.
 *
 * РЕФАКТОРИНГ (п. 5.2): Иммутабельность ValidationError
 *
 * БЫЛО:
 *   - @Setter позволял изменять поля после создания
 *   - parameters — изменяемый HashMap, возвращался через геттер напрямую
 *   - withParameter() мутировал существующий объект (this.parameters.put(...))
 *
 * СТАЛО:
 *   - @Setter удалён; все поля final — объект неизменяем после создания
 *   - parameters через геттер отдаётся как Collections.unmodifiableMap()
 *     — внешний код не может изменить карту параметров
 *   - withParameter() создаёт НОВЫЙ ValidationError с расширенным набором
 *     параметров (copy-on-write), сохраняя привычный fluent API без мутации
 *
 * СОВМЕСТИМОСТЬ: публичные конструкторы и фабричные методы не изменились.
 * Весь существующий код, использующий цепочки withParameter(), продолжает
 * работать без изменений — разница только в том, что теперь каждый вызов
 * возвращает новый объект, а не изменяет текущий.
 */
@Getter
public class ValidationError {

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
        this(field, message, errorCode, severity, Collections.emptyMap());
    }

    /**
     * Приватный полный конструктор — используется только внутри класса
     * для создания копии с расширенными параметрами в withParameter().
     */
    private ValidationError(String field, String message, String errorCode,
                            Severity severity, Map<String, Object> parameters) {
        this.field     = field;
        this.message   = message;
        this.errorCode = errorCode;
        this.severity  = severity != null ? severity : Severity.ERROR;
        // Храним собственную изолированную копию карты
        this.parameters = Collections.unmodifiableMap(new HashMap<>(parameters));
    }

    /**
     * Возвращает НОВЫЙ ValidationError с добавленным параметром.
     *
     * Исходный объект остаётся неизменным (иммутабельность).
     * Использование: цепочки вызовов работают как раньше —
     *   ValidationError.error("field", "msg")
     *       .withParameter("min", 0)
     *       .withParameter("max", 80)
     */
    public ValidationError withParameter(String key, Object value) {
        Map<String, Object> extended = new HashMap<>(this.parameters);
        extended.put(key, value);
        return new ValidationError(this.field, this.message, this.errorCode,
                this.severity, extended);
    }

    /**
     * Уровень серьёзности ошибки.
     */
    public enum Severity {
        /** Предупреждение — не блокирует операцию */
        WARNING,
        /** Ошибка — блокирует операцию */
        ERROR,
        /** Критическая ошибка — прерывает дальнейшую валидацию */
        CRITICAL
    }

    // ── Фабричные методы ────────────────────────────────────────────────────

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