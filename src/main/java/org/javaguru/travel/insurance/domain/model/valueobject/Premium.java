package org.javaguru.travel.insurance.domain.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object для премии (страховой суммы)
 * Immutable, содержит бизнес-логику валидации и операций
 */
public final class Premium {
    
    private final BigDecimal amount;
    private final Currency currency;
    
    // Минимальная премия по бизнес-правилам
    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");
    
    public Premium(BigDecimal amount, Currency currency) {
        if (amount == null) {
            throw new IllegalArgumentException("Premium amount cannot be null");
        }
        if (currency == null) {
            throw new IllegalArgumentException("Currency cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Premium amount cannot be negative: " + amount);
        }
        
        this.amount = amount.setScale(2, RoundingMode.HALF_UP);
        this.currency = currency;
    }
    
    /**
     * Создает премию с валютой по умолчанию (EUR)
     */
    public static Premium of(BigDecimal amount) {
        return new Premium(amount, Currency.EUR);
    }
    
    /**
     * Создает премию из строки
     */
    public static Premium of(String amount, Currency currency) {
        return new Premium(new BigDecimal(amount), currency);
    }
    
    /**
     * Создает нулевую премию
     */
    public static Premium zero(Currency currency) {
        return new Premium(BigDecimal.ZERO, currency);
    }
    
    /**
     * Складывает две премии
     * @throws IllegalArgumentException если валюты не совпадают
     */
    public Premium add(Premium other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot add premiums with different currencies: %s and %s",
                    this.currency, other.currency)
            );
        }
        return new Premium(this.amount.add(other.amount), this.currency);
    }
    
    /**
     * Вычитает премию
     */
    public Premium subtract(Premium other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                String.format("Cannot subtract premiums with different currencies: %s and %s",
                    this.currency, other.currency)
            );
        }
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) {
            result = BigDecimal.ZERO;
        }
        return new Premium(result, this.currency);
    }
    
    /**
     * Умножает премию на коэффициент
     */
    public Premium multiply(Coefficient coefficient) {
        return new Premium(this.amount.multiply(coefficient.value()), this.currency);
    }
    
    /**
     * Умножает премию на число
     */
    public Premium multiply(BigDecimal multiplier) {
        return new Premium(this.amount.multiply(multiplier), this.currency);
    }
    
    /**
     * Применяет минимальную премию
     */
    public Premium applyMinimum() {
        if (this.amount.compareTo(MIN_PREMIUM) < 0) {
            return new Premium(MIN_PREMIUM, this.currency);
        }
        return this;
    }
    
    /**
     * Проверяет, является ли премия нулевой
     */
    public boolean isZero() {
        return this.amount.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Проверяет, больше ли эта премия другой
     */
    public boolean isGreaterThan(Premium other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare premiums with different currencies");
        }
        return this.amount.compareTo(other.amount) > 0;
    }
    
    /**
     * Проверяет, меньше ли эта премия другой
     */
    public boolean isLessThan(Premium other) {
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare premiums with different currencies");
        }
        return this.amount.compareTo(other.amount) < 0;
    }
    
    // Getters
    public BigDecimal amount() {
        return amount;
    }
    
    public Currency currency() {
        return currency;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Premium premium = (Premium) o;
        return amount.compareTo(premium.amount) == 0 && currency == premium.currency;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(amount, currency);
    }

    @Override
    public String toString() {
        return String.format(java.util.Locale.ROOT, "%.2f %s", amount, currency);
    }
}
