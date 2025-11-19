package org.javaguru.travel.insurance.core.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * Уровни медицинского покрытия и соответствующие базовые ставки
 */
@Getter
@RequiredArgsConstructor
public enum MedicalRiskLimitLevel {

    LEVEL_5000("5000", new BigDecimal("5000"), new BigDecimal("1.50")),
    LEVEL_10000("10000", new BigDecimal("10000"), new BigDecimal("2.00")),
    LEVEL_20000("20000", new BigDecimal("20000"), new BigDecimal("3.00")),
    LEVEL_50000("50000", new BigDecimal("50000"), new BigDecimal("4.50")),
    LEVEL_100000("100000", new BigDecimal("100000"), new BigDecimal("7.00")),
    LEVEL_200000("200000", new BigDecimal("200000"), new BigDecimal("12.00")),
    LEVEL_500000("500000", new BigDecimal("500000"), new BigDecimal("20.00"));

    private final String code;
    private final BigDecimal coverage;
    private final BigDecimal dailyRate;

    /**
     * Находит уровень покрытия по коду
     */
    public static MedicalRiskLimitLevel fromCode(String code) {
        for (MedicalRiskLimitLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Unknown medical risk limit level: " + code);
    }

    /**
     * Находит подходящий уровень покрытия по запрошенной сумме
     */
    public static MedicalRiskLimitLevel findByAmount(BigDecimal amount) {
        for (MedicalRiskLimitLevel level : values()) {
            if (amount.compareTo(level.coverage) <= 0) {
                return level;
            }
        }
        return LEVEL_500000; // максимальный уровень
    }
}