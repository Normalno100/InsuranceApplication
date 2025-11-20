package org.javaguru.travel.insurance.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Типы дополнительных рисков
 */
@Getter
@RequiredArgsConstructor
public enum RiskType {

    /**
     * Базовый медицинский риск - всегда включен
     */
    TRAVEL_MEDICAL(
            "TRAVEL_MEDICAL",
            "Medical Coverage",
            "Медицинское покрытие",
            new BigDecimal("0"),
            true
    ),

    /**
     * Активный спорт (лыжи, сноуборд, дайвинг)
     */
    SPORT_ACTIVITIES(
            "SPORT_ACTIVITIES",
            "Sport Activities",
            "Активный спорт",
            new BigDecimal("0.3"),
            false
    ),

    /**
     * Экстремальный спорт (альпинизм, парашют)
     */
    EXTREME_SPORT(
            "EXTREME_SPORT",
            "Extreme Sport",
            "Экстремальный спорт",
            new BigDecimal("0.6"),
            false
    ),

    /**
     * Беременность до 31 недели
     */
    PREGNANCY(
            "PREGNANCY",
            "Pregnancy Coverage",
            "Покрытие беременности",
            new BigDecimal("0.2"),
            false
    ),

    /**
     * Хронические заболевания
     */
    CHRONIC_DISEASES(
            "CHRONIC_DISEASES",
            "Chronic Diseases",
            "Хронические заболевания",
            new BigDecimal("0.4"),
            false
    ),

    /**
     * Расширенное покрытие ДТП и травм
     */
    ACCIDENT_COVERAGE(
            "ACCIDENT_COVERAGE",
            "Accident Coverage",
            "Покрытие от несчастных случаев",
            new BigDecimal("0.2"),
            false
    ),

    /**
     * Отмена поездки
     */
    TRIP_CANCELLATION(
            "TRIP_CANCELLATION",
            "Trip Cancellation",
            "Отмена поездки",
            new BigDecimal("0.15"),
            false
    ),

    /**
     * Потеря багажа
     */
    LUGGAGE_LOSS(
            "LUGGAGE_LOSS",
            "Luggage Loss",
            "Потеря багажа",
            new BigDecimal("0.1"),
            false
    ),

    /**
     * Задержка рейса
     */
    FLIGHT_DELAY(
            "FLIGHT_DELAY",
            "Flight Delay",
            "Задержка рейса",
            new BigDecimal("0.05"),
            false
    ),

    /**
     * Гражданская ответственность
     */
    CIVIL_LIABILITY(
            "CIVIL_LIABILITY",
            "Civil Liability",
            "Гражданская ответственность",
            new BigDecimal("0.1"),
            false
    );

    private final String code;
    private final String nameEn;
    private final String nameRu;
    private final BigDecimal coefficient;
    private final boolean mandatory;

    /**
     * Поиск риска по коду
     */
    public static RiskType fromCode(String code) {
        for (RiskType risk : values()) {
            if (risk.code.equals(code)) {
                return risk;
            }
        }
        throw new IllegalArgumentException("Unknown risk type: " + code);
    }

    /**
     * Получить все обязательные риски
     */
    public static RiskType[] getMandatoryRisks() {
        return java.util.Arrays.stream(values())
                .filter(RiskType::isMandatory)
                .toArray(RiskType[]::new);
    }

    /**
     * Получить все опциональные риски
     */
    public static RiskType[] getOptionalRisks() {
        return java.util.Arrays.stream(values())
                .filter(risk -> !risk.isMandatory())
                .toArray(RiskType[]::new);
    }
}