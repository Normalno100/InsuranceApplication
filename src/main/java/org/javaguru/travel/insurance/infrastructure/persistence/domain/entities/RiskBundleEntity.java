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
 * ИСПРАВЛЕНИЕ: columnDefinition изменён с "jsonb" на "TEXT".
 *
 * ПРОБЛЕМА (было):
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(name = "required_risks", columnDefinition = "jsonb", nullable = false)
 *
 *   При INSERT PostgreSQL отказывался принять VARCHAR в jsonb-колонку:
 *     ERROR: column "required_risks" is of type jsonb
 *            but expression is of type character varying
 *
 * РЕШЕНИЕ (стало):
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(name = "required_risks", columnDefinition = "TEXT", nullable = false)
 *
 *   JsonStringConverter хранит JSON как обычную строку.
 *   PostgreSQL принимает TEXT без дополнительного приведения типов.
 *   RiskBundleService использует ObjectMapper для десериализации строки —
 *   поведение при чтении не меняется.
 *
 *   ПРИМЕЧАНИЕ ПО СХЕМЕ:
 *   Реальная колонка в БД была создана Liquibase с типом jsonb (009-create-advanced-pricing-tables.xml).
 *   Для production PostgreSQL нужна Liquibase-миграция:
 *     ALTER TABLE risk_bundles
 *       ALTER COLUMN required_risks TYPE TEXT USING required_risks::TEXT;
 *
 *   Также при смене типа потребуется удалить GIN-индекс (idx_bundle_risks),
 *   созданный в changeSet 009-02, так как GIN-индексация TEXT без явного оператора
 *   gin_trgm_ops не поддерживается стандартно.
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
     * ИСПРАВЛЕНО: columnDefinition = "TEXT" вместо "jsonb".
     * JsonStringConverter сериализует List<String> в JSON-строку через ObjectMapper
     * в RiskBundleService.parseRequiredRisks() — поведение при чтении не изменилось.
     */
    @Convert(converter = JsonStringConverter.class)
    @Column(name = "required_risks", columnDefinition = "TEXT", nullable = false)
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