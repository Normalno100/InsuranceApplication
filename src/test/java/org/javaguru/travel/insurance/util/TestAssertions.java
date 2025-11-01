package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Кастомные assertion'ы для уменьшения дублирования в тестах
 *
 * Использование:
 * <pre>
 * import static org.javaguru.travel.insurance.util.TestAssertions.*;
 *
 * // В тестах
 * assertSuccessfulResponse(response);
 * assertHasError(response, "personFirstName", "Must not be empty!");
 * assertFieldsCopied(request, response);
 * </pre>
 */
public final class TestAssertions {

    private TestAssertions() {
        throw new AssertionError("Utility class should not be instantiated");
    }

    // ========================================
    // ASSERTION'Ы ДЛЯ RESPONSE
    // ========================================

    /**
     * Проверяет что ответ успешный (без ошибок)
     *
     * @param response ответ для проверки
     */
    public static void assertSuccessfulResponse(TravelCalculatePremiumResponse response) {
        assertNotNull(response, "Response should not be null");
        assertFalse(response.hasErrors(), "Response should not have errors");
        assertNull(response.getErrors(), "Errors list should be null for successful response");
    }

    /**
     * Проверяет что ответ содержит ошибки
     *
     * @param response ответ для проверки
     */
    public static void assertHasErrors(TravelCalculatePremiumResponse response) {
        assertNotNull(response, "Response should not be null");
        assertTrue(response.hasErrors(), "Response should have errors");
        assertNotNull(response.getErrors(), "Errors list should not be null");
        assertFalse(response.getErrors().isEmpty(), "Errors list should not be empty");
    }

    /**
     * Проверяет что ответ содержит указанное количество ошибок
     *
     * @param response ответ для проверки
     * @param expectedCount ожидаемое количество ошибок
     */
    public static void assertErrorCount(TravelCalculatePremiumResponse response, int expectedCount) {
        assertHasErrors(response);
        assertEquals(expectedCount, response.getErrors().size(),
                () -> String.format("Expected %d errors but got %d. Errors: %s",
                        expectedCount, response.getErrors().size(), formatErrors(response.getErrors())));
    }

    /**
     * Проверяет что ответ содержит ошибку для указанного поля
     *
     * @param response ответ для проверки
     * @param field имя поля
     */
    public static void assertHasErrorForField(TravelCalculatePremiumResponse response, String field) {
        assertHasErrors(response);
        boolean hasError = response.getErrors().stream()
                .anyMatch(error -> error.getField().equals(field));
        assertTrue(hasError,
                () -> String.format("Expected error for field '%s' but errors are: %s",
                        field, formatErrors(response.getErrors())));
    }

    /**
     * Проверяет что ответ содержит ошибку с указанным полем и сообщением
     *
     * @param response ответ для проверки
     * @param expectedField ожидаемое имя поля
     * @param expectedMessage ожидаемое сообщение об ошибке
     */
    public static void assertHasError(TravelCalculatePremiumResponse response,
                                      String expectedField,
                                      String expectedMessage) {
        assertHasErrors(response);
        boolean hasError = response.getErrors().stream()
                .anyMatch(error -> error.getField().equals(expectedField)
                        && error.getMessage().equals(expectedMessage));
        assertTrue(hasError,
                () -> String.format("Expected error with field='%s' and message='%s' but errors are: %s",
                        expectedField, expectedMessage, formatErrors(response.getErrors())));
    }

    /**
     * Проверяет что цена равна ожидаемой (long)
     *
     * @param response ответ для проверки
     * @param expectedPrice ожидаемая цена
     */
    public static void assertPrice(TravelCalculatePremiumResponse response, long expectedPrice) {
        assertNotNull(response.getAgreementPrice(), "Price should not be null");
        assertEquals(new BigDecimal(expectedPrice), response.getAgreementPrice(),
                () -> String.format("Expected price %d but got %s",
                        expectedPrice, response.getAgreementPrice()));
    }

    /**
     * Проверяет что цена равна ожидаемой (BigDecimal)
     *
     * @param response ответ для проверки
     * @param expectedPrice ожидаемая цена
     */
    public static void assertPrice(TravelCalculatePremiumResponse response, BigDecimal expectedPrice) {
        assertNotNull(response.getAgreementPrice(), "Price should not be null");
        assertEquals(0, expectedPrice.compareTo(response.getAgreementPrice()),
                () -> String.format("Expected price %s but got %s",
                        expectedPrice, response.getAgreementPrice()));
    }

    /**
     * Проверяет что цена null (для ответов с ошибками)
     *
     * @param response ответ для проверки
     */
    public static void assertPriceIsNull(TravelCalculatePremiumResponse response) {
        assertNull(response.getAgreementPrice(),
                "Price should be null for error response");
    }

    /**
     * Проверяет что все поля скопированы из запроса в ответ
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertFieldsCopied(TravelCalculatePremiumRequest request,
                                          TravelCalculatePremiumResponse response) {
        assertAll("All fields should be copied from request to response",
                () -> assertEquals(request.getPersonFirstName(), response.getPersonFirstName(),
                        "First name should be copied"),
                () -> assertEquals(request.getPersonLastName(), response.getPersonLastName(),
                        "Last name should be copied"),
                () -> assertEquals(request.getAgreementDateFrom(), response.getAgreementDateFrom(),
                        "Date from should be copied"),
                () -> assertEquals(request.getAgreementDateTo(), response.getAgreementDateTo(),
                        "Date to should be copied")
        );
    }

    /**
     * Проверяет что поля ответа null (для error response)
     *
     * @param response ответ для проверки
     */
    public static void assertFieldsAreNull(TravelCalculatePremiumResponse response) {
        assertAll("All fields should be null in error response",
                () -> assertNull(response.getPersonFirstName(), "First name should be null"),
                () -> assertNull(response.getPersonLastName(), "Last name should be null"),
                () -> assertNull(response.getAgreementDateFrom(), "Date from should be null"),
                () -> assertNull(response.getAgreementDateTo(), "Date to should be null"),
                () -> assertNull(response.getAgreementPrice(), "Price should be null")
        );
    }

    /**
     * Проверяет что поле firstName скопировано
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertFirstNameCopied(TravelCalculatePremiumRequest request,
                                             TravelCalculatePremiumResponse response) {
        assertEquals(request.getPersonFirstName(), response.getPersonFirstName(),
                "First name should be copied from request to response");
    }

    /**
     * Проверяет что поле lastName скопировано
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertLastNameCopied(TravelCalculatePremiumRequest request,
                                            TravelCalculatePremiumResponse response) {
        assertEquals(request.getPersonLastName(), response.getPersonLastName(),
                "Last name should be copied from request to response");
    }

    /**
     * Проверяет что поле dateFrom скопировано
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertDateFromCopied(TravelCalculatePremiumRequest request,
                                            TravelCalculatePremiumResponse response) {
        assertEquals(request.getAgreementDateFrom(), response.getAgreementDateFrom(),
                "Date from should be copied from request to response");
    }

    /**
     * Проверяет что поле dateTo скопировано
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertDateToCopied(TravelCalculatePremiumRequest request,
                                          TravelCalculatePremiumResponse response) {
        assertEquals(request.getAgreementDateTo(), response.getAgreementDateTo(),
                "Date to should be copied from request to response");
    }

    // ========================================
    // ASSERTION'Ы ДЛЯ СПИСКА ОШИБОК
    // ========================================

    /**
     * Проверяет что список ошибок пустой
     *
     * @param errors список ошибок
     */
    public static void assertNoErrors(List<ValidationError> errors) {
        assertNotNull(errors, "Errors list should not be null");
        assertTrue(errors.isEmpty(),
                () -> "Expected no errors but got: " + formatErrors(errors));
    }

    /**
     * Проверяет что список содержит указанное количество ошибок
     *
     * @param errors список ошибок
     * @param expectedCount ожидаемое количество
     */
    public static void assertErrorCount(List<ValidationError> errors, int expectedCount) {
        assertNotNull(errors, "Errors list should not be null");
        assertEquals(expectedCount, errors.size(),
                () -> String.format("Expected %d errors but got %d. Errors: %s",
                        expectedCount, errors.size(), formatErrors(errors)));
    }

    /**
     * Проверяет что список содержит ошибку для указанного поля
     *
     * @param errors список ошибок
     * @param field имя поля
     */
    public static void assertContainsErrorForField(List<ValidationError> errors, String field) {
        assertNotNull(errors, "Errors list should not be null");
        boolean hasError = errors.stream()
                .anyMatch(error -> error.getField().equals(field));
        assertTrue(hasError,
                () -> String.format("Expected error for field '%s' but errors are: %s",
                        field, formatErrors(errors)));
    }

    /**
     * Проверяет что список содержит ошибку с указанным полем и сообщением
     *
     * @param errors список ошибок
     * @param expectedField ожидаемое имя поля
     * @param expectedMessage ожидаемое сообщение
     */
    public static void assertContainsError(List<ValidationError> errors,
                                           String expectedField,
                                           String expectedMessage) {
        assertNotNull(errors, "Errors list should not be null");
        boolean hasError = errors.stream()
                .anyMatch(error -> error.getField().equals(expectedField)
                        && error.getMessage().equals(expectedMessage));
        assertTrue(hasError,
                () -> String.format("Expected error with field='%s' and message='%s' but errors are: %s",
                        expectedField, expectedMessage, formatErrors(errors)));
    }

    /**
     * Проверяет что список НЕ содержит ошибку для указанного поля
     *
     * @param errors список ошибок
     * @param field имя поля
     */
    public static void assertDoesNotContainErrorForField(List<ValidationError> errors, String field) {
        assertNotNull(errors, "Errors list should not be null");
        boolean hasError = errors.stream()
                .anyMatch(error -> error.getField().equals(field));
        assertFalse(hasError,
                () -> String.format("Did not expect error for field '%s' but found one", field));
    }

    /**
     * Проверяет что в списке есть хотя бы одна ошибка
     *
     * @param errors список ошибок
     */
    public static void assertHasErrors(List<ValidationError> errors) {
        assertNotNull(errors, "Errors list should not be null");
        assertFalse(errors.isEmpty(), "Errors list should not be empty");
    }

    // ========================================
    // КОМБИНИРОВАННЫЕ ASSERTION'Ы
    // ========================================

    /**
     * Полная проверка успешного ответа:
     * - нет ошибок
     * - поля скопированы
     * - цена правильная
     *
     * @param request запрос
     * @param response ответ
     * @param expectedPrice ожидаемая цена
     */
    public static void assertCompleteSuccessfulResponse(TravelCalculatePremiumRequest request,
                                                        TravelCalculatePremiumResponse response,
                                                        long expectedPrice) {
        assertAll("Complete successful response validation",
                () -> assertSuccessfulResponse(response),
                () -> assertFieldsCopied(request, response),
                () -> assertPrice(response, expectedPrice)
        );
    }

    /**
     * Полная проверка ответа с ошибкой:
     * - есть ошибки
     * - поля null
     * - цена null
     *
     * @param response ответ
     * @param expectedErrorCount ожидаемое количество ошибок
     */
    public static void assertCompleteErrorResponse(TravelCalculatePremiumResponse response,
                                                   int expectedErrorCount) {
        assertAll("Complete error response validation",
                () -> assertErrorCount(response, expectedErrorCount),
                () -> assertFieldsAreNull(response),
                () -> assertPriceIsNull(response)
        );
    }

    /**
     * Проверка ответа с одной ошибкой валидации
     *
     * @param response ответ
     * @param expectedField ожидаемое поле ошибки
     * @param expectedMessage ожидаемое сообщение
     */
    public static void assertSingleValidationError(TravelCalculatePremiumResponse response,
                                                   String expectedField,
                                                   String expectedMessage) {
        assertAll("Single validation error",
                () -> assertErrorCount(response, 1),
                () -> assertHasError(response, expectedField, expectedMessage),
                () -> assertFieldsAreNull(response),
                () -> assertPriceIsNull(response)
        );
    }

    /**
     * Проверка успешного ответа только с проверкой полей (без цены)
     *
     * @param request запрос
     * @param response ответ
     */
    public static void assertSuccessfulResponseWithoutPrice(TravelCalculatePremiumRequest request,
                                                            TravelCalculatePremiumResponse response) {
        assertAll("Successful response without price check",
                () -> assertSuccessfulResponse(response),
                () -> assertFieldsCopied(request, response)
        );
    }

    // ========================================
    // ASSERTION'Ы ДЛЯ ДОПОЛНИТЕЛЬНЫХ ПРОВЕРОК
    // ========================================

    /**
     * Проверяет что первая ошибка содержит указанное поле и сообщение
     *
     * @param response ответ
     * @param expectedField ожидаемое поле
     * @param expectedMessage ожидаемое сообщение
     */
    public static void assertFirstError(TravelCalculatePremiumResponse response,
                                        String expectedField,
                                        String expectedMessage) {
        assertHasErrors(response);
        ValidationError error = response.getErrors().get(0);
        assertAll("First error validation",
                () -> assertEquals(expectedField, error.getField(),
                        "First error field mismatch"),
                () -> assertEquals(expectedMessage, error.getMessage(),
                        "First error message mismatch")
        );
    }

    /**
     * Проверяет что ответ не null
     *
     * @param response ответ
     */
    public static void assertResponseNotNull(TravelCalculatePremiumResponse response) {
        assertNotNull(response, "Response should not be null");
    }

    /**
     * Проверяет тип цены (должна быть BigDecimal)
     *
     * @param response ответ
     */
    public static void assertPriceType(TravelCalculatePremiumResponse response) {
        assertNotNull(response.getAgreementPrice(), "Price should not be null");
        assertTrue(response.getAgreementPrice() instanceof BigDecimal,
                "Price should be instance of BigDecimal");
    }

    /**
     * Проверяет тип даты (должна быть LocalDate)
     *
     * @param response ответ
     */
    public static void assertDateTypes(TravelCalculatePremiumResponse response) {
        assertAll("Date types validation",
                () -> assertTrue(response.getAgreementDateFrom() instanceof LocalDate,
                        "Date from should be instance of LocalDate"),
                () -> assertTrue(response.getAgreementDateTo() instanceof LocalDate,
                        "Date to should be instance of LocalDate")
        );
    }

    /**
     * Проверяет что scale цены равен 0
     *
     * @param response ответ
     */
    public static void assertPriceScale(TravelCalculatePremiumResponse response) {
        assertNotNull(response.getAgreementPrice(), "Price should not be null");
        assertEquals(0, response.getAgreementPrice().scale(),
                "Price scale should be 0");
    }

    /**
     * Проверяет что цена положительная или 0
     *
     * @param response ответ
     */
    public static void assertPriceNotNegative(TravelCalculatePremiumResponse response) {
        assertNotNull(response.getAgreementPrice(), "Price should not be null");
        assertTrue(response.getAgreementPrice().compareTo(BigDecimal.ZERO) >= 0,
                "Price should not be negative");
    }

    /**
     * Проверяет что dateTo >= dateFrom
     *
     * @param response ответ
     */
    public static void assertDateToAfterOrEqualDateFrom(TravelCalculatePremiumResponse response) {
        assertNotNull(response.getAgreementDateFrom(), "Date from should not be null");
        assertNotNull(response.getAgreementDateTo(), "Date to should not be null");
        assertFalse(response.getAgreementDateTo().isBefore(response.getAgreementDateFrom()),
                "Date to should be after or equal to date from");
    }

    // ========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Получает первую ошибку из ответа
     *
     * @param response ответ
     * @return первая ошибка
     */
    public static ValidationError getFirstError(TravelCalculatePremiumResponse response) {
        assertHasErrors(response);
        return response.getErrors().get(0);
    }

    /**
     * Получает ошибку по индексу
     *
     * @param response ответ
     * @param index индекс ошибки
     * @return ошибка
     */
    public static ValidationError getError(TravelCalculatePremiumResponse response, int index) {
        assertHasErrors(response);
        assertTrue(index >= 0 && index < response.getErrors().size(),
                () -> String.format("Index %d is out of bounds for errors list of size %d",
                        index, response.getErrors().size()));
        return response.getErrors().get(index);
    }

    /**
     * Форматирует список ошибок для вывода в сообщениях об ошибках
     *
     * @param errors список ошибок
     * @return отформатированная строка
     */
    private static String formatErrors(List<ValidationError> errors) {
        if (errors == null || errors.isEmpty()) {
            return "[]";
        }

        StringBuilder sb = new StringBuilder("[\n");
        for (int i = 0; i < errors.size(); i++) {
            ValidationError error = errors.get(i);
            sb.append(String.format("  [%d] field='%s', message='%s'",
                    i, error.getField(), error.getMessage()));
            if (i < errors.size() - 1) {
                sb.append(",");
            }
            sb.append("\n");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Выводит информацию об ошибках для отладки
     * (полезно при разработке тестов)
     *
     * @param response ответ
     */
    public static void printErrors(TravelCalculatePremiumResponse response) {
        if (response.hasErrors()) {
            System.out.println("Errors in response:");
            System.out.println(formatErrors(response.getErrors()));
        } else {
            System.out.println("No errors in response");
        }
    }

    /**
     * Выводит полную информацию об ответе для отладки
     *
     * @param response ответ
     */
    public static void printResponse(TravelCalculatePremiumResponse response) {
        System.out.println("=== Response Details ===");
        System.out.println("First Name: " + response.getPersonFirstName());
        System.out.println("Last Name: " + response.getPersonLastName());
        System.out.println("Date From: " + response.getAgreementDateFrom());
        System.out.println("Date To: " + response.getAgreementDateTo());
        System.out.println("Price: " + response.getAgreementPrice());
        System.out.println("Has Errors: " + response.hasErrors());
        if (response.hasErrors()) {
            System.out.println("Errors: " + formatErrors(response.getErrors()));
        }
        System.out.println("========================");
    }
}
