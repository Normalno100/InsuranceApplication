package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javaguru.travel.insurance.infrastructure.persistence.converter.JsonStringConverter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity для пакетов рисков.
 *
 * task_133: Заменён @Type(JsonBinaryType.class) на @Convert(converter = JsonStringConverter.class)
 * для совместимости с H2 в тестах.
 *
 * БЫЛО:
 *   @Type(JsonBinaryType.class)
 *   @Column(name = "required_risks", columnDefinition = "jsonb", nullable = false)
 *
 * СТАЛО:
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(name = "required_risks", columnDefinition = "jsonb", nullable = false)
 *
 * columnDefinition = "jsonb" сохранён, чтобы:
 *   1. В PostgreSQL (production) schema validation проходил без ошибок.
 *   2. В H2 (tests) — H2 в режиме MODE=PostgreSQL понимает jsonb как OTHER/VARCHAR.
 *      Hibernate создаёт колонку с типом jsonb, H2 транслирует это в VARCHAR.
 */
@Entity
@Table(name = "risk_bundles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiskBundleEntity implements TemporallyValid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "name_en", nullable = false, length = 100)
    private String nameEn;

    @Column(name = "name_ru", length = 100)
    private String nameRu;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "discount_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercentage;

    /**
     * JSON массив требуемых рисков.
     * Например: ["SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"]
     *
     * task_133: @Type(JsonBinaryType.class) заменён на @Convert(converter = JsonStringConverter.class).
     * columnDefinition = "jsonb" оставлен — соответствует реальному типу колонки в PostgreSQL,
     * поэтому schema validation не падает. В H2 MODE=PostgreSQL jsonb транслируется в VARCHAR.
     */
    @Convert(converter = JsonStringConverter.class)
    @Column(name = "required_risks", columnDefinition = "jsonb", nullable = false)
    private String requiredRisks;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to")
    private LocalDate validTo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // isActiveOn(LocalDate) — унаследован от TemporallyValid

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (validFrom == null) {
            validFrom = LocalDate.now();
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