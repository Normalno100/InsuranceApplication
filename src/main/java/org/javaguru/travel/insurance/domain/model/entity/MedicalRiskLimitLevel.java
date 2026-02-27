package org.javaguru.travel.insurance.domain.model.entity;

import org.javaguru.travel.insurance.domain.model.valueobject.Currency;
import org.javaguru.travel.insurance.domain.model.valueobject.Premium;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Domain Entity для уровня медицинского покрытия.
 *
 * ИЗМЕНЕНИЯ task_117:
 * - Добавлен maxPayoutAmount — максимальная сумма выплаты
 * - Добавлены методы getEffectivePayoutLimit(), isPayoutLimitApplicable()
 */
public class MedicalRiskLimitLevel {

    private final String code;
    private final BigDecimal coverageAmount;
    private final BigDecimal dailyRate;
    private final Currency currency;

    /**
     * task_117: Максимальная сумма страховой выплаты.
     * null = не установлен, используется coverageAmount.
     */
    private final BigDecimal maxPayoutAmount;

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
        this(code, coverageAmount, dailyRate, currency, null, validFrom, validTo);
    }

    public MedicalRiskLimitLevel(
            String code,
            BigDecimal coverageAmount,
            BigDecimal dailyRate,
            Currency currency,
            BigDecimal maxPayoutAmount,
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
        if (maxPayoutAmount != null && maxPayoutAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Max payout amount must be positive if specified");
        }

        this.code = code;
        this.coverageAmount = coverageAmount;
        this.dailyRate = dailyRate;
        this.currency = currency;
        this.maxPayoutAmount = maxPayoutAmount;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }

    /**
     * Рассчитывает базовую премию за указанное количество дней.
     */
    public Premium calculateBasePremium(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive");
        }
        BigDecimal amount = dailyRate.multiply(BigDecimal.valueOf(days));
        return new Premium(amount, currency);
    }

    /**
     * task_117: Возвращает эффективный лимит выплат.
     * Если maxPayoutAmount задан — возвращает его, иначе coverageAmount.
     */
    public BigDecimal getEffectivePayoutLimit() {
        return maxPayoutAmount != null ? maxPayoutAmount : coverageAmount;
    }

    /**
     * task_117: Проверяет, применяется ли ограничение выплат
     * (т.е. maxPayoutAmount < coverageAmount).
     */
    public boolean isPayoutLimitApplicable() {
        if (maxPayoutAmount == null) {
            return false;
        }
        return maxPayoutAmount.compareTo(coverageAmount) < 0;
    }

    /**
     * Проверяет, активен ли уровень на указанную дату.
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }

    /**
     * Проверяет, является ли это высокое покрытие.
     */
    public boolean isHighCoverage() {
        return coverageAmount.compareTo(new BigDecimal("100000")) >= 0;
    }

    /**
     * Проверяет, является ли это очень высокое покрытие.
     */
    public boolean isVeryHighCoverage() {
        return coverageAmount.compareTo(new BigDecimal("200000")) >= 0;
    }

    // Getters
    public String getCode() { return code; }
    public BigDecimal getCoverageAmount() { return coverageAmount; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public Currency getCurrency() { return currency; }
    public BigDecimal getMaxPayoutAmount() { return maxPayoutAmount; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }

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
        return String.format("MedicalLevel[%s, coverage=%s %s, daily=%s, maxPayout=%s]",
                code, coverageAmount, currency, dailyRate,
                maxPayoutAmount != null ? maxPayoutAmount : "none");
    }
}