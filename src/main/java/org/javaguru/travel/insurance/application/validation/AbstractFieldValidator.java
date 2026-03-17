package org.javaguru.travel.insurance.application.validation;

import java.util.Objects;
import java.util.function.Function;

/**
 * Базовый класс для валидаторов одного поля с встроенной защитой от null.
 *
 * РЕФАКТОРИНГ (п. 4.4 плана):
 *   ДО: defensive guard "if (field == null) return success()" дублировался
 *   примерно в 10 валидаторах:
 *     DateInPastValidator, IsoCodeValidator, StringLengthValidator,
 *     DateRangeValidator, AgeValidator, TripDurationValidator, и т.д.
 *
 *   ПОСЛЕ:
 *     1. Подклассы AbstractFieldValidator получают автоматическую null-защиту
 *        через метод skipIfNull() — достаточно объявить его в конструкторе.
 *     2. Для более сложных условий (пара полей и т.п.) — ConditionalValidator.when(Objects::nonNull).
 *     3. Существующие валидаторы рефакторятся постепенно: убирают ручной
 *        null-guard и при необходимости вызывают skipIfNull() или передают
 *        флаг в конструктор.
 *
 * ПАТТЕРН ИСПОЛЬЗОВАНИЯ:
 * <pre>
 *   // Вариант А: флаг skipIfNull в конструкторе подкласса
 *   public class DateInPastValidator<T> extends AbstractFieldValidator<T, LocalDate> {
 *       public DateInPastValidator(String fieldName, Function<T, LocalDate> extractor) {
 *           super(fieldName, extractor, 110, true); // skipIfNull=true
 *       }
 *       protected ValidationResult doValidateNonNull(LocalDate value, ValidationContext ctx) { ... }
 *   }
 *
 *   // Вариант Б: ConditionalValidator (для условий вне поля)
 *   .addRule(ConditionalValidator.when(
 *       req -> req.getDateFrom() != null,
 *       new DateInPastValidator<>(...)
 *   ))
 * </pre>
 *
 * @param <T> тип объекта для валидации
 * @param <F> тип поля
 */
public abstract class AbstractFieldValidator<T, F> extends FieldValidationRule<T, F> {

    /** Если true — автоматически возвращать success() когда значение поля равно null. */
    private final boolean skipIfNull;

    // ── Конструкторы ──────────────────────────────────────────────────────────

    /**
     * Конструктор без null-пропуска (поведение по умолчанию как в FieldValidationRule).
     * Используйте, если null должен вызывать ошибку или обрабатывается вручную.
     */
    protected AbstractFieldValidator(String fieldName,
                                     Function<T, F> fieldExtractor) {
        super(fieldName, fieldExtractor);
        this.skipIfNull = false;
    }

    /**
     * Конструктор с порядком выполнения.
     */
    protected AbstractFieldValidator(String fieldName,
                                     Function<T, F> fieldExtractor,
                                     int order) {
        super(fieldName, fieldExtractor, order);
        this.skipIfNull = false;
    }

    /**
     * Основной конструктор с явным управлением null-поведением.
     *
     * @param fieldName      имя поля для сообщений об ошибках
     * @param fieldExtractor функция извлечения значения поля из объекта
     * @param order          порядок выполнения валидатора
     * @param skipIfNull     если true — при null значении возвращать success()
     *                       без вызова validateField(). Устраняет дублирование
     *                       defensive guard "if (value == null) return success()".
     */
    protected AbstractFieldValidator(String fieldName,
                                     Function<T, F> fieldExtractor,
                                     int order,
                                     boolean skipIfNull) {
        super(fieldName, fieldExtractor, order);
        this.skipIfNull = skipIfNull;
    }

    // ── Основная логика ───────────────────────────────────────────────────────

    /**
     * Перехватывает вызов validateField() и применяет null-guard перед делегированием.
     *
     * Если skipIfNull=true и значение == null → возвращает success() без
     * вызова подкласса. Это устраняет необходимость повторять
     * "if (fieldValue == null) return success();" в каждом валидаторе.
     */
    @Override
    protected final ValidationResult validateField(F fieldValue, ValidationContext context) {
        if (skipIfNull && fieldValue == null) {
            return success();
        }
        return doValidateField(fieldValue, context);
    }

    /**
     * Метод для реализации бизнес-логики валидации в подклассах.
     *
     * При skipIfNull=true этот метод вызывается только с ненулевым значением.
     * При skipIfNull=false — вызывается с любым значением, включая null.
     *
     * @param fieldValue значение поля (может быть null если skipIfNull=false)
     * @param context    контекст валидации
     * @return результат валидации
     */
    protected abstract ValidationResult doValidateField(F fieldValue, ValidationContext context);

    // ── Вспомогательные методы для подклассов ────────────────────────────────

    /**
     * Проверяет, что строковое значение не пустое (после trim).
     * Часто используется в строковых валидаторах как дополнительная защита.
     *
     * @param value строковое значение
     * @return true если строка непустая
     */
    protected boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Проверяет, что значение не null.
     * Синоним Objects.nonNull() — для более читаемого кода в подклассах.
     */
    protected boolean isNotNull(F value) {
        return Objects.nonNull(value);
    }

    /**
     * Возвращает флаг skipIfNull.
     * Полезен для логирования/отладки.
     */
    protected boolean isSkipIfNull() {
        return skipIfNull;
    }
}