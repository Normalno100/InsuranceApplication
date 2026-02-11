package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Упрощённый набор утилит для тестов
 * Только самое необходимое - билдеры базовых объектов
 */
public class SimplifiedTestUtils {

    // ========== REQUEST BUILDERS ==========

    /**
     * Базовый валидный запрос для большинства тестов
     */
    public static TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с дополнительными рисками
     */
    public static TravelCalculatePremiumRequest requestWithRisks(String... risks) {
        var request = validRequest();
        request.setSelectedRisks(List.of(risks));
        return request;
    }

    /**
     * Запрос для группы
     */
    public static TravelCalculatePremiumRequest requestForGroup(int count) {
        var request = validRequest();
        request.setPersonsCount(count);
        return request;
    }

    /**
     * Запрос с промо-кодом
     */
    public static TravelCalculatePremiumRequest requestWithPromo(String code) {
        var request = validRequest();
        request.setPromoCode(code);
        return request;
    }

    /**
     * Корпоративный запрос
     */
    public static TravelCalculatePremiumRequest corporateRequest() {
        var request = validRequest();
        request.setIsCorporate(true);
        return request;
    }

    // ========== ASSERTION HELPERS ==========

    /**
     * Проверка, что премия в разумных пределах
     */
    public static boolean isReasonablePremium(BigDecimal premium) {
        return premium.compareTo(BigDecimal.ZERO) > 0
                && premium.compareTo(new BigDecimal("10000")) < 0;
    }

    /**
     * Проверка округления до 2 знаков
     */
    public static boolean hasTwoDecimals(BigDecimal value) {
        return value.scale() == 2;
    }

    // ========== DATE HELPERS ==========

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    public static LocalDate nextWeek() {
        return LocalDate.now().plusWeeks(1);
    }

    public static LocalDate birthDateForAge(int age) {
        return LocalDate.now().minusYears(age);
    }
}