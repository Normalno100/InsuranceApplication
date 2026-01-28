package org.javaguru.travel.insurance.domain.model.entity;

import org.javaguru.travel.insurance.domain.model.valueobject.Age;
import org.javaguru.travel.insurance.domain.model.valueobject.Coefficient;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain Entity для риска
 * Содержит бизнес-логику расчета коэффициентов и проверки доступности
 */
public class Risk {
    
    private final RiskCode code;
    private final String nameEn;
    private final String nameRu;
    private final Coefficient baseCoefficient;
    private final boolean mandatory;
    private final String description;
    private final LocalDate validFrom;
    private final LocalDate validTo;
    
    public Risk(
            RiskCode code,
            String nameEn,
            String nameRu,
            Coefficient baseCoefficient,
            boolean mandatory,
            String description,
            LocalDate validFrom,
            LocalDate validTo
    ) {
        if (code == null) {
            throw new IllegalArgumentException("Risk code cannot be null");
        }
        if (nameEn == null || nameEn.trim().isEmpty()) {
            throw new IllegalArgumentException("Risk name (EN) cannot be null or empty");
        }
        if (baseCoefficient == null) {
            throw new IllegalArgumentException("Base coefficient cannot be null");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("Valid from date cannot be null");
        }
        
        this.code = code;
        this.nameEn = nameEn;
        this.nameRu = nameRu;
        this.baseCoefficient = baseCoefficient;
        this.mandatory = mandatory;
        this.description = description;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }
    
    /**
     * Рассчитывает коэффициент с учетом возраста
     * В домене может быть базовая логика, специфичная логика - в сервисах
     */
    public Coefficient calculateCoefficient(Age age) {
        // Базовый коэффициент
        Coefficient result = baseCoefficient;
        
        // Экстремальный спорт дороже для пожилых (примерная логика)
        if (code.equals(RiskCode.EXTREME_SPORT)) {
            if (age.years() > 60) {
                result = result.multiply(Coefficient.of("1.5"));
            } else if (age.years() > 50) {
                result = result.multiply(Coefficient.of("1.3"));
            }
        }
        
        return result;
    }
    
    /**
     * Проверяет, доступен ли риск для указанного возраста
     */
    public boolean isAvailableFor(Age age) {
        // Экстремальный спорт не доступен для >70 лет
        if (code.equals(RiskCode.EXTREME_SPORT) && age.years() > 70) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Проверяет, активен ли риск на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }
    
    /**
     * Проверяет, является ли риск обязательным
     */
    public boolean isMandatory() {
        return mandatory;
    }
    
    // Getters
    public RiskCode getCode() {
        return code;
    }
    
    public String getNameEn() {
        return nameEn;
    }
    
    public String getNameRu() {
        return nameRu;
    }
    
    public Coefficient getBaseCoefficient() {
        return baseCoefficient;
    }
    
    public String getDescription() {
        return description;
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
        Risk risk = (Risk) o;
        return code.equals(risk.code);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
    
    @Override
    public String toString() {
        return String.format("Risk[%s, %s, coeff=%s]", code, nameEn, baseCoefficient);
    }
}
