package org.javaguru.travel.insurance.infrastructure.web.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Кастомный Jackson сериализатор для BigDecimal значений
 * 
 * ЦЕЛЬ:
 * Обеспечить корректное форматирование денежных сумм в JSON:
 * - Всегда 2 десятичных знака: 10 → "10.00", 5.5 → "5.50"
 * - Plain notation (не научная): 1500 → "1500.00" (не "1.5E+3")
 * - Округление HALF_UP (банковское округление)
 * 
 * ПРИМЕРЫ:
 * Input        → Output JSON
 * ─────────────────────────
 * 10           → "10.00"
 * 5.5          → "5.50"
 * 1500         → "1500.00"
 * 123.456      → "123.46"  (округление)
 * 0.1          → "0.10"
 * 
 * ИСПОЛЬЗОВАНИЕ:
 * Автоматически применяется ко всем BigDecimal полям через JacksonConfig.
 */
public class BigDecimalJsonSerializer extends JsonSerializer<BigDecimal> {

    /**
     * Количество десятичных знаков для денежных значений
     * 2 знака - стандарт для большинства валют
     */
    private static final int DECIMAL_PLACES = 2;

    /**
     * Режим округления: HALF_UP (банковское округление)
     * 0.5 → округляется вверх
     * Пример: 10.125 → 10.13
     */
    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;

    @Override
    public void serialize(BigDecimal value, 
                         JsonGenerator gen, 
                         SerializerProvider serializers) throws IOException {
        
        if (value == null) {
            // Null значения сериализуем как null (не как "null")
            gen.writeNull();
            return;
        }

        // Форматируем BigDecimal:
        // 1. Округляем до 2 знаков с HALF_UP
        // 2. Устанавливаем scale = 2 (всегда 2 знака после запятой)
        BigDecimal formatted = value
                .setScale(DECIMAL_PLACES, ROUNDING_MODE);

        // Записываем как число (не как строку)
        // Jackson автоматически использует plain notation
        gen.writeNumber(formatted);
    }

    /**
     * Альтернативная версия: вывод как строка (опционально)
     * 
     * Иногда клиенты предпочитают получать денежные значения как строки
     * для избежания проблем с floating-point precision в JavaScript.
     * 
     * Раскомментируйте этот метод и закомментируйте предыдущий, если нужен вывод в строках.
     */
    /*
    @Override
    public void serialize(BigDecimal value, 
                         JsonGenerator gen, 
                         SerializerProvider serializers) throws IOException {
        
        if (value == null) {
            gen.writeNull();
            return;
        }

        BigDecimal formatted = value.setScale(DECIMAL_PLACES, ROUNDING_MODE);
        
        // Выводим как строку: "10.00" вместо 10.00
        gen.writeString(formatted.toPlainString());
    }
    */
}
