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
 * Сущность уровня медицинского покрытия в базе данных.
 *
 * ИЗМЕНЕНИЯ task_117:
 * - Добавлено поле maxPayoutAmount — максимальная сумма страховой выплаты.
 */
@Entity
@Table(name = "medical_risk_limit_levels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MedicalRiskLimitLevelEntity implements TemporallyValid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 20)
    private String code;

    @Column(name = "coverage_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal coverageAmount;

    @Column(name = "daily_rate", nullable = false, precision = 8, scale = 2)
    private BigDecimal dailyRate;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    /**
     * Максимальная сумма страховой выплаты по медицинскому риску.
     *
     * task_117:
     *   NULL  → лимит не установлен, выплата = coverageAmount
     *   > 0   → выплата ограничена этим значением
     */
    @Column(name = "max_payout_amount", precision = 12, scale = 2)
    private BigDecimal maxPayoutAmount;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Возвращает эффективный лимит выплат:
     * - если maxPayoutAmount задан — возвращает его
     * - иначе — возвращает coverageAmount
     */
    public BigDecimal getEffectivePayoutLimit() {
        return maxPayoutAmount != null ? maxPayoutAmount : coverageAmount;
    }

    /**
     * Проверяет, установлен ли явный лимит выплат
     */
    public boolean hasExplicitPayoutLimit() {
        return maxPayoutAmount != null;
    }

    // isActiveOn(LocalDate) — унаследован от TemporallyValid

    /**
     * Проверяет, активен ли уровень сейчас
     */
    public boolean isActive() {
        return isActiveOn(LocalDate.now());
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (validFrom == null) {
            validFrom = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}