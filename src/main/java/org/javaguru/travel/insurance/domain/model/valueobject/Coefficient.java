package org.javaguru.travel.insurance.domain.model.valueobject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Value Object для коэффициента
 * Используется для расчета премии (возраст, страна, длительность, риски)
 */
public final class Coefficient {
    
    private final BigDecimal value;
    
    public static final Coefficient ONE = new Coefficient(BigDecimal.ONE);
    public static final Coefficient ZERO = new Coefficient(BigDecimal.ZERO);
    
    public Coefficient(BigDecimal value) {
        if (value == null) {
            throw new IllegalArgumentException("Coefficient value cannot be null");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Coefficient cannot be negative: " + value);
        }
        this.value = value.setScale(4, RoundingMode.HALF_UP);
    }
    
    /**
     * Создает коэффициент из строки
     */
    public static Coefficient of(String value) {
        return new Coefficient(new BigDecimal(value));
    }
    
    /**
     * Создает коэффициент из double
     */
    public static Coefficient of(double value) {
        return new Coefficient(BigDecimal.valueOf(value));
    }
    
    /**
     * Умножает коэффициенты
     */
    public Coefficient multiply(Coefficient other) {
        return new Coefficient(this.value.multiply(other.value));
    }
    
    /**
     * Складывает коэффициенты
     */
    public Coefficient add(Coefficient other) {
        return new Coefficient(this.value.add(other.value));
    }
    
    /**
     * Проверяет, равен ли коэффициент единице
     */
    public boolean isOne() {
        return this.value.compareTo(BigDecimal.ONE) == 0;
    }
    
    /**
     * Проверяет, равен ли коэффициент нулю
     */
    public boolean isZero() {
        return this.value.compareTo(BigDecimal.ZERO) == 0;
    }
    
    /**
     * Проверяет, больше ли этот коэффициент единицы (увеличивает премию)
     */
    public boolean isIncreasing() {
        return this.value.compareTo(BigDecimal.ONE) > 0;
    }
    
    /**
     * Проверяет, меньше ли этот коэффициент единицы (уменьшает премию)
     */
    public boolean isDecreasing() {
        return this.value.compareTo(BigDecimal.ONE) < 0;
    }
    
    /**
     * Возвращает процент изменения относительно 1.0
     * Например: 1.30 → +30%, 0.85 → -15%
     */
    public BigDecimal getPercentageChange() {
        return this.value.subtract(BigDecimal.ONE)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }
    
    public BigDecimal value() {
        return value;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Coefficient that = (Coefficient) o;
        return value.compareTo(that.value) == 0;
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
    
    @Override
    public String toString() {
        return value.toString();
    }
}
