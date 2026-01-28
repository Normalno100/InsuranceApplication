package org.javaguru.travel.insurance.domain.model.valueobject;

import java.util.Objects;

/**
 * Value Object для кода риска
 * Type-safe обертка над строковым кодом
 */
public final class RiskCode {
    
    // Предопределенные коды рисков
    public static final RiskCode TRAVEL_MEDICAL = new RiskCode("TRAVEL_MEDICAL");
    public static final RiskCode SPORT_ACTIVITIES = new RiskCode("SPORT_ACTIVITIES");
    public static final RiskCode EXTREME_SPORT = new RiskCode("EXTREME_SPORT");
    public static final RiskCode PREGNANCY = new RiskCode("PREGNANCY");
    public static final RiskCode CHRONIC_DISEASES = new RiskCode("CHRONIC_DISEASES");
    public static final RiskCode ACCIDENT_COVERAGE = new RiskCode("ACCIDENT_COVERAGE");
    public static final RiskCode TRIP_CANCELLATION = new RiskCode("TRIP_CANCELLATION");
    public static final RiskCode LUGGAGE_LOSS = new RiskCode("LUGGAGE_LOSS");
    public static final RiskCode FLIGHT_DELAY = new RiskCode("FLIGHT_DELAY");
    public static final RiskCode CIVIL_LIABILITY = new RiskCode("CIVIL_LIABILITY");
    
    private final String value;
    
    public RiskCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk code cannot be null or empty");
        }
        this.value = value.toUpperCase().trim();
    }
    
    /**
     * Создает код риска из строки
     */
    public static RiskCode of(String value) {
        return new RiskCode(value);
    }
    
    /**
     * Проверяет, является ли риск обязательным
     */
    public boolean isMandatory() {
        return TRAVEL_MEDICAL.equals(this);
    }
    
    /**
     * Проверяет, является ли риск экстремальным спортом
     */
    public boolean isExtremeSport() {
        return EXTREME_SPORT.equals(this);
    }
    
    public String value() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RiskCode riskCode = (RiskCode) o;
        return value.equals(riskCode.value);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value;
    }
}
