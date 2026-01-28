package org.javaguru.travel.insurance.domain.model.entity;

import org.javaguru.travel.insurance.domain.model.valueobject.Coefficient;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain Entity для страны
 * Содержит бизнес-логику определения риска страны
 */
public class Country {
    
    private final CountryCode isoCode;
    private final String nameEn;
    private final String nameRu;
    private final RiskGroup riskGroup;
    private final Coefficient riskCoefficient;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    
    public Country(
            CountryCode isoCode,
            String nameEn,
            String nameRu,
            RiskGroup riskGroup,
            Coefficient riskCoefficient,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (isoCode == null) {
            throw new IllegalArgumentException("Country ISO code cannot be null");
        }
        if (nameEn == null || nameEn.trim().isEmpty()) {
            throw new IllegalArgumentException("Country name (EN) cannot be null or empty");
        }
        if (riskGroup == null) {
            throw new IllegalArgumentException("Risk group cannot be null");
        }
        if (riskCoefficient == null) {
            throw new IllegalArgumentException("Risk coefficient cannot be null");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("Valid from date cannot be null");
        }
        
        this.isoCode = isoCode;
        this.nameEn = nameEn;
        this.nameRu = nameRu;
        this.riskGroup = riskGroup;
        this.riskCoefficient = riskCoefficient;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    
    /**
     * Группа риска страны
     */
    public enum RiskGroup {
        LOW("Low risk"),
        MEDIUM("Medium risk"),
        HIGH("High risk"),
        VERY_HIGH("Very high risk");
        
        private final String description;
        
        RiskGroup(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
        
        public boolean isHighRisk() {
            return this == HIGH || this == VERY_HIGH;
        }
    }
    
    /**
     * Проверяет, активна ли страна на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }
    
    /**
     * Проверяет, доступна ли страна для страхования
     * (некоторые страны могут быть заблокированы)
     */
    public boolean isAvailableForInsurance() {
        // Страны с очень высоким риском могут требовать особого подхода
        return riskGroup != RiskGroup.VERY_HIGH;
    }
    
    /**
     * Проверяет, требуется ли ручная проверка для этой страны
     */
    public boolean requiresManualReview() {
        return riskGroup == RiskGroup.HIGH || riskGroup == RiskGroup.VERY_HIGH;
    }
    
    // Getters
    public CountryCode getIsoCode() {
        return isoCode;
    }
    
    public String getNameEn() {
        return nameEn;
    }
    
    public String getNameRu() {
        return nameRu;
    }
    
    public RiskGroup getRiskGroup() {
        return riskGroup;
    }
    
    public Coefficient getRiskCoefficient() {
        return riskCoefficient;
    }
    
    public LocalDate getValidFrom() {
        return validFrom;
    }
    
    public LocalDate getValidTo() {
        return validTo;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Country country = (Country) o;
        return isoCode.equals(country.isoCode);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(isoCode);
    }
    
    @Override
    public String toString() {
        return String.format("Country[%s, %s, %s, coeff=%s]",
            isoCode, nameEn, riskGroup, riskCoefficient);
    }
}
