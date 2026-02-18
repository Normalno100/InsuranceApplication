package org.javaguru.travel.insurance.web.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.infrastructure.web.config.JacksonConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@JsonTest
@Import(JacksonConfig.class)
@DisplayName("Jackson BigDecimal JSON Formatting Tests")
class BigDecimalJsonFormattingTest {

    @Autowired
    private ObjectMapper objectMapper;

    record MoneyDto(BigDecimal amount) {}

    record ComplexMoneyDto(
            BigDecimal totalPremium,
            BigDecimal discount,
            BigDecimal finalAmount
    ) {}

    @Test
    @DisplayName("Should serialize integer BigDecimal with 2 decimal places")
    void shouldFormatIntegerWithTwoDecimals() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("10")));

        assertThat(json).contains("10.00");
        assertThat(json).doesNotContain("\"10.00\"");
    }

    @Test
    @DisplayName("Should format decimal with trailing zeros")
    void shouldFormatDecimalWithTrailingZeros() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("5.5")));
        assertThat(json).contains("5.50");
    }

    @Test
    @DisplayName("Should use plain notation for large numbers")
    void shouldUsePlainNotationForLargeNumbers() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("1500")));

        assertThat(json).contains("1500.00");
        assertThat(json).doesNotContain("E");
    }

    @Test
    @DisplayName("Should round to 2 decimal places using HALF_UP")
    void shouldRoundToTwoDecimalsHalfUp() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("123.456")));
        assertThat(json).contains("123.46");
    }

    @Test
    @DisplayName("Should format small decimal correctly")
    void shouldFormatSmallDecimalCorrectly() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("0.1")));
        assertThat(json).contains("0.10");
    }

    @Test
    @DisplayName("Should handle zero correctly")
    void shouldHandleZeroCorrectly() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(BigDecimal.ZERO));
        assertThat(json).contains("0.00");
    }

    @Test
    @DisplayName("Should handle null correctly")
    void shouldHandleNullCorrectly() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(null));

        JsonNode node = objectMapper.readTree(json);
        assertThat(node.get("amount").isNull()).isTrue();
    }

    @Test
    @DisplayName("Should format negative number correctly")
    void shouldFormatNegativeNumberCorrectly() throws Exception {
        String json = objectMapper.writeValueAsString(new MoneyDto(new BigDecimal("-50.99")));
        assertThat(json).contains("-50.99");
    }

    @Test
    @DisplayName("Complete JSON structure test")
    void shouldProduceCompleteCorrectJson() throws Exception {
        ComplexMoneyDto dto = new ComplexMoneyDto(
                new BigDecimal("100"),
                new BigDecimal("15.5"),
                new BigDecimal("84.50")
        );

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"totalPremium\" : 100.00");
        assertThat(json).contains("\"discount\" : 15.50");
        assertThat(json).contains("\"finalAmount\" : 84.50");
    }

    @Test
    @DisplayName("Deserialization should preserve precision")
    void deserializationShouldPreservePrecision() throws Exception {
        String json = "{\"amount\":123.45}";

        MoneyDto dto = objectMapper.readValue(json, MoneyDto.class);

        assertEquals(0, new BigDecimal("123.45").compareTo(dto.amount()));
    }

    @Test
    @DisplayName("Round-trip serialization should be consistent")
    void roundTripSerializationShouldBeConsistent() throws Exception {
        MoneyDto original = new MoneyDto(new BigDecimal("999.99"));

        String json = objectMapper.writeValueAsString(original);
        MoneyDto restored = objectMapper.readValue(json, MoneyDto.class);

        assertEquals(0, original.amount().compareTo(restored.amount()));
    }
}
