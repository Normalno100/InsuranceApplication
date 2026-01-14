package org.javaguru.travel.insurance.core.validation;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Контекст валидации
 * Позволяет передавать дополнительные данные между валидаторами
 */
public class ValidationContext {

    private final LocalDate validationDate;
    private final Map<String, Object> attributes;

    public ValidationContext() {
        this(LocalDate.now());
    }

    public ValidationContext(LocalDate validationDate) {
        this.validationDate = validationDate;
        this.attributes = new HashMap<>();
    }

    /**
     * Дата валидации (для проверки validFrom/validTo в справочниках)
     */
    public LocalDate getValidationDate() {
        return validationDate;
    }

    /**
     * Сохранить атрибут в контекст
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Получить атрибут из контекста
     */
    public <T> Optional<T> getAttribute(String key, Class<T> type) {
        Object value = attributes.get(key);
        if (value != null && type.isInstance(value)) {
            return Optional.of(type.cast(value));
        }
        return Optional.empty();
    }

    /**
     * Проверить наличие атрибута
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }

    /**
     * Создать копию контекста
     */
    public ValidationContext copy() {
        ValidationContext copy = new ValidationContext(this.validationDate);
        copy.attributes.putAll(this.attributes);
        return copy;
    }
}