package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javaguru.travel.insurance.infrastructure.persistence.converter.JsonStringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность решения андеррайтинга.
 *
 * task_133: Заменён @Type(JsonBinaryType.class) на @Convert(converter = JsonStringConverter.class)
 * для совместимости с H2 в тестах.
 *
 * ИСПРАВЛЕНИЕ: columnDefinition изменён с "jsonb" на "TEXT".
 *
 * ПРОБЛЕМА (было):
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(name = "rule_results", columnDefinition = "jsonb")
 *
 *   JsonStringConverter сериализует значение как обычную Java-строку (VARCHAR/TEXT).
 *   PostgreSQL отказывался принять её в колонку типа jsonb без явного каста:
 *     ERROR: column "request_data" is of type jsonb
 *            but expression is of type character varying
 *
 * РЕШЕНИЕ (стало):
 *   @Convert(converter = JsonStringConverter.class)
 *   @Column(name = "rule_results", columnDefinition = "TEXT")
 *
 *   В PostgreSQL TEXT — это обычная строка без проверки JSON-структуры.
 *   Данные хранятся идентично (UTF-8 текст), индексирование через GIN недоступно,
 *   но для аудит-лога решений это приемлемо.
 *
 *   ПРИМЕЧАНИЕ ПО СХЕМЕ:
 *   Реальные колонки в БД были созданы Liquibase с типом jsonb (007-create-underwriting-tables.xml).
 *   Изменение columnDefinition в Entity не меняет существующую схему в PostgreSQL —
 *   оно используется только при ddl-auto=create/create-drop (тесты с H2) и
 *   при schema validation (hibernate.ddl-auto=validate).
 *
 *   Для production PostgreSQL нужна отдельная Liquibase-миграция,
 *   изменяющая тип колонки с jsonb на text:
 *     ALTER TABLE underwriting_decisions
 *       ALTER COLUMN rule_results TYPE TEXT USING rule_results::TEXT,
 *       ALTER COLUMN request_data TYPE TEXT USING request_data::TEXT;
 *
 *   ИЛИ (если менять схему нежелательно) — убрать columnDefinition совсем,
 *   тогда Hibernate использует стандартный тип TEXT/VARCHAR(255) в зависимости от диалекта.
 */
@Entity
@Table(name = "underwriting_decisions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UnderwritingDecisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private UUID requestId;

    // Персональные данные
    @Column(name = "person_first_name", nullable = false, length = 100)
    private String personFirstName;

    @Column(name = "person_last_name", nullable = false, length = 100)
    private String personLastName;

    @Column(name = "person_birth_date", nullable = false)
    private LocalDate personBirthDate;

    // Детали поездки
    @Column(name = "country_iso_code", nullable = false, length = 2)
    private String countryIsoCode;

    @Column(name = "agreement_date_from", nullable = false)
    private LocalDate agreementDateFrom;

    @Column(name = "agreement_date_to", nullable = false)
    private LocalDate agreementDateTo;

    // Решение
    @Column(name = "decision", nullable = false, length = 50)
    private String decision;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "review_reason", columnDefinition = "TEXT")
    private String reviewReason;

    /**
     * JSON с результатами применения правил андеррайтинга.
     *
     * ИСПРАВЛЕНО: columnDefinition = "TEXT" вместо "jsonb".
     * JsonStringConverter хранит строку — PostgreSQL принимает TEXT без приведения типов.
     */
    @Convert(converter = JsonStringConverter.class)
    @Column(name = "rule_results", columnDefinition = "TEXT")
    private String ruleResults;

    /**
     * JSON с исходными данными запроса.
     *
     * ИСПРАВЛЕНО: columnDefinition = "TEXT" вместо "jsonb".
     */
    @Convert(converter = JsonStringConverter.class)
    @Column(name = "request_data", columnDefinition = "TEXT")
    private String requestData;

    // Метрики
    @Column(name = "evaluation_duration_ms")
    private Integer evaluationDurationMs;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (requestId == null) {
            requestId = UUID.randomUUID();
        }
    }
}