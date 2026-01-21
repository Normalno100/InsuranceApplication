package org.javaguru.travel.insurance.core.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "age_risk_coefficients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgeRiskCoefficientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_type_code", nullable = false, length = 50)
    private String riskTypeCode;

    @Column(name = "age_from", nullable = false)
    private Integer ageFrom;

    @Column(name = "age_to", nullable = false)
    private Integer ageTo;

    /**
     * Модификатор коэффициента
     * 1.0 = без изменений
     * 1.5 = +50% к стоимости риска
     * 0.8 = -20% к стоимости риска
     */
    @Column(name = "coefficient_modifier", nullable = false, precision = 5, scale = 2)
    private BigDecimal coefficientModifier;

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

    /**
     * Проверяет, попадает ли указанный возраст в этот диапазон
     */
    public boolean isApplicableForAge(int age) {
        return age >= ageFrom && age <= ageTo;
    }

    /**
     * Проверяет, активен ли коэффициент на указанную дату
     */
    public boolean isActiveOn(LocalDate date) {
        if (date.isBefore(validFrom)) {
            return false;
        }
        return validTo == null || !date.isAfter(validTo);
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
