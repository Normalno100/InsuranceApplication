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
 */
@Entity
@Table(name = "country_default_day_premiums")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryDefaultDayPremiumEntity implements TemporallyValid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "country_iso_code", nullable = false, length = 2)
    private String countryIsoCode;

    @Column(name = "default_day_premium", nullable = false, precision = 8, scale = 2)
    private BigDecimal defaultDayPremium;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // isActiveOn(LocalDate) — унаследован от TemporallyValid

    /**
     * Проверяет, активен ли тариф на текущую дату
     */
    public boolean isActive() {
        return isActiveOn(LocalDate.now());
    }

    /**
     * Проверяет, является ли это текущий действующий тариф
     */
    public boolean isCurrent() {
        LocalDate today = LocalDate.now();
        return !validFrom.isAfter(today) && (validTo == null || !validTo.isBefore(today));
    }

    /**
     * Проверяет, является ли это будущий тариф
     */
    public boolean isFuture() {
        return validFrom.isAfter(LocalDate.now());
    }

    /**
     * Проверяет, является ли это исторический тариф
     */
    public boolean isExpired() {
        return validTo != null && validTo.isBefore(LocalDate.now());
    }

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