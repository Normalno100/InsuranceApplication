package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с типами рисков
 */
public interface RiskTypeRepository extends JpaRepository<RiskTypeEntity, Long> {

    /**
     * Проверяет существование типа риска по коду
     */
    boolean existsByCode(String code);

    /**
     * Находит активный тип риска по коду на указанную дату
     */
    @Query("SELECT r FROM RiskTypeEntity r " +
            "WHERE r.code = :code " +
            "AND r.validFrom <= :date " +
            "AND (r.validTo IS NULL OR r.validTo >= :date)")
    Optional<RiskTypeEntity> findActiveByCode(
            @Param("code") String code,
            @Param("date") LocalDate date
    );

    /**
     * Находит активный тип риска по коду на текущую дату
     */
    default Optional<RiskTypeEntity> findActiveByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    /**
     * Получает все активные типы рисков на текущую дату
     */
    @Query("SELECT r FROM RiskTypeEntity r " +
            "WHERE r.validFrom <= CURRENT_DATE " +
            "AND (r.validTo IS NULL OR r.validTo >= CURRENT_DATE) " +
            "ORDER BY r.isMandatory DESC, r.code")
    List<RiskTypeEntity> findAllActive();

    /**
     * Получает все обязательные риски
     */
    @Query("SELECT r FROM RiskTypeEntity r " +
            "WHERE r.isMandatory = true " +
            "AND r.validFrom <= CURRENT_DATE " +
            "AND (r.validTo IS NULL OR r.validTo >= CURRENT_DATE)")
    List<RiskTypeEntity> findAllMandatory();

    /**
     * Получает все опциональные риски
     */
    @Query("SELECT r FROM RiskTypeEntity r " +
            "WHERE r.isMandatory = false " +
            "AND r.validFrom <= CURRENT_DATE " +
            "AND (r.validTo IS NULL OR r.validTo >= CURRENT_DATE) " +
            "ORDER BY r.code")
    List<RiskTypeEntity> findAllOptional();
}