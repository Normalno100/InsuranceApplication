package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.AgeCoefficientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для базовых возрастных коэффициентов.
 *
 * ДОБАВЛЕНО в task_113.
 */
@Repository
public interface AgeCoefficientRepository extends JpaRepository<AgeCoefficientEntity, Long> {

    /**
     * Находит активный коэффициент для указанного возраста на указанную дату.
     *
     * @param age  возраст в годах
     * @param date дата применения (обычно agreementDateFrom)
     * @return Optional с найденным коэффициентом или empty
     */
    @Query("SELECT a FROM AgeCoefficientEntity a " +
            "WHERE :age >= a.ageFrom " +
            "AND :age <= a.ageTo " +
            "AND a.validFrom <= :date " +
            "AND (a.validTo IS NULL OR a.validTo >= :date)")
    Optional<AgeCoefficientEntity> findCoefficientForAge(
            @Param("age") int age,
            @Param("date") LocalDate date
    );

    /**
     * Находит коэффициент для возраста на текущую дату.
     */
    default Optional<AgeCoefficientEntity> findCoefficientForAge(int age) {
        return findCoefficientForAge(age, LocalDate.now());
    }

    /**
     * Получает все активные коэффициенты на указанную дату, отсортированные по ageFrom.
     */
    @Query("SELECT a FROM AgeCoefficientEntity a " +
            "WHERE a.validFrom <= :date " +
            "AND (a.validTo IS NULL OR a.validTo >= :date) " +
            "ORDER BY a.ageFrom")
    List<AgeCoefficientEntity> findAllActive(@Param("date") LocalDate date);

    /**
     * Получает все активные коэффициенты на текущую дату.
     */
    default List<AgeCoefficientEntity> findAllActive() {
        return findAllActive(LocalDate.now());
    }
}
