package org.javaguru.travel.insurance.util;

/**
 * Константы для имен JSON файлов с тестовыми данными
 * Используйте эти константы вместо строковых литералов для типобезопасности
 */
public final class TestDataConstants {

    private TestDataConstants() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // ========== ЗАПРОСЫ ==========

    public static final class Requests {
        private Requests() {}

        // Валидные запросы
        public static final String VALID = "valid-request.json";
        public static final String SAME_DATES = "same-dates-request.json";
        public static final String LONG_TRIP = "long-trip-request.json";
        public static final String SPECIAL_CHARS = "special-chars-request.json";
        public static final String CYRILLIC = "cyrillic-request.json";

        // Невалидные запросы
        public static final String EMPTY_FIRST_NAME = "empty-first-name-request.json";
        public static final String EMPTY_LAST_NAME = "empty-last-name-request.json";
        public static final String NULL_DATE_FROM = "null-date-from-request.json";
        public static final String NULL_DATE_TO = "null-date-to-request.json";
        public static final String INVALID_DATE_ORDER = "invalid-date-order-request.json";
        public static final String ALL_INVALID = "all-invalid-request.json";

        // Специальные случаи
        public static final String LEAP_YEAR = "leap-year-request.json";
        public static final String YEAR_BOUNDARY = "year-boundary-request.json";
        public static final String MULTIPLE_ERRORS = "multiple-errors-request.json";
    }

    // ========== ОТВЕТЫ ==========

    public static final class Responses {
        private Responses() {}

        // Успешные ответы
        public static final String SUCCESSFUL = "successful-response.json";
        public static final String ZERO_PRICE = "zero-price-response.json";
        public static final String LONG_TRIP = "long-trip-response.json";

        // Ответы с ошибками
        public static final String ERROR_FIRST_NAME = "error-first-name-response.json";
        public static final String ERROR_LAST_NAME = "error-last-name-response.json";
        public static final String ERROR_DATE_FROM = "error-date-from-response.json";
        public static final String ERROR_DATE_TO = "error-date-to-response.json";
        public static final String ERROR_DATE_ORDER = "error-date-order-response.json";
        public static final String ALL_ERRORS = "all-errors-response.json";
    }

    // ========== ПУТИ К ПАПКАМ ==========

    public static final class Paths {
        private Paths() {}

        public static final String REQUESTS = "test-data/requests/";
        public static final String RESPONSES = "test-data/responses/";
    }
}