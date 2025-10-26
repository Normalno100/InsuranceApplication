package org.javaguru.travel.insurance.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Утилита для загрузки тестовых данных из JSON файлов
 */
public class TestDataLoader {

    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // Регистрируем модуль для Java 8 Date/Time API
        objectMapper.registerModule(new JavaTimeModule());
        // Отключаем запись дат как timestamps
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final String REQUESTS_PATH = "test-data/requests/";
    private static final String RESPONSES_PATH = "test-data/responses/";

    /**
     * Загружает запрос из JSON файла и десериализует в объект
     *
     * @param fileName имя файла
     * @param clazz класс для десериализации
     * @return объект запроса
     */
    public static <T> T loadRequest(String fileName, Class<T> clazz) throws IOException {
        return loadJson(REQUESTS_PATH + fileName, clazz);
    }

    /**
     * Загружает ответ из JSON файла и десериализует в объект
     *
     * @param fileName имя файла (например, "successful-response.json")
     * @param clazz класс для десериализации
     * @return объект ответа
     */
    public static <T> T loadResponse(String fileName, Class<T> clazz) throws IOException {
        return loadJson(RESPONSES_PATH + fileName, clazz);
    }

    /**
     * Загружает запрос как строку (для использования в MockMvc)
     *
     * @param fileName имя файла
     * @return JSON строка
     */
    public static String loadRequestAsString(String fileName) throws IOException {
        return loadJsonAsString(REQUESTS_PATH + fileName);
    }

    /**
     * Загружает ответ как строку
     *
     * @param fileName имя файла
     * @return JSON строка
     */
    public static String loadResponseAsString(String fileName) throws IOException {
        return loadJsonAsString(RESPONSES_PATH + fileName);
    }

    /**
     * Загружает произвольный JSON файл и десериализует
     */
    public static <T> T loadJson(String path, Class<T> clazz) throws IOException {
        try (InputStream is = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return objectMapper.readValue(is, clazz);
        }
    }

    /**
     * Загружает JSON файл как строку
     */
    public static String loadJsonAsString(String path) throws IOException {
        try (InputStream is = TestDataLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IOException("Resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Получает ObjectMapper для тестов (с поддержкой LocalDate)
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}