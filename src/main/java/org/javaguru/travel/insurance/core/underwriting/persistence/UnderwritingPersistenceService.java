package org.javaguru.travel.insurance.core.underwriting.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.UnderwritingAuditLogEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.UnderwritingDecisionEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.UnderwritingAuditLogRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.UnderwritingDecisionRepository;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Сервис для сохранения решений андеррайтинга в БД
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingPersistenceService {

    private final UnderwritingDecisionRepository decisionRepository;
    private final UnderwritingAuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    /**
     * Сохраняет решение андеррайтинга в БД
     */
    @Transactional
    public UnderwritingDecisionEntity saveDecision(
            TravelCalculatePremiumRequest request,
            UnderwritingResult result,
            long evaluationDurationMs) {

        log.info("Saving underwriting decision for {} {}: {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                result.getDecision());

        // Создаём сущность решения
        UnderwritingDecisionEntity entity = new UnderwritingDecisionEntity();
        entity.setRequestId(UUID.randomUUID());

        // Персональные данные
        entity.setPersonFirstName(request.getPersonFirstName());
        entity.setPersonLastName(request.getPersonLastName());
        entity.setPersonBirthDate(request.getPersonBirthDate());

        // Детали поездки
        entity.setCountryIsoCode(request.getCountryIsoCode());
        entity.setAgreementDateFrom(request.getAgreementDateFrom());
        entity.setAgreementDateTo(request.getAgreementDateTo());

        // Решение
        entity.setDecision(result.getDecision().name());
        entity.setDeclineReason(result.getDeclineReason());

        if (result.requiresManualReview()) {
            entity.setReviewReason(result.getDeclineReason());
        }

        // JSON данные
        try {
            entity.setRuleResults(objectMapper.writeValueAsString(result.getRuleResults()));
            entity.setRequestData(objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            log.error("Error serializing JSON data", e);
        }

        // Метрики
        entity.setEvaluationDurationMs((int) evaluationDurationMs);
        entity.setCreatedBy("SYSTEM");

        // Сохраняем
        entity = decisionRepository.save(entity);
        log.debug("Saved underwriting decision with ID: {}", entity.getId());

        // Сохраняем аудит-лог для каждого правила
        saveAuditLog(entity.getId(), result.getRuleResults());

        return entity;
    }

    /**
     * Сохраняет аудит-лог правил
     */
    private void saveAuditLog(Long decisionId, List<RuleResult> ruleResults) {
        int order = 0;
        for (RuleResult ruleResult : ruleResults) {
            UnderwritingAuditLogEntity auditLog = new UnderwritingAuditLogEntity();
            auditLog.setDecisionId(decisionId);
            auditLog.setRuleName(ruleResult.getRuleName());
            auditLog.setSeverity(ruleResult.getSeverity().name());
            auditLog.setMessage(ruleResult.getMessage());
            auditLog.setRuleOrder(order++);

            auditLogRepository.save(auditLog);
        }

        log.debug("Saved {} audit log entries for decision ID: {}", ruleResults.size(), decisionId);
    }
}