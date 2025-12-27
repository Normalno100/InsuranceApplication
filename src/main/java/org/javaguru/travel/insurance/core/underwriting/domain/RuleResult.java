package org.javaguru.travel.insurance.core.underwriting.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Результат проверки одного правила
 */
@Getter
@AllArgsConstructor
public class RuleResult {

    private final String ruleName;
    private final RuleSeverity severity;
    private final String message;

    // Factory methods для удобства

    public static RuleResult pass(String ruleName) {
        return new RuleResult(ruleName, RuleSeverity.PASS, "Rule passed");
    }

    public static RuleResult warning(String ruleName, String message) {
        return new RuleResult(ruleName, RuleSeverity.WARNING, message);
    }

    public static RuleResult reviewRequired(String ruleName, String message) {
        return new RuleResult(ruleName, RuleSeverity.REVIEW_REQUIRED, message);
    }

    public static RuleResult blocking(String ruleName, String message) {
        return new RuleResult(ruleName, RuleSeverity.BLOCKING, message);
    }

    // Проверки

    public boolean isPassed() {
        return severity == RuleSeverity.PASS;
    }

    public boolean isWarning() {
        return severity == RuleSeverity.WARNING;
    }

    public boolean requiresReview() {
        return severity == RuleSeverity.REVIEW_REQUIRED;
    }

    public boolean isBlocking() {
        return severity == RuleSeverity.BLOCKING;
    }
}