package org.javaguru.travel.insurance.infrastructure.persistence.repositories;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.UnderwritingAuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для аудит-лога андеррайтинга
 */
@Repository
public interface UnderwritingAuditLogRepository extends JpaRepository<UnderwritingAuditLogEntity, Long> {

    /**
     * Находит все логи для решения
     */
    List<UnderwritingAuditLogEntity> findByDecisionIdOrderByRuleOrder(Long decisionId);

    /**
     * Находит логи по названию правила
     */
    List<UnderwritingAuditLogEntity> findByRuleName(String ruleName);

    /**
     * Находит блокирующие правила за период
     */
    @Query("SELECT a FROM UnderwritingAuditLogEntity a " +
            "WHERE a.severity = 'BLOCKING' " +
            "AND a.evaluatedAt BETWEEN :from AND :to " +
            "ORDER BY a.evaluatedAt DESC")
    List<UnderwritingAuditLogEntity> findBlockingRulesBetween(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Статистика по правилам
     */
    @Query("SELECT a.ruleName, a.severity, COUNT(a) FROM UnderwritingAuditLogEntity a " +
            "WHERE a.evaluatedAt BETWEEN :from AND :to " +
            "GROUP BY a.ruleName, a.severity")
    List<Object[]> getRuleStatistics(
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );
}