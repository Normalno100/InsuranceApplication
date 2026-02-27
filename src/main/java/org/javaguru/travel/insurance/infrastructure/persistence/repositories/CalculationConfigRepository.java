package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CalculationConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Репозиторий для настроек расчёта.
 */
@Repository
public interface CalculationConfigRepository extends JpaRepository<CalculationConfigEntity, Long> {

    /**
     * Находит активную настройку по ключу на указанную дату.
     *
     * @param configKey ключ настройки (например, "AGE_COEFFICIENT_ENABLED")
     * @param date      дата, на которую нужна настройка
     * @return Optional с найденной настройкой или empty
     */
    @Query("SELECT c FROM CalculationConfigEntity c " +
            "WHERE c.configKey = :configKey " +
            "AND c.isActive = true " +
            "AND c.validFrom <= :date " +
            "AND (c.validTo IS NULL OR c.validTo >= :date)")
    Optional<CalculationConfigEntity> findActiveByKey(
            @Param("configKey") String configKey,
            @Param("date") LocalDate date
    );

    /**
     * Находит активную настройку по ключу на текущую дату.
     */
    default Optional<CalculationConfigEntity> findActiveByKey(String configKey) {
        return findActiveByKey(configKey, LocalDate.now());
    }
}
