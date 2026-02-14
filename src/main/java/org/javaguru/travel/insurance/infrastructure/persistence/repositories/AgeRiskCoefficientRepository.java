package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.AgeRiskCoefficientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.RepositoryDefinition;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgeRiskCoefficientRepository extends JpaRepository<AgeRiskCoefficientEntity, Long> {

    /**
     * Находит активный модификатор для риска и возраста
     */
    @Query("SELECT a FROM AgeRiskCoefficientEntity a " +
            "WHERE a.riskTypeCode = :riskCode " +
            "AND :age >= a.ageFrom " +
            "AND :age <= a.ageTo " +
            "AND a.validFrom <= :date " +
            "AND (a.validTo IS NULL OR a.validTo >= :date)")
    Optional<AgeRiskCoefficientEntity> findModifierForRiskAndAge(
            @Param("riskCode") String riskCode,
            @Param("age") int age,
            @Param("date") LocalDate date
    );

    /**
     * Находит модификатор на текущую дату
     */
    default Optional<AgeRiskCoefficientEntity> findModifierForRiskAndAge(
            String riskCode,
            int age) {
        return findModifierForRiskAndAge(riskCode, age, LocalDate.now());
    }

    /**
     * Получает все активные модификаторы для указанного риска
     */
    @Query("SELECT a FROM AgeRiskCoefficientEntity a " +
            "WHERE a.riskTypeCode = :riskCode " +
            "AND a.validFrom <= :date " +
            "AND (a.validTo IS NULL OR a.validTo >= :date) " +
            "ORDER BY a.ageFrom")
    List<AgeRiskCoefficientEntity> findAllActiveForRisk(
            @Param("riskCode") String riskCode,
            @Param("date") LocalDate date
    );

    /**
     * Получает все активные модификаторы для риска на текущую дату
     */
    default List<AgeRiskCoefficientEntity> findAllActiveForRisk(String riskCode) {
        return findAllActiveForRisk(riskCode, LocalDate.now());
    }

    /**
     * Получает все активные модификаторы на указанную дату
     */
    @Query("SELECT a FROM AgeRiskCoefficientEntity a " +
            "WHERE a.validFrom <= :date " +
            "AND (a.validTo IS NULL OR a.validTo >= :date) " +
            "ORDER BY a.riskTypeCode, a.ageFrom")
    List<AgeRiskCoefficientEntity> findAllActive(@Param("date") LocalDate date);

    /**
     * Получает все активные модификаторы на текущую дату
     */
    default List<AgeRiskCoefficientEntity> findAllActive() {
        return findAllActive(LocalDate.now());
    }
}
