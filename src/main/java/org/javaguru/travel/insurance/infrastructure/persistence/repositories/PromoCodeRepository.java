package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import jakarta.persistence.LockModeType;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.PromoCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Репозиторий для работы с промо-кодами.
 *
 * ИСПРАВЛЕНИЕ 2.1 (Race Condition):
 * Добавлен метод findActiveByCodeForUpdate() с пессимистической блокировкой
 * (SELECT ... FOR UPDATE). Используется в PromoCodeService.applyPromoCode()
 * для атомарного чтения + инкремента счётчика использования.
 */
@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {

    /**
     * Находит активный промо-код по коду на указанную дату.
     * Используется только для чтения (без изменения счётчика).
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
     * Находит активный промо-код по коду на текущую дату.
     * Используется только для чтения (без изменения счётчика).
     */
    default Optional<PromoCodeEntity> findActiveByCode(String code) {
        return findActiveByCode(code, LocalDate.now());
    }

    /**
     * Находит активный промо-код с пессимистической блокировкой строки
     * (SELECT ... FOR UPDATE).
     *
     * НАЗНАЧЕНИЕ: устраняет race condition при конкурентном применении
     * одного промо-кода. Блокировка удерживается до конца транзакции,
     * поэтому метод должен вызываться строго внутри @Transactional.
     *
     * ГАРАНТИЯ: в любой момент времени только одна транзакция держит
     * блокировку строки → проверка лимита и инкремент счётчика атомарны.
     *
     * @param code          код промо-кода (в верхнем регистре)
     * @param date          дата применения (обычно agreementDateFrom)
     * @return Optional с заблокированной сущностью или empty если не найден
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PromoCodeEntity p " +
            "WHERE p.code = :code " +
            "AND p.isActive = true " +
            "AND p.validFrom <= :date " +
            "AND p.validTo >= :date")
    Optional<PromoCodeEntity> findActiveByCodeForUpdate(
            @Param("code") String code,
            @Param("date") LocalDate date
    );

    /**
     * Инкрементирует счётчик использования промо-кода.
     * Альтернативный подход через UPDATE-запрос (используется как запасной вариант).
     */
    @Modifying
    @Query("UPDATE PromoCodeEntity p " +
            "SET p.currentUsageCount = p.currentUsageCount + 1, " +
            "p.updatedAt = CURRENT_TIMESTAMP " +
            "WHERE p.id = :id")
    void incrementUsageCount(@Param("id") Long id);

    /**
     * Проверяет существование промо-кода.
     */
    boolean existsByCode(String code);
}