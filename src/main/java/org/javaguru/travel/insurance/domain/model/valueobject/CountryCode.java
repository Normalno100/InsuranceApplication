package org.javaguru.travel.insurance.domain.model.valueobject;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object для ISO кода страны
 * Валидирует формат (2 буквы, заглавные)
 */
public final class CountryCode {
    
    private static final Pattern ISO_CODE_PATTERN = Pattern.compile("^[A-Z]{2}$");
    
    private final String value;
    
    public CountryCode(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Country code cannot be null or empty");
        }
        
        String normalized = value.toUpperCase().trim();
        
        if (!ISO_CODE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                "Invalid country ISO code format: " + value + 
                ". Expected 2 uppercase letters (e.g., 'US', 'GB', 'FR')"
            );
        }
        
        this.value = normalized;
    }
    
    /**
     * Создает код страны из строки
     */
    public static CountryCode of(String value) {
        return new CountryCode(value);
    }
    
    public String value() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CountryCode that = (CountryCode) o;
        return value.equals(that.value);
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
