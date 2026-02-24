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
 * JPA Entity для базовых возрастных коэффициентов.
 */
@Entity
@Table(name = "age_coefficients")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AgeCoefficientEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Нижняя граница возрастного диапазона (включительно).
     */
    @Column(name = "age_from", nullable = false)
    private Integer ageFrom;

    /**
     * Верхняя граница возрастного диапазона (включительно).
     */
    @Column(name = "age_to", nullable = false)
    private Integer ageTo;

    /**
     * Коэффициент премии для данного возрастного диапазона.
     * 1.0 = без изменений, 1.5 = +50%, 0.9 = -10%.
     */
    @Column(name = "coefficient", nullable = false, precision = 5, scale = 4)
    private BigDecimal coefficient;

    /**
     * Описание возрастной группы (например "Young adults", "Elderly").
     */
    @Column(name = "description", length = 255)
    private String description;

    /**
     * Дата начала действия коэффициента.
     */
    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    /**
     * Дата окончания действия коэффициента (NULL = бессрочно).
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * Проверяет, попадает ли указанный возраст в этот диапазон.
     */
    public boolean isApplicableForAge(int age) {
        return age >= ageFrom && age <= ageTo;
    }

    /**
     * Проверяет, активен ли коэффициент на указанную дату.
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
