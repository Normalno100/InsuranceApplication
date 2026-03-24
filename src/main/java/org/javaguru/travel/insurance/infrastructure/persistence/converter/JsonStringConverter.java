package org.javaguru.travel.insurance.infrastructure.persistence.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA AttributeConverter для сериализации JSON-строк в БД и обратно.
 *
 * task_133: Заменяет @Type(JsonBinaryType.class) (Hypersistence Utils, PostgreSQL-specific)
 * для совместимости с H2 в тестах.
 *
 * ПРОИЗВОДСТВЕННАЯ СРЕДА (PostgreSQL):
 *   Колонка хранится как TEXT, не JSONB.
 *   Индексирование по JSON-полям через GIN недоступно,
 *   но для хранения audit-данных это приемлемо.
 *
 * ТЕСТОВАЯ СРЕДА (H2):
 *   Колонка хранится как VARCHAR — полная совместимость.
 *
 * ПРИМЕНЕНИЕ:
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(columnDefinition = "VARCHAR(5000)")
 *   private String ruleResults;
 */
@Slf4j
@Converter
public class JsonStringConverter implements AttributeConverter<String, String> {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Сохраняет JSON-строку в БД.
     * Логирует предупреждение если строка не является валидным JSON.
     */
    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        try {
            OBJECT_MAPPER.readTree(attribute);
        } catch (JsonProcessingException e) {
            log.warn("Storing potentially invalid JSON to DB column: {}", e.getMessage());
        }
        return attribute;
    }

    /**
     * Читает JSON-строку из БД.
     * Прозрачно возвращает как есть — трансформация не требуется.
     */
    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData;
    }
}