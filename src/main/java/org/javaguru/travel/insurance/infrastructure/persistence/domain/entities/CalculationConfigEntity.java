package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * JPA Entity для глобальных настроек расчёта премии.
 *
 * Таблица calculation_config хранит ключ-значение конфигурации,
 * которую можно менять в БД без перезапуска приложения.
 *
 * Примеры ключей:
 *   AGE_COEFFICIENT_ENABLED — включить/выключить возрастной коэффициент (true/false)
 */
@Entity
@Table(name = "calculation_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CalculationConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальный ключ настройки (например, "AGE_COEFFICIENT_ENABLED").
     */
    @Column(name = "config_key", nullable = false, unique = true, length = 100)
    private String configKey;

    /**
     * Значение настройки (строка). Парсится в нужный тип в сервисе.
     */
    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    /**
     * Текстовое описание назначения настройки.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

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

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Проверяет активность настройки на указанную дату.
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
