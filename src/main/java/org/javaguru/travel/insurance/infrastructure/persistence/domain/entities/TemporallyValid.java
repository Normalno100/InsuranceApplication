package org.javaguru.travel.insurance.infrastructure.persistence.domain.entities;

import java.time.LocalDate;

/**
 * Интерфейс для JPA entity-классов с поддержкой temporal validity.
 *
 * РЕФАКТОРИНГ (п. 4.1): Устранение дублирования isActiveOn в 8 Entity-классах.
 *
 * ДО: идентичный метод isActiveOn(LocalDate) был скопирован в 8 классах:
 *   AgeCoefficientEntity, AgeRiskCoefficientEntity, CountryDefaultDayPremiumEntity,
 *   CountryEntity, MedicalRiskLimitLevelEntity, RiskBundleEntity,
 *   RiskTypeEntity, TripDurationCoefficientEntity.
 *
 * ПОСЛЕ: каждый класс объявляет implements TemporallyValid.
 *   Геттеры getValidFrom() / getValidTo() уже присутствуют в каждом классе
 *   (генерируются Lombok @Getter), поэтому дополнительный код не требуется.
 */
public interface TemporallyValid {

    /**
     * Дата начала действия записи (включительно).
     */
    LocalDate getValidFrom();

    /**
     * Дата окончания действия записи (включительно), или {@code null} если бессрочно.
     */
    LocalDate getValidTo();

    /**
     * Проверяет, активна ли запись на указанную дату.
     *
     * <p>Запись активна, если:
     * <ul>
     *   <li>{@code date >= validFrom}</li>
     *   <li>{@code validTo == null} (бессрочно) или {@code date <= validTo}</li>
     * </ul>
     *
     * @param date дата проверки (обычно agreementDateFrom)
     * @return {@code true} если запись активна на указанную дату
     */
    default boolean isActiveOn(LocalDate date) {
        if (date.isBefore(getValidFrom())) {
            return false;
        }
        return getValidTo() == null || !date.isAfter(getValidTo());
    }
}