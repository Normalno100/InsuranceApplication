package org.javaguru.travel.insurance.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.math.BigDecimal;

/**
 * Типы страховых рисков
 */
@Getter
@RequiredArgsConstructor
public enum RiskType {
    TRAVEL_MEDICAL("TRAVEL_MEDICAL", "Medical Coverage", "Медицинское покрытие",
            new BigDecimal("0"), true),
    SPORT_ACTIVITIES("SPORT_ACTIVITIES", "Sport Activities", "Активный спорт",
            new BigDecimal("0.3"), false),
    EXTREME_SPORT("EXTREME_SPORT", "Extreme Sport", "Экстремальный спорт",
            new BigDecimal("0.6"), false),
    PREGNANCY("PREGNANCY", "Pregnancy Coverage", "Покрытие беременности",
            new BigDecimal("0.2"), false),
    CHRONIC_DISEASES("CHRONIC_DISEASES", "Chronic Diseases", "Хронические заболевания",
            new BigDecimal("0.4"), false),
    ACCIDENT_COVERAGE("ACCIDENT_COVERAGE", "Accident Coverage", "От несчастных случаев",
            new BigDecimal("0.2"), false),
    TRIP_CANCELLATION("TRIP_CANCELLATION", "Trip Cancellation", "Отмена поездки",
            new BigDecimal("0.15"), false),
    LUGGAGE_LOSS("LUGGAGE_LOSS", "Luggage Loss", "Потеря багажа",
            new BigDecimal("0.1"), false),
    FLIGHT_DELAY("FLIGHT_DELAY", "Flight Delay", "Задержка рейса",
            new BigDecimal("0.05"), false),
    CIVIL_LIABILITY("CIVIL_LIABILITY", "Civil Liability", "Гражданская ответственность",
            new BigDecimal("0.1"), false);

    private final String code;
    private final String nameEn;
    private final String nameRu;
    private final BigDecimal coefficient;
    private final boolean mandatory;

    public static RiskType fromCode(String code) {
        for (RiskType risk : values()) {
            if (risk.code.equals(code)) {
                return risk;
            }
        }
        throw new IllegalArgumentException("Unknown risk type: " + code);
    }
}