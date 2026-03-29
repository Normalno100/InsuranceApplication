package org.javaguru.travel.insurance.infrastructure.persistence.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для JsonStringConverter.
 *
 * task_133: JPA AttributeConverter, обеспечивающий совместимость
 * JSON-полей с H2 (тесты) и PostgreSQL (production).
 */
@DisplayName("JsonStringConverter")
class JsonStringConverterTest {

    private final JsonStringConverter converter = new JsonStringConverter();

    // ── convertToDatabaseColumn ───────────────────────────────────────────────

    @Nested
    @DisplayName("convertToDatabaseColumn()")
    class ConvertToDatabaseColumnTests {

        @Test
        @DisplayName("должен вернуть null когда атрибут null")
        void shouldReturnNullWhenAttributeIsNull() {
            String result = converter.convertToDatabaseColumn(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("должен вернуть JSON строку без изменений")
        void shouldReturnJsonStringAsIs() {
            String json = "{\"decision\":\"APPROVED\",\"rules\":[]}";
            String result = converter.convertToDatabaseColumn(json);
            assertThat(result).isEqualTo(json);
        }

        @Test
        @DisplayName("должен вернуть валидный JSON объект")
        void shouldReturnValidJsonObject() {
            String json = "{\"key\":\"value\",\"number\":42}";
            String result = converter.convertToDatabaseColumn(json);
            assertThat(result).isEqualTo(json);
        }

        @Test
        @DisplayName("должен вернуть JSON массив")
        void shouldReturnJsonArray() {
            String json = "[\"APPROVED\",\"DECLINED\"]";
            String result = converter.convertToDatabaseColumn(json);
            assertThat(result).isEqualTo(json);
        }

        @Test
        @DisplayName("должен вернуть пустой JSON объект")
        void shouldReturnEmptyJsonObject() {
            String json = "{}";
            String result = converter.convertToDatabaseColumn(json);
            assertThat(result).isEqualTo(json);
        }

        @Test
        @DisplayName("должен записать невалидный JSON с предупреждением (не бросать исключение)")
        void shouldNotThrowExceptionForInvalidJson() {
            String invalidJson = "not a valid json";
            String result = converter.convertToDatabaseColumn(invalidJson);
            // Логирует предупреждение, но сохраняет значение
            assertThat(result).isEqualTo(invalidJson);
        }

        @Test
        @DisplayName("должен обработать вложенный JSON объект")
        void shouldHandleNestedJson() {
            String json = "{\"person\":{\"name\":\"Ivan\",\"age\":35},\"risks\":[\"TRAVEL_MEDICAL\"]}";
            String result = converter.convertToDatabaseColumn(json);
            assertThat(result).isEqualTo(json);
        }
    }

    // ── convertToEntityAttribute ──────────────────────────────────────────────

    @Nested
    @DisplayName("convertToEntityAttribute()")
    class ConvertToEntityAttributeTests {

        @Test
        @DisplayName("должен вернуть null когда данные из БД null")
        void shouldReturnNullWhenDbDataIsNull() {
            String result = converter.convertToEntityAttribute(null);
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("должен вернуть строку без изменений")
        void shouldReturnStringAsIs() {
            String dbData = "{\"decision\":\"APPROVED\"}";
            String result = converter.convertToEntityAttribute(dbData);
            assertThat(result).isEqualTo(dbData);
        }

        @Test
        @DisplayName("должен вернуть пустую строку")
        void shouldReturnEmptyString() {
            String dbData = "";
            String result = converter.convertToEntityAttribute(dbData);
            assertThat(result).isEqualTo(dbData);
        }

        @Test
        @DisplayName("должен вернуть JSON массив без изменений")
        void shouldReturnJsonArrayAsIs() {
            String dbData = "[{\"ruleName\":\"AgeRule\",\"severity\":\"PASS\"}]";
            String result = converter.convertToEntityAttribute(dbData);
            assertThat(result).isEqualTo(dbData);
        }
    }

    // ── round-trip ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Round-trip (write → read)")
    class RoundTripTests {

        @Test
        @DisplayName("round-trip должен сохранять значение без потерь")
        void shouldPreserveValueInRoundTrip() {
            String original = "{\"decision\":\"APPROVED\",\"reason\":null,\"rules\":[{\"name\":\"AgeRule\"}]}";

            String dbValue = converter.convertToDatabaseColumn(original);
            String restored = converter.convertToEntityAttribute(dbValue);

            assertThat(restored).isEqualTo(original);
        }

        @Test
        @DisplayName("round-trip для null должен вернуть null")
        void shouldPreserveNullInRoundTrip() {
            String dbValue = converter.convertToDatabaseColumn(null);
            String restored = converter.convertToEntityAttribute(dbValue);
            assertThat(restored).isNull();
        }

        @Test
        @DisplayName("round-trip для сложного JSON с вложенными структурами")
        void shouldPreserveComplexJsonInRoundTrip() {
            String original = """
                    {"requestId":"abc-123","personFirstName":"Ivan","personLastName":"Petrov",
                     "decision":"APPROVED","ruleResults":[
                       {"ruleName":"AgeRule","severity":"PASS","message":"Rule passed"},
                       {"ruleName":"CountryRiskRule","severity":"WARNING","message":"Medium risk"}
                     ]}""";

            String dbValue = converter.convertToDatabaseColumn(original);
            String restored = converter.convertToEntityAttribute(dbValue);

            assertThat(restored).isEqualTo(original);
        }
    }
}