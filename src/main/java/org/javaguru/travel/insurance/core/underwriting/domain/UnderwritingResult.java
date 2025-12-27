package org.javaguru.travel.insurance.core.underwriting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * Итоговый результат андеррайтинга
 */
@Getter
@AllArgsConstructor
public class UnderwritingResult {

    private final UnderwritingDecision decision;
    private final List<RuleResult> ruleResults;
    private final String declineReason;

    // Factory methods

    public static UnderwritingResult approved() {
        return new UnderwritingResult(
                UnderwritingDecision.APPROVED,
                new ArrayList<>(),
                null
        );
    }

    public static UnderwritingResult approved(List<RuleResult> ruleResults) {
        return new UnderwritingResult(
                UnderwritingDecision.APPROVED,
                ruleResults,
                null
        );
    }

    public static UnderwritingResult requiresReview(List<RuleResult> ruleResults, String reason) {
        return new UnderwritingResult(
                UnderwritingDecision.REQUIRES_MANUAL_REVIEW,
                ruleResults,
                reason
        );
    }

    public static UnderwritingResult declined(List<RuleResult> ruleResults, String reason) {
        return new UnderwritingResult(
                UnderwritingDecision.DECLINED,
                ruleResults,
                reason
        );
    }

    // Проверки

    public boolean isApproved() {
        return decision == UnderwritingDecision.APPROVED;
    }

    public boolean requiresManualReview() {
        return decision == UnderwritingDecision.REQUIRES_MANUAL_REVIEW;
    }

    public boolean isDeclined() {
        return decision == UnderwritingDecision.DECLINED;
    }
}