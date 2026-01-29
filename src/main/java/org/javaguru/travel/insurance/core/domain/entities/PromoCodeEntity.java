package org.javaguru.travel.insurance.core.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity для промо-кода
 * 
 * СОЗДАН в рамках Этапа 3 - Task 3.1
 * Соответствует таблице promo_codes в БД
 */
@Entity
@Table(name = "promo_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;  // PERCENTAGE, FIXED_AMOUNT

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "min_premium_amount", precision = 12, scale = 2)
    private BigDecimal minPremiumAmount;

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    private BigDecimal maxDiscountAmount;

    @Column(name = "applicable_countries", length = 255)
    private String applicableCountries;

    @Column(name = "applicable_risk_levels", length = 255)
    private String applicableRiskLevels;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "current_usage_count", nullable = false)
    private Integer currentUsageCount = 0;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Проверяет, активен ли промо-код на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        if (date.isBefore(validFrom)) {
            return false;
        }
        return !date.isAfter(validTo);
    }

    /**
     * Проверяет, можно ли использовать промо-код (не достигнут лимит)
     */
    public boolean canBeUsed() {
        if (maxUsageCount == null) {
            return true;
        }
        return currentUsageCount < maxUsageCount;
    }

    /**
     * Инкрементирует счётчик использования
     */
    public void incrementUsageCount() {
        this.currentUsageCount++;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (currentUsageCount == null) {
            currentUsageCount = 0;
        }
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
