package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.RiskBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RiskBundleRepository extends JpaRepository<RiskBundleEntity, Long> {

    /**
     * Находит активный пакет по коду
     */
    @Query("SELECT r FROM RiskBundleEntity r " +
            "WHERE r.code = :code " +
            "AND r.isActive = true " +
            "AND r.validFrom <= :date " +
            "AND (r.validTo IS NULL OR r.validTo >= :date)")
    Optional<RiskBundleEntity> findActiveByCode(
            @Param("code") String code,
            @Param("date") LocalDate date
    );

    /**
     * Находит активный пакет по коду на текущую дату
     */
    default Optional<RiskBundleEntity> findActiveByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    /**
     * Получает все активные пакеты на указанную дату
     */
    @Query("SELECT r FROM RiskBundleEntity r " +
            "WHERE r.isActive = true " +
            "AND r.validFrom <= :date " +
            "AND (r.validTo IS NULL OR r.validTo >= :date) " +
            "ORDER BY r.discountPercentage DESC")
    List<RiskBundleEntity> findAllActive(@Param("date") LocalDate date);

    /**
     * Получает все активные пакеты на текущую дату
     */
    default List<RiskBundleEntity> findAllActive() {
        return findAllActive(LocalDate.now());
    }

    /**
     * Проверяет существование пакета по коду
     */
    boolean existsByCode(String code);
}