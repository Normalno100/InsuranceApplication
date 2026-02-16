package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity для дефолтных дневных премий по странам
 * 
 * НАЗНАЧЕНИЕ:
 * Хранит базовую дневную ставку страхования для каждой страны.
 * Используется как альтернатива medical_risk_limit_levels для упрощенного расчета.
 * 
 * БИЗНЕС-ЛОГИКА:
 * - Каждая страна имеет свою базовую ставку (зависит от риска страны)
 * - Поддерживается версионирование через valid_from/valid_to
 * - Исторические данные сохраняются для аудита
 * 
 * ФОРМУЛА:
 * Premium = DefaultDayPremium × Days × AgeCoeff × RiskCoeffs × DurationCoeff
 */
@Entity
@Table(name = "country_default_day_premiums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryDefaultDayPremiumEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ISO код страны (2 символа)
     * Связь с таблицей countries
     */
    @Column(name = "country_iso_code", nullable = false, length = 2)
    private String countryIsoCode;

    /**
     * Дефолтная дневная премия
     * DECIMAL(8,2) обеспечивает точность для денежных расчетов
     */
    @Column(name = "default_day_premium", nullable = false, precision = 8, scale = 2)
    private BigDecimal defaultDayPremium;

    /**
     * Валюта (по умолчанию EUR)
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    /**
     * Описание/комментарий
     */
    @Column(name = "description", length = 255)
    private String description;

    // ============================================
    // Временная валидность (Temporal Validity)
    // ============================================

    /**
     * Дата начала действия тарифа
     * Позволяет планировать изменения тарифов заранее
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Дата окончания действия (NULL = бессрочно)
     * Позволяет хранить историю изменений тарифов
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    // ============================================
    // Audit fields
    // ============================================

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // ============================================
    // Business Logic Methods
    // ============================================

    /**
     * Проверяет, активен ли тариф на указанную дату
     * 
     * @param date дата проверки
     * @return true если тариф активен
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }

    /**
     * Проверяет, активен ли тариф на текущую дату
     * 
     * @return true если тариф активен сейчас
     */
    public boolean isActive() {
        return isActiveOn(LocalDate.now());
    }

    /**
     * Проверяет, является ли это текущий действующий тариф
     * (valid_from <= today AND (valid_to IS NULL OR valid_to >= today))
     * 
     * @return true если это текущий тариф
     */
    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return !validFrom.isAfter(today) && (validTo == null || !validTo.isBefore(today));
    }

    /**
     * Проверяет, является ли это будущий тариф
     * (valid_from > today)
     * 
     * @return true если тариф вступит в силу в будущем
     */
    public boolean isFuture() {
        return validFrom.isAfter(LocalDate.now());
    }

    /**
     * Проверяет, является ли это исторический тариф
     * (valid_to < today)
     * 
     * @return true если тариф уже не действует
     */
    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDate.now());
    }

    // ============================================
    // JPA Lifecycle Callbacks
    // ============================================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        if (validFrom == null) {
            validFrom = LocalDate.now();
        }
        
        if (currency == null || currency.trim().isEmpty()) {
            currency = "EUR";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ============================================
    // toString, equals, hashCode
    // ============================================

    @Override
    public String toString() {
        return String.format(
            "CountryDefaultDayPremium[id=%d, country=%s, premium=%s %s, validFrom=%s, validTo=%s]",
            id, countryIsoCode, defaultDayPremium, currency, validFrom, validTo
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CountryDefaultDayPremiumEntity)) return false;
        CountryDefaultDayPremiumEntity that = (CountryDefaultDayPremiumEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
