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
 * JPA Entity для скидок (групповые, корпоративные, сезонные, программа лояльности).
 *
 * Соответствует таблице discounts, созданной в 002-06-create-discounts.xml.
 *
 * РЕФАКТОРИНГ (п. 3.3 плана):
 *   Заменяет hardcoded Map<String, Discount> в DiscountService.
 *   Скидки теперь читаются из БД, что позволяет менять условия скидок
 *   без редеплоя приложения.
 */
@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный код скидки (например, GROUP_5, CORPORATE, LOYALTY_10).
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Человекочитаемое название скидки.
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * Описание скидки (опционально).
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Тип скидки: GROUP, CORPORATE, SEASONAL, LOYALTY.
     */
    @Column(name = "discount_type", nullable = false, length = 20)
    private String discountType;

    /**
     * Размер скидки в процентах (например, 10 = 10%).
     */
    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * Минимальное количество застрахованных лиц для применения скидки.
     * Используется для GROUP скидок. null = не требуется.
     */
    @Column(name = "min_persons_count")
    private Integer minPersonsCount;

    /**
     * Минимальная сумма премии для применения скидки.
     * null = нет минимума.
     */
    @Column(name = "min_premium_amount", precision = 12, scale = 2)
    private BigDecimal minPremiumAmount;

    /**
     * Дата начала действия скидки (включительно).
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Дата окончания действия скидки (включительно).
     * null = бессрочно.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /**
     * Флаг активности. false = скидка отключена без удаления из БД.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Проверяет, активна ли скидка на указанную дату.
     */
    public boolean isActiveOn(LocalDate date) {
        if (!Boolean.TRUE.equals(isActive)) {
            return false;
        }
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}