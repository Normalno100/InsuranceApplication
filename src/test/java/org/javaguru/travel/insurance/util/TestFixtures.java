package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Центральная фабрика для создания тестовых данных.
 * Устраняет дублирование кода в тестах
 */
public final class TestFixtures {

    private TestFixtures() {
        throw new AssertionError("Utility class");
    }

    // ========== СТАНДАРТНЫЕ ЗНАЧЕНИЯ ==========

    public static final class StandardValues {
        public static final String FIRST_NAME = "John";
        public static final String LAST_NAME = "Smith";
        public static final LocalDate DATE_FROM = LocalDate.of(2023, 1, 1);
        public static final LocalDate DATE_TO = LocalDate.of(2023, 1, 11);
        public static final long STANDARD_DAYS = 10L;
        public static final BigDecimal STANDARD_PRICE = new BigDecimal("10");

        // Специальные имена
        public static final String SPECIAL_FIRST_NAME = "Jean-Pierre";
        public static final String SPECIAL_LAST_NAME = "O'Connor";
        public static final String CYRILLIC_FIRST_NAME = "Иван";
        public static final String CYRILLIC_LAST_NAME = "Петров";
    }

    // ========== СООБЩЕНИЯ ОБ ОШИБКАХ ==========

    public static final class ErrorMessages {
        public static final String MUST_NOT_BE_EMPTY = "Must not be empty!";
        public static final String MUST_BE_AFTER = "Must be after agreementDateFrom!";
    }

    // ========== ФАБРИКИ ЗАПРОСОВ ==========

    /**
     * Создает валидный запрос с использованием стандартных значений
     */
    public static TravelCalculatePremiumRequest validRequest() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с пустым firstName
     */
    public static TravelCalculatePremiumRequest requestWithEmptyFirstName() {
        return request()
                .withFirstName("")
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с пустым lastName
     */
    public static TravelCalculatePremiumRequest requestWithEmptyLastName() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName("")
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с null dateFrom
     */
    public static TravelCalculatePremiumRequest requestWithNullDateFrom() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(null)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с null dateTo
     */
    public static TravelCalculatePremiumRequest requestWithNullDateTo() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(null)
                .build();
    }

    /**
     * Создает запрос с неверным порядком дат
     */
    public static TravelCalculatePremiumRequest requestWithInvalidDateOrder() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(StandardValues.DATE_TO)
                .withDateTo(StandardValues.DATE_FROM)
                .build();
    }

    /**
     * Создает запрос с одинаковыми датами
     */
    public static TravelCalculatePremiumRequest requestWithSameDates() {
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_FROM)
                .build();
    }

    /**
     * Создает запрос со специальными символами в именах
     */
    public static TravelCalculatePremiumRequest requestWithSpecialCharacters() {
        return request()
                .withFirstName(StandardValues.SPECIAL_FIRST_NAME)
                .withLastName(StandardValues.SPECIAL_LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с кириллическими символами
     */
    public static TravelCalculatePremiumRequest requestWithCyrillicNames() {
        return request()
                .withFirstName(StandardValues.CYRILLIC_FIRST_NAME)
                .withLastName(StandardValues.CYRILLIC_LAST_NAME)
                .withDateFrom(StandardValues.DATE_FROM)
                .withDateTo(StandardValues.DATE_TO)
                .build();
    }

    /**
     * Создает запрос с указанным периодом в днях
     */
    public static TravelCalculatePremiumRequest requestWithDays(long days) {
        LocalDate from = StandardValues.DATE_FROM;
        LocalDate to = from.plusDays(days);
        return request()
                .withFirstName(StandardValues.FIRST_NAME)
                .withLastName(StandardValues.LAST_NAME)
                .withDateFrom(from)
                .withDateTo(to)
                .build();
    }

    /**
     * Создает полностью невалидный запрос (все поля null/empty)
     */
    public static TravelCalculatePremiumRequest invalidRequest() {
        return request()
                .withFirstName(null)
                .withLastName(null)
                .withDateFrom(null)
                .withDateTo(null)
                .build();
    }

    // ========== ФАБРИКИ ОТВЕТОВ ==========

    /**
     * Создает успешный ответ на основе запроса и количества дней
     */
    public static TravelCalculatePremiumResponse successfulResponse(
            TravelCalculatePremiumRequest request, long days) {
        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
        response.setPersonFirstName(request.getPersonFirstName());
        response.setPersonLastName(request.getPersonLastName());
        response.setAgreementDateFrom(request.getAgreementDateFrom());
        response.setAgreementDateTo(request.getAgreementDateTo());
        response.setAgreementPrice(new BigDecimal(days));
        return response;
    }

    /**
     * Создает успешный ответ со стандартными значениями
     */
    public static TravelCalculatePremiumResponse successfulResponse() {
        return successfulResponse(validRequest(), StandardValues.STANDARD_DAYS);
    }

    /**
     * Создает ответ с одной ошибкой
     */
    public static TravelCalculatePremiumResponse errorResponse(String field, String message) {
        return new TravelCalculatePremiumResponse(
                List.of(new ValidationError(field, message))
        );
    }

    /**
     * Создает ответ с ошибкой для пустого firstName
     */
    public static TravelCalculatePremiumResponse errorResponseForFirstName() {
        return errorResponse("personFirstName", ErrorMessages.MUST_NOT_BE_EMPTY);
    }

    /**
     * Создает ответ с ошибкой для пустого lastName
     */
    public static TravelCalculatePremiumResponse errorResponseForLastName() {
        return errorResponse("personLastName", ErrorMessages.MUST_NOT_BE_EMPTY);
    }

    /**
     * Создает ответ с ошибкой для null dateFrom
     */
    public static TravelCalculatePremiumResponse errorResponseForDateFrom() {
        return errorResponse("agreementDateFrom", ErrorMessages.MUST_NOT_BE_EMPTY);
    }

    /**
     * Создает ответ с ошибкой для null dateTo
     */
    public static TravelCalculatePremiumResponse errorResponseForDateTo() {
        return errorResponse("agreementDateTo", ErrorMessages.MUST_NOT_BE_EMPTY);
    }

    /**
     * Создает ответ с ошибкой для неверного порядка дат
     */
    public static TravelCalculatePremiumResponse errorResponseForDateOrder() {
        return errorResponse("agreementDateTo", ErrorMessages.MUST_BE_AFTER);
    }

    /**
     * Создает ответ со всеми 4 ошибками
     */
    public static TravelCalculatePremiumResponse allErrorsResponse() {
        return new TravelCalculatePremiumResponse(
                List.of(
                        new ValidationError("personFirstName", ErrorMessages.MUST_NOT_BE_EMPTY),
                        new ValidationError("personLastName", ErrorMessages.MUST_NOT_BE_EMPTY),
                        new ValidationError("agreementDateFrom", ErrorMessages.MUST_NOT_BE_EMPTY),
                        new ValidationError("agreementDateTo", ErrorMessages.MUST_NOT_BE_EMPTY)
                )
        );
    }

    // ========== БИЛДЕР (делегирование к TestDataBuilder) ==========

    private static TestDataBuilder.RequestBuilder request() {
        return new TestDataBuilder.RequestBuilder();
    }

    // ========== СПЕЦИАЛЬНЫЕ ДАТЫ ==========

    public static final class SpecialDates {
        /**
         * Дата в високосном году (29 февраля 2024)
         */
        public static LocalDate leapYearDate() {
            return LocalDate.of(2024, 2, 29);
        }

        /**
         * Последний день года
         */
        public static LocalDate endOfYear() {
            return LocalDate.of(2023, 12, 31);
        }

        /**
         * Первый день года
         */
        public static LocalDate startOfYear() {
            return LocalDate.of(2024, 1, 1);
        }

        /**
         * Создает пару дат с переходом через границу года
         */
        public static LocalDate[] yearBoundaryDates() {
            return new LocalDate[]{
                    LocalDate.of(2023, 12, 30),
                    LocalDate.of(2024, 1, 5)
            };
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Создает список ошибок валидации
     */
    public static List<ValidationError> errors(ValidationError... errors) {
        return List.of(errors);
    }

    /**
     * Создает ValidationError
     */
    public static ValidationError error(String field, String message) {
        return new ValidationError(field, message);
    }
}