package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.TripDurationCoefficientEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripDurationCoefficientRepository
        extends JpaRepository<TripDurationCoefficientEntity, Long> {

    /**
     * Находит активный коэффициент для указанного количества дней
     */
    @Query("SELECT t FROM TripDurationCoefficientEntity t " +
            "WHERE :days >= t.daysFrom " +
            "AND :days <= t.daysTo " +
            "AND t.validFrom <= :date " +
            "AND (t.validTo IS NULL OR t.validTo >= :date) " +
            "ORDER BY t.daysFrom DESC")
    Optional<TripDurationCoefficientEntity> findCoefficientForDays(
            @Param("days") int days,
            @Param("date") LocalDate date
    );

    /**
     * Находит коэффициент для дней на текущую дату
     */
    default Optional<TripDurationCoefficientEntity> findCoefficientForDays(int days) {
        return findCoefficientForDays(days, LocalDate.now());
    }

    /**
     * Получает все активные коэффициенты на указанную дату
     */
    @Query("SELECT t FROM TripDurationCoefficientEntity t " +
            "WHERE t.validFrom <= :date " +
            "AND (t.validTo IS NULL OR t.validTo >= :date) " +
            "ORDER BY t.daysFrom")
    List<TripDurationCoefficientEntity> findAllActive(@Param("date") LocalDate date);

    /**
     * Получает все активные коэффициенты на текущую дату
     */
    default List<TripDurationCoefficientEntity> findAllActive() {
        return findAllActive(LocalDate.now());
    }
}