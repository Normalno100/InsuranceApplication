package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.UnderwritingRuleConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для конфигурации правил андеррайтинга
 */
public interface UnderwritingRuleConfigRepository extends JpaRepository<UnderwritingRuleConfigEntity, Long> {

    /**
     * Находит активную конфигурацию параметра правила на дату
     */
    @Query("SELECT c FROM UnderwritingRuleConfigEntity c " +
            "WHERE c.ruleName = :ruleName " +
            "AND c.parameterName = :parameterName " +
            "AND c.isActive = true " +
            "AND c.validFrom <= :date " +
            "AND (c.validTo IS NULL OR c.validTo >= :date)")
    Optional<UnderwritingRuleConfigEntity> findActiveConfig(
            @Param("ruleName") String ruleName,
            @Param("parameterName") String parameterName,
            @Param("date") LocalDate date
    );

    /**
     * Находит все активные конфигурации для правила
     */
    @Query("SELECT c FROM UnderwritingRuleConfigEntity c " +
            "WHERE c.ruleName = :ruleName " +
            "AND c.isActive = true " +
            "AND c.validFrom <= :date " +
            "AND (c.validTo IS NULL OR c.validTo >= :date)")
    List<UnderwritingRuleConfigEntity> findAllActiveConfigsForRule(
            @Param("ruleName") String ruleName,
            @Param("date") LocalDate date
    );

    /**
     * Находит все конфигурации по имени правила
     */
    List<UnderwritingRuleConfigEntity> findByRuleName(String ruleName);

    /**
     * Находит конфигурацию по правилу и параметру
     */
    Optional<UnderwritingRuleConfigEntity> findByRuleNameAndParameterName(
            String ruleName,
            String parameterName
    );

    /**
     * Проверяет существование конфигурации
     */
    boolean existsByRuleNameAndParameterName(String ruleName, String parameterName);
}