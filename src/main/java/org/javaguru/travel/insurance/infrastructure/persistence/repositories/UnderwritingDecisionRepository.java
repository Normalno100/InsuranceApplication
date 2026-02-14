package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.UnderwritingDecisionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Репозиторий для решений андеррайтинга
 */
@Repository
public interface UnderwritingDecisionRepository extends JpaRepository<UnderwritingDecisionEntity, Long> {

    /**
     * Находит решение по request ID
     */
    Optional<UnderwritingDecisionEntity> findByRequestId(UUID requestId);

    /**
     * Находит все решения по персоне
     */
    List<UnderwritingDecisionEntity> findByPersonFirstNameAndPersonLastNameAndPersonBirthDate(
            String firstName,
            String lastName,
            LocalDate birthDate
    );

    /**
     * Находит решения по типу решения
     */
    List<UnderwritingDecisionEntity> findByDecision(String decision);

    /**
     * Находит решения за период
     */
    List<UnderwritingDecisionEntity> findByCreatedAtBetween(
            LocalDateTime from,
            LocalDateTime to
    );

    /**
     * Подсчитывает решения по типу
     */
    long countByDecision(String decision);

    /**
     * Статистика решений за период
     */
    @Query("SELECT u.decision, COUNT(u) FROM UnderwritingDecisionEntity u " +
            "WHERE u.createdAt BETWEEN :from AND :to " +
            "GROUP BY u.decision")
    List<Object[]> getDecisionStatistics(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Проверяет наличие предыдущих отказов для персоны
     */
    @Query("SELECT COUNT(u) > 0 FROM UnderwritingDecisionEntity u " +
            "WHERE u.personFirstName = :firstName " +
            "AND u.personLastName = :lastName " +
            "AND u.personBirthDate = :birthDate " +
            "AND u.decision = 'DECLINED'")
    boolean hasPreviousDeclines(
            @Param("firstName") String firstName,
            @Param("lastName") String lastName,
            @Param("birthDate") LocalDate birthDate
    );
}