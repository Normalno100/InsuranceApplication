package org.javaguru.travel.insurance.web.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Тесты для проверки корректного форматирования BigDecimal в JSON
 * 
 * ЦЕЛЬ ТЕСТОВ:
 * Убедиться, что денежные значения всегда форматируются корректно:
 * - Всегда 2 десятичных знака
 * - Plain notation (не научная)
 * - Корректное округление
 */
@SpringBootTest
@DisplayName("Jackson BigDecimal JSON Formatting Tests")
class BigDecimalJsonFormattingTest {

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * DTO для тестирования сериализации
     */
    record MoneyDto(BigDecimal amount) {}

    @Test
    @DisplayName("Should serialize integer BigDecimal with 2 decimal places")
    void shouldFormatIntegerWithTwoDecimals() throws Exception {

        MoneyDto dto = new MoneyDto(new BigDecimal("10"));

        String json = objectMapper.writeValueAsString(dto);

        JsonNode node = objectMapper.readTree(json);
        JsonNode amountNode = node.get("amount");

        // Проверяем, что это число
        assertThat(amountNode.isNumber()).isTrue();

        // Проверяем текстовое представление в JSON
        assertThat(json).contains("10.00");

        // И что это не строка
        assertThat(json).doesNotContain("\"10.00\"");
    }

    @Test
    @DisplayName("Should format decimal with trailing zeros")
    void shouldFormatDecimalWithTrailingZeros() throws JsonProcessingException {
        // Given: число с одним десятичным знаком
        MoneyDto dto = new MoneyDto(new BigDecimal("5.5"));

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: должно быть "5.50"
        assertThat(json).contains("5.50");
    }

    @Test
    @DisplayName("Should use plain notation for large numbers")
    void shouldUsePlainNotationForLargeNumbers() throws JsonProcessingException {
        // Given: большое число
        MoneyDto dto = new MoneyDto(new BigDecimal("1500"));

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: "1500.00", НЕ "1.5E+3"
        assertThat(json).contains("1500.00");
        assertThat(json).doesNotContain("E");  // Нет научной нотации
    }

    @Test
    @DisplayName("Should round to 2 decimal places using HALF_UP")
    void shouldRoundToTwoDecimalsHalfUp() throws JsonProcessingException {
        // Given: число с >2 десятичными знаками
        MoneyDto dto = new MoneyDto(new BigDecimal("123.456"));

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: округление до "123.46"
        assertThat(json).contains("123.46");
    }

    @Test
    @DisplayName("Should format small decimal correctly")
    void shouldFormatSmallDecimalCorrectly() throws JsonProcessingException {
        // Given: малое число
        MoneyDto dto = new MoneyDto(new BigDecimal("0.1"));

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: "0.10"
        assertThat(json).contains("0.10");
    }

    @Test
    @DisplayName("Should handle zero correctly")
    void shouldHandleZeroCorrectly() throws JsonProcessingException {
        // Given: ноль
        MoneyDto dto = new MoneyDto(BigDecimal.ZERO);

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: "0.00"
        assertThat(json).contains("0.00");
    }

    @Test
    @DisplayName("Should handle null correctly")
    void shouldHandleNullCorrectly() throws JsonProcessingException {
        // Given: null
        MoneyDto dto = new MoneyDto(null);

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: null в JSON (не строка "null")
        assertThat(json).contains("null");
        assertThat(json).doesNotContain("\"null\"");
    }

    @Test
    @DisplayName("Should format negative number correctly")
    void shouldFormatNegativeNumberCorrectly() throws JsonProcessingException {
        // Given: отрицательное число
        MoneyDto dto = new MoneyDto(new BigDecimal("-50.99"));

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: "-50.99"
        assertThat(json).contains("-50.99");
    }

    @Test
    @DisplayName("Complete JSON structure test")
    void shouldProduceCompleteCorrectJson() throws JsonProcessingException {
        // Given: сложный DTO с несколькими суммами
        ComplexMoneyDto dto = new ComplexMoneyDto(
                new BigDecimal("100"),
                new BigDecimal("15.5"),
                new BigDecimal("84.50")
        );

        // When
        String json = objectMapper.writeValueAsString(dto);

        // Then: проверяем полную структуру
        assertThat(json).contains("\"totalPremium\" : 100.00");
        assertThat(json).contains("\"discount\" : 15.50");
        assertThat(json).contains("\"finalAmount\" : 84.50");
    }

    /**
     * DTO для комплексного теста
     */
    record ComplexMoneyDto(
            BigDecimal totalPremium,
            BigDecimal discount,
            BigDecimal finalAmount
    ) {}

    @Test
    @DisplayName("Deserialization should preserve precision")
    void deserializationShouldPreservePrecision() throws JsonProcessingException {
        // Given: JSON с точным значением
        String json = "{\"amount\": 123.45}";

        // When: десериализуем
        MoneyDto dto = objectMapper.readValue(json, MoneyDto.class);

        // Then: значение сохраняется точно
        assertEquals(0, new BigDecimal("123.45").compareTo(dto.amount()));
    }

    @Test
    @DisplayName("Round-trip serialization should be consistent")
    void roundTripSerializationShouldBeConsistent() throws JsonProcessingException {
        // Given: исходный DTO
        MoneyDto original = new MoneyDto(new BigDecimal("999.99"));

        // When: сериализуем и десериализуем обратно
        String json = objectMapper.writeValueAsString(original);
        MoneyDto restored = objectMapper.readValue(json, MoneyDto.class);

        // Then: значения должны совпадать
        assertEquals(0, original.amount().compareTo(restored.amount()));
    }
}
