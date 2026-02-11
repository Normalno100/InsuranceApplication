package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Сущность решения андеррайтинга
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

    // JSON поля
    @Type(JsonBinaryType.class)
    @Column(name = "rule_results", columnDefinition = "jsonb")
    private String ruleResults;

    @Type(JsonBinaryType.class)
    @Column(name = "request_data", columnDefinition = "jsonb")
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