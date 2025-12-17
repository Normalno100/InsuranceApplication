package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Утилиты для assertions в тестах
 * Содержит специализированные методы проверки для доменных объектов
 */
public class TestAssertions {

    // ========== RESPONSE V2 ASSERTIONS ==========

    /**
     * Проверяет, что ответ содержит ошибки
     */
    public static void assertHasErrors(TravelCalculatePremiumResponseV2 response) {
        assertNotNull(response, "Response should not be null");
        assertTrue(response.hasErrors(), "Response should have errors");
        assertNotNull(response.getErrors(), "Errors list should not be null");
        assertFalse(response.getErrors().isEmpty(), "Errors list should not be empty");
    }

    /**
     * Проверяет, что ответ не содержит ошибок
     */
    public static void assertNoErrors(TravelCalculatePremiumResponseV2 response) {
        assertNotNull(response, "Response should not be null");
        assertFalse(response.hasErrors(), "Response should not have errors");
    }

    /**
     * Проверяет, что ответ содержит конкретную ошибку
     */
    public static void assertHasError(TravelCalculatePremiumResponseV2 response,
                                      String field,
                                      String message) {
        assertHasErrors(response);

        boolean found = response.getErrors().stream()
                .anyMatch(e -> field.equals(e.getField()) && message.equals(e.getMessage()));

        assertTrue(found,
                String.format("Expected error not found: field='%s', message='%s'", field, message));
    }

    /**
     * Проверяет, что ответ содержит ошибку для указанного поля
     */
    public static void assertHasErrorForField(TravelCalculatePremiumResponseV2 response,
                                              String field) {
        assertHasErrors(response);

        boolean found = response.getErrors().stream()
                .anyMatch(e -> field.equals(e.getField()));

        assertTrue(found, String.format("Expected error for field '%s' not found", field));
    }

    /**
     * Проверяет, что ответ содержит ошибку с сообщением, содержащим текст
     */
    public static void assertHasErrorContaining(TravelCalculatePremiumResponseV2 response,
                                                String field,
                                                String messageFragment) {
        assertHasErrors(response);

        boolean found = response.getErrors().stream()
                .anyMatch(e -> field.equals(e.getField()) &&
                        e.getMessage().contains(messageFragment));

        assertTrue(found,
                String.format("Expected error not found: field='%s', message containing='%s'",
                        field, messageFragment));
    }

    /**
     * Проверяет количество ошибок
     */
    public static void assertErrorCount(TravelCalculatePremiumResponseV2 response,
                                        int expectedCount) {
        assertHasErrors(response);
        assertEquals(expectedCount, response.getErrors().size(),
                "Unexpected number of errors");
    }

    /**
     * Проверяет, что ответ успешный (без ошибок)
     */
    public static void assertSuccessfulResponse(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertNotNull(response.getAgreementPrice(), "Agreement price should not be null");
        assertNotNull(response.getCurrency(), "Currency should not be null");
    }

    /**
     * Проверяет базовые поля ответа
     */
    public static void assertBasicResponseFields(TravelCalculatePremiumResponseV2 response,
                                                 String firstName,
                                                 String lastName,
                                                 LocalDate dateFrom,
                                                 LocalDate dateTo,
                                                 String countryIsoCode) {
        assertNoErrors(response);
        assertEquals(firstName, response.getPersonFirstName());
        assertEquals(lastName, response.getPersonLastName());
        assertEquals(dateFrom, response.getAgreementDateFrom());
        assertEquals(dateTo, response.getAgreementDateTo());
        assertEquals(countryIsoCode, response.getCountryIsoCode());
    }

    /**
     * Проверяет премию
     */
    public static void assertPremium(TravelCalculatePremiumResponseV2 response,
                                     BigDecimal expectedPremium) {
        assertNoErrors(response);
        assertNotNull(response.getAgreementPrice());
        assertEquals(0, expectedPremium.compareTo(response.getAgreementPrice()),
                String.format("Expected premium %s but got %s",
                        expectedPremium, response.getAgreementPrice()));
    }

    /**
     * Проверяет премию с точностью
     */
    public static void assertPremiumWithTolerance(TravelCalculatePremiumResponseV2 response,
                                                  BigDecimal expectedPremium,
                                                  BigDecimal tolerance) {
        assertNoErrors(response);
        assertNotNull(response.getAgreementPrice());

        BigDecimal diff = response.getAgreementPrice().subtract(expectedPremium).abs();
        assertTrue(diff.compareTo(tolerance) <= 0,
                String.format("Premium %s is not within tolerance %s of expected %s",
                        response.getAgreementPrice(), tolerance, expectedPremium));
    }

    /**
     * Проверяет, что премия больше минимальной
     */
    public static void assertPremiumGreaterThan(TravelCalculatePremiumResponseV2 response,
                                                BigDecimal minPremium) {
        assertNoErrors(response);
        assertNotNull(response.getAgreementPrice());
        assertTrue(response.getAgreementPrice().compareTo(minPremium) > 0,
                String.format("Premium %s should be greater than %s",
                        response.getAgreementPrice(), minPremium));
    }

    /**
     * Проверяет наличие скидок
     */
    public static void assertHasDiscounts(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertTrue(response.hasDiscounts(), "Response should have discounts");
        assertNotNull(response.getDiscountAmount());
        assertTrue(response.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0,
                "Discount amount should be positive");
    }

    /**
     * Проверяет отсутствие скидок
     */
    public static void assertNoDiscounts(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertFalse(response.hasDiscounts(), "Response should not have discounts");
    }

    /**
     * Проверяет сумму скидки
     */
    public static void assertDiscountAmount(TravelCalculatePremiumResponseV2 response,
                                            BigDecimal expectedDiscount) {
        assertHasDiscounts(response);
        assertEquals(0, expectedDiscount.compareTo(response.getDiscountAmount()),
                String.format("Expected discount %s but got %s",
                        expectedDiscount, response.getDiscountAmount()));
    }

    /**
     * Проверяет наличие промо-кода
     */
    public static void assertHasPromoCode(TravelCalculatePremiumResponseV2 response,
                                          String expectedCode) {
        assertNoErrors(response);
        assertTrue(response.hasPromoCode(), "Response should have promo code");
        assertNotNull(response.getPromoCodeInfo());
        assertEquals(expectedCode, response.getPromoCodeInfo().getCode());
    }

    /**
     * Проверяет отсутствие промо-кода
     */
    public static void assertNoPromoCode(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertFalse(response.hasPromoCode(), "Response should not have promo code");
    }

    /**
     * Проверяет валюту
     */
    public static void assertCurrency(TravelCalculatePremiumResponseV2 response,
                                      String expectedCurrency) {
        assertNoErrors(response);
        assertEquals(expectedCurrency, response.getCurrency());
    }

    /**
     * Проверяет количество дней
     */
    public static void assertDays(TravelCalculatePremiumResponseV2 response,
                                  int expectedDays) {
        assertNoErrors(response);
        assertEquals(expectedDays, response.getAgreementDays());
    }

    /**
     * Проверяет возраст
     */
    public static void assertAge(TravelCalculatePremiumResponseV2 response,
                                 int expectedAge) {
        assertNoErrors(response);
        assertEquals(expectedAge, response.getPersonAge());
    }

    /**
     * Проверяет наличие деталей расчета
     */
    public static void assertHasCalculationDetails(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertNotNull(response.getCalculation(), "Calculation details should not be null");
        assertNotNull(response.getCalculation().getBaseRate());
        assertNotNull(response.getCalculation().getAgeCoefficient());
        assertNotNull(response.getCalculation().getCountryCoefficient());
    }

    /**
     * Проверяет наличие деталей по рискам
     */
    public static void assertHasRiskDetails(TravelCalculatePremiumResponseV2 response) {
        assertNoErrors(response);
        assertNotNull(response.getRiskPremiums(), "Risk premiums should not be null");
        assertFalse(response.getRiskPremiums().isEmpty(), "Risk premiums should not be empty");
    }

    /**
     * Проверяет количество выбранных рисков
     */
    public static void assertRiskCount(TravelCalculatePremiumResponseV2 response,
                                       int expectedCount) {
        assertNoErrors(response);
        assertEquals(expectedCount, response.getSelectedRisksCount());
    }

    // ========== VALIDATION ERROR ASSERTIONS ==========

    /**
     * Проверяет ValidationError
     */
    public static void assertValidationError(ValidationError error,
                                             String expectedField,
                                             String expectedMessage) {
        assertNotNull(error, "Validation error should not be null");
        assertEquals(expectedField, error.getField(), "Field name mismatch");
        assertEquals(expectedMessage, error.getMessage(), "Error message mismatch");
    }

    /**
     * Проверяет, что ValidationError содержит поле
     */
    public static void assertValidationErrorField(ValidationError error,
                                                  String expectedField) {
        assertNotNull(error, "Validation error should not be null");
        assertEquals(expectedField, error.getField(), "Field name mismatch");
    }

    /**
     * Проверяет, что ValidationError содержит сообщение с текстом
     */
    public static void assertValidationErrorMessageContains(ValidationError error,
                                                            String messageFragment) {
        assertNotNull(error, "Validation error should not be null");
        assertNotNull(error.getMessage(), "Error message should not be null");
        assertTrue(error.getMessage().contains(messageFragment),
                String.format("Error message '%s' should contain '%s'",
                        error.getMessage(), messageFragment));
    }

    /**
     * Проверяет список ValidationError на наличие конкретной ошибки
     */
    public static void assertContainsError(List<ValidationError> errors,
                                           String field,
                                           String message) {
        assertNotNull(errors, "Errors list should not be null");
        assertFalse(errors.isEmpty(), "Errors list should not be empty");

        boolean found = errors.stream()
                .anyMatch(e -> field.equals(e.getField()) && message.equals(e.getMessage()));

        assertTrue(found,
                String.format("Expected error not found: field='%s', message='%s'", field, message));
    }

    /**
     * Проверяет список ValidationError на наличие ошибки для поля
     */
    public static void assertContainsErrorForField(List<ValidationError> errors,
                                                   String field) {
        assertNotNull(errors, "Errors list should not be null");
        assertFalse(errors.isEmpty(), "Errors list should not be empty");

        boolean found = errors.stream()
                .anyMatch(e -> field.equals(e.getField()));

        assertTrue(found,
                String.format("Expected error for field '%s' not found", field));
    }

    // ========== BIGDECIMAL ASSERTIONS ==========

    /**
     * Проверяет равенство BigDecimal
     */
    public static void assertBigDecimalEquals(BigDecimal expected, BigDecimal actual) {
        assertNotNull(expected, "Expected value should not be null");
        assertNotNull(actual, "Actual value should not be null");
        assertEquals(0, expected.compareTo(actual),
                String.format("Expected %s but got %s", expected, actual));
    }

    /**
     * Проверяет, что BigDecimal положительный
     */
    public static void assertBigDecimalPositive(BigDecimal value) {
        assertNotNull(value, "Value should not be null");
        assertTrue(value.compareTo(BigDecimal.ZERO) > 0,
                String.format("Value %s should be positive", value));
    }

    /**
     * Проверяет, что BigDecimal неотрицательный
     */
    public static void assertBigDecimalNonNegative(BigDecimal value) {
        assertNotNull(value, "Value should not be null");
        assertTrue(value.compareTo(BigDecimal.ZERO) >= 0,
                String.format("Value %s should be non-negative", value));
    }

    /**
     * Проверяет, что первое значение больше второго
     */
    public static void assertBigDecimalGreaterThan(BigDecimal value, BigDecimal threshold) {
        assertNotNull(value, "Value should not be null");
        assertNotNull(threshold, "Threshold should not be null");
        assertTrue(value.compareTo(threshold) > 0,
                String.format("Value %s should be greater than %s", value, threshold));
    }

    /**
     * Проверяет, что первое значение меньше второго
     */
    public static void assertBigDecimalLessThan(BigDecimal value, BigDecimal threshold) {
        assertNotNull(value, "Value should not be null");
        assertNotNull(threshold, "Threshold should not be null");
        assertTrue(value.compareTo(threshold) < 0,
                String.format("Value %s should be less than %s", value, threshold));
    }

    // ========== DATE ASSERTIONS ==========

    /**
     * Проверяет, что первая дата раньше второй
     */
    public static void assertDateBefore(LocalDate date, LocalDate compareDate) {
        assertNotNull(date, "Date should not be null");
        assertNotNull(compareDate, "Compare date should not be null");
        assertTrue(date.isBefore(compareDate),
                String.format("Date %s should be before %s", date, compareDate));
    }

    /**
     * Проверяет, что первая дата позже второй
     */
    public static void assertDateAfter(LocalDate date, LocalDate compareDate) {
        assertNotNull(date, "Date should not be null");
        assertNotNull(compareDate, "Compare date should not be null");
        assertTrue(date.isAfter(compareDate),
                String.format("Date %s should be after %s", date, compareDate));
    }

    /**
     * Проверяет, что даты равны
     */
    public static void assertDateEquals(LocalDate expected, LocalDate actual) {
        assertNotNull(expected, "Expected date should not be null");
        assertNotNull(actual, "Actual date should not be null");
        assertEquals(expected, actual,
                String.format("Expected date %s but got %s", expected, actual));
    }
}