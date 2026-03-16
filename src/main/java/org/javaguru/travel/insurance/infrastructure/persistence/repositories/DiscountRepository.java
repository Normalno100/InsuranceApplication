package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.DiscountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы со скидками.
 *
 * РЕФАКТОРИНГ (п. 3.3 плана):
 *   Создан для замены hardcoded Map<String, Discount> в DiscountService.
 */
@Repository
public interface DiscountRepository extends JpaRepository<DiscountEntity, Long> {

    /**
     * Находит скидку по коду (без учёта активности/дат).
     */
    Optional<DiscountEntity> findByCode(String code);

    /**
     * Находит все активные скидки на указанную дату.
     * Используется для кеширования всех скидок при инициализации.
     */
    @Query("SELECT d FROM DiscountEntity d " +
            "WHERE d.isActive = true " +
            "AND d.validFrom <= :date " +
            "AND (d.validTo IS NULL OR d.validTo >= :date)")
    List<DiscountEntity> findAllActiveOnDate(@Param("date") LocalDate date);

    /**
     * Находит все активные скидки на текущую дату.
     */
    default List<DiscountEntity> findAllActive() {
        return findAllActiveOnDate(LocalDate.now());
    }

    /**
     * Находит все активные скидки определённого типа на указанную дату.
     * Используется для фильтрации по типу (GROUP, CORPORATE, SEASONAL, LOYALTY).
     */
    @Query("SELECT d FROM DiscountEntity d " +
            "WHERE d.discountType = :discountType " +
            "AND d.isActive = true " +
            "AND d.validFrom <= :date " +
            "AND (d.validTo IS NULL OR d.validTo >= :date)")
    List<DiscountEntity> findAllActiveByTypeOnDate(
            @Param("discountType") String discountType,
            @Param("date") LocalDate date
    );
}