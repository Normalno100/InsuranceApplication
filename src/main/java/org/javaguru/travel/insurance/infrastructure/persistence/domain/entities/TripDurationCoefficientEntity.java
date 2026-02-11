package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_duration_coefficients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TripDurationCoefficientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "days_from", nullable = false)
    private Integer daysFrom;

    @Column(name = "days_to", nullable = false)
    private Integer daysTo;

    @Column(name = "coefficient", nullable = false, precision = 5, scale = 2)
    private BigDecimal coefficient;

    @Column(name = "description", length = 100)
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
     * Проверяет, подходит ли указанное количество дней в этот диапазон
     */
    public boolean isApplicableForDays(int days) {
        return days >= daysFrom && days <= daysTo;
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