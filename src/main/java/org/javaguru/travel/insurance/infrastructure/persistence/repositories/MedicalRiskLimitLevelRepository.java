package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.MedicalRiskLimitLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с уровнями медицинского покрытия
 */
@Repository
public interface MedicalRiskLimitLevelRepository extends JpaRepository<MedicalRiskLimitLevelEntity, Long> {

    /**
     * Проверяет существование уровня покрытия по коду
     */
    boolean existsByCode(String code);

    /**
     * Находит активный уровень покрытия по коду на указанную дату
     */
    @Query("SELECT m FROM MedicalRiskLimitLevelEntity m " +
            "WHERE m.code = :code " +
            "AND m.validFrom <= :date " +
            "AND (m.validTo IS NULL OR m.validTo >= :date)")
    Optional<MedicalRiskLimitLevelEntity> findActiveByCode(
            @Param("code") String code,
            @Param("date") LocalDate date
    );

    /**
     * Находит активный уровень покрытия по коду на текущую дату
     */
    default Optional<MedicalRiskLimitLevelEntity> findActiveByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    /**
     * Получает все активные уровни покрытия на текущую дату
     */
    @Query("SELECT m FROM MedicalRiskLimitLevelEntity m " +
            "WHERE m.validFrom <= CURRENT_DATE " +
            "AND (m.validTo IS NULL OR m.validTo >= CURRENT_DATE) " +
            "ORDER BY m.coverageAmount")
    List<MedicalRiskLimitLevelEntity> findAllActive();
}