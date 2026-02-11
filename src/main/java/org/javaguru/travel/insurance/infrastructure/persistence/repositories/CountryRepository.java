package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Репозиторий для работы со странами
 */
public interface CountryRepository extends JpaRepository<CountryEntity, Long> {

    /**
     * Проверяет существование страны по ISO коду
     */
    boolean existsByIsoCode(String isoCode);

    /**
     * Находит активную страну по ISO коду на указанную дату
     */
    @Query("SELECT c FROM CountryEntity c " +
            "WHERE c.isoCode = :isoCode " +
            "AND c.validFrom <= :date " +
            "AND (c.validTo IS NULL OR c.validTo >= :date)")
    Optional<CountryEntity> findActiveByIsoCode(
            @Param("isoCode") String isoCode,
            @Param("date") LocalDate date
    );

    /**
     * Находит активную страну по ISO коду на текущую дату
     */
    default Optional<CountryEntity> findActiveByIsoCode(String isoCode) {
        return findActiveByIsoCode(isoCode, LocalDate.now());
    }
}