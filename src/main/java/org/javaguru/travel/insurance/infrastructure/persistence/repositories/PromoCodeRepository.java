package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Репозиторий для работы с промо-кодами
 */
public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {

    /**
     * Находит активный промо-код по коду на указанную дату
     */
    @Query("SELECT p FROM PromoCodeEntity p " +
            "WHERE p.code = :code " +
            "AND p.isActive = true " +
            "AND p.validFrom <= :date " +
            "AND p.validTo >= :date")
    Optional<PromoCodeEntity> findActiveByCode(
            @Param("code") String code,
            @Param("date") LocalDate date
    );

    /**
     * Находит активный промо-код по коду на текущую дату
     */
    default Optional<PromoCodeEntity> findActiveByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    /**
     * Инкрементирует счётчик использования промо-кода
     */
    @Modifying
    @Query("UPDATE PromoCodeEntity p " +
            "SET p.currentUsageCount = p.currentUsageCount + 1, " +
            "p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    /**
     * Проверяет существование промо-кода
     */
    boolean existsByCode(String code);
}
