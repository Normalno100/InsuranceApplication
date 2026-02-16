package org.javaguru.travel.insurance.infrastructure.web.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;

/**
 * Глобальная конфигурация Jackson для Spring Boot приложения
 * 
 * ЦЕЛЬ:
 * Обеспечить корректное форматирование денежных значений (BigDecimal) в JSON.
 * 
 * ПРОБЛЕМА:
 * По умолчанию Jackson сериализует BigDecimal в научной нотации:
 * - 10.00 → 10.0 или 10
 * - 1000.50 → 1.00050E+3
 * 
 * РЕШЕНИЕ:
 * - Всегда выводить 2 десятичных знака (денежный формат)
 * - Использовать plain notation (не научную)
 * - Обеспечить консистентность во всех DTO
 * 
 * ПРИМЕНЕНИЕ:
 * Эта конфигурация автоматически применяется ко всем REST контроллерам.
 */
@Configuration
public class JacksonConfig {

    /**
     * Создает настроенный ObjectMapper для всего приложения
     * 
     * @Primary гарантирует, что этот ObjectMapper будет использоваться везде
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder
                .createXmlMapper(false)
                .build();

        // ========================================
        // Настройки для BigDecimal
        // ========================================
        
        // Отключаем научную нотацию (1.5E+3 → 1500.00)
        objectMapper.configure(SerializationFeature.WRITE_BIGDECIMAL_AS_PLAIN, true);
        
        // Регистрируем кастомный модуль для BigDecimal
        SimpleModule bigDecimalModule = new SimpleModule("BigDecimalModule");
        bigDecimalModule.addSerializer(BigDecimal.class, new BigDecimalJsonSerializer());
        objectMapper.registerModule(bigDecimalModule);

        // ========================================
        // Настройки для дат (Java 8 Time API)
        // ========================================
        
        // Поддержка LocalDate, LocalDateTime и т.д.
        objectMapper.registerModule(new JavaTimeModule());
        
        // Сериализовать даты как строки (ISO-8601), а не timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // ========================================
        // Общие настройки
        // ========================================
        
        // Не падать на неизвестных полях в JSON (для обратной совместимости)
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        
        // Pretty print для читабельности (можно отключить в production)
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);

        return objectMapper;
    }
}
