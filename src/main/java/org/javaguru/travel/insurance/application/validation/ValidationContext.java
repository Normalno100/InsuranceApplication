package org.javaguru.travel.insurance.application.validation;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контекст валидации.
 * Позволяет передавать дополнительные данные между валидаторами.
 *
 * ЭТАП 1 (рефакторинг): Добавлен конструктор с java.time.Clock.
 *
 * БЫЛО:
 *   public ValidationContext() {
 *       this(LocalDate.now());  // нестабильно — зависит от системного времени
 *   }
 *
 * СТАЛО:
 *   public ValidationContext() {
 *       this(Clock.systemDefaultZone());  // продакшн: системные часы
 *   }
 *   public ValidationContext(Clock clock) {
 *       this.validationDate = LocalDate.now(clock);  // тест: Clock.fixed(...)
 *   }
 *
 * Конструктор с LocalDate оставлен для обратной совместимости.
 */
public class ValidationContext {

    private final LocalDate validationDate;
    private final Map<String, Object> attributes;

    /**
     * Конструктор по умолчанию — использует системные часы.
     * Используется в продакшн-коде.
     */
    public ValidationContext() {
        this(Clock.systemDefaultZone());
    }

    /**
     * Конструктор с Clock — основной для тестов.
     *
     * В тестах передавать TestConstants.TEST_CLOCK:
     *   new ValidationContext(TestConstants.TEST_CLOCK)
     *
     * @param clock источник текущего времени
     */
    public ValidationContext(Clock clock) {
        this.validationDate = LocalDate.now(clock);
        this.attributes = new HashMap<>();
    }

    /**
     * Конструктор с явной датой.
     * Оставлен для обратной совместимости.
     *
     * @deprecated Используйте конструктор с Clock для предсказуемости.
     *             В тестах: new ValidationContext(TestConstants.TEST_CLOCK)
     */
    @Deprecated(since = "refactoring-stage-1", forRemoval = false)
    public ValidationContext(LocalDate validationDate) {
        this.validationDate = validationDate;
        this.attributes = new HashMap<>();
    }

    /**
     * Дата валидации (для проверки validFrom/validTo в справочниках).
     */
    public LocalDate getValidationDate() {
        return validationDate;
    }

    /**
     * Сохранить атрибут в контекст.
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Получить атрибут из контекста.
     */
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Проверить наличие атрибута.
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Создать копию контекста.
     */
    public ValidationContext copy() {
        ValidationContext copy = new ValidationContext(this.validationDate);
        copy.attributes.putAll(this.attributes);
        return copy;
    }
}