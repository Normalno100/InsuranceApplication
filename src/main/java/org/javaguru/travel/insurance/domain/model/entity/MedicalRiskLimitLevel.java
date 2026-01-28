package org.javaguru.travel.insurance.domain.model.entity;

import org.javaguru.travel.insurance.domain.model.valueobject.Currency;
import org.javaguru.travel.insurance.domain.model.valueobject.Premium;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain Entity для уровня медицинского покрытия
 * Содержит бизнес-логику определения покрытия
 */
public class MedicalRiskLimitLevel {
    
    private final String code;
    private final BigDecimal coverageAmount;
    private final BigDecimal dailyRate;
    private final Currency currency;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    
    public MedicalRiskLimitLevel(
            String code,
            BigDecimal coverageAmount,
            BigDecimal dailyRate,
            Currency currency,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (code == null || code.trim().isEmpty()) {
            throw new IllegalArgumentException("Level code cannot be null or empty");
        }
        if (coverageAmount == null || coverageAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Coverage amount must be positive");
        }
        if (dailyRate == null || dailyRate.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Daily rate must be positive");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("Valid from date cannot be null");
        }
        
        this.code = code;
        this.coverageAmount = coverageAmount;
        this.dailyRate = dailyRate;
        this.currency = currency;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    
    /**
     * Рассчитывает базовую премию за указанное количество дней
     */
    public Premium calculateBasePremium(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(days));
        return new Premium(amount, currency);
    }
    
    /**
     * Проверяет, активен ли уровень на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }
    
    /**
     * Проверяет, является ли это высокое покрытие
     */
    public boolean isHighCoverage() {
        return coverageAmount.compareTo(new BigDecimal("100000")) >= 0;
    }
    
    /**
     * Проверяет, является ли это очень высокое покрытие
     */
    public boolean isVeryHighCoverage() {
        return coverageAmount.compareTo(new BigDecimal("200000")) >= 0;
    }
    
    // Getters
    public String getCode() {
        return code;
    }
    
    public BigDecimal getCoverageAmount() {
        return coverageAmount;
    }
    
    public BigDecimal getDailyRate() {
        return dailyRate;
    }
    
    public Currency getCurrency() {
        return currency;
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
        MedicalRiskLimitLevel that = (MedicalRiskLimitLevel) o;
        return code.equals(that.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
    
    @Override
    public String toString() {
        return String.format("MedicalLevel[%s, coverage=%s %s, daily=%s]",
            code, coverageAmount, currency, dailyRate);
    }
}
