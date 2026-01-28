package org.javaguru.travel.insurance.domain.model.valueobject;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Value Object для возраста
 * Инкапсулирует бизнес-правила валидации возраста
 */
public final class Age {
    
    private static final int MIN_AGE = 0;
    private static final int MAX_AGE = 80;
    
    private final int years;
    
    public Age(int years) {
        if (years < MIN_AGE) {
            throw new IllegalArgumentException("Age cannot be negative: " + years);
        }
        if (years > MAX_AGE) {
            throw new IllegalArgumentException(
                String.format("Age %d exceeds maximum allowed age of %d", years, MAX_AGE)
            );
        }
        this.years = years;
    }
    
    /**
     * Создает Age из даты рождения и референсной даты
     */
    public static Age fromBirthDate(LocalDate birthDate, LocalDate referenceDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        if (birthDate.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
        
        int years = Period.between(birthDate, referenceDate).getYears();
        return new Age(years);
    }
    
    /**
     * Возвращает описание возрастной группы
     */
    public String getAgeGroupDescription() {
        if (years <= 5) {
            return "Infants and toddlers";
        } else if (years <= 17) {
            return "Children and teenagers";
        } else if (years <= 30) {
            return "Young adults";
        } else if (years <= 40) {
            return "Adults";
        } else if (years <= 50) {
            return "Middle-aged";
        } else if (years <= 60) {
            return "Senior";
        } else if (years <= 70) {
            return "Elderly";
        } else {
            return "Very elderly";
        }
    }
    
    /**
     * Проверяет, доступен ли экстремальный спорт для этого возраста
     */
    public boolean isExtremeSportAvailable() {
        return years <= 70;
    }
    
    /**
     * Проверяет, требуется ли ручная проверка для этого возраста
     */
    public boolean requiresManualReview() {
        return years >= 75;
    }
    
    public int years() {
        return years;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Age age = (Age) o;
        return years == age.years;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(years);
    }
    
    @Override
    public String toString() {
        return years + " years";
    }
}
