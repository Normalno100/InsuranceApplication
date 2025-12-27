package org.javaguru.travel.insurance.core.underwriting.domain;

/**
 * Решение андеррайтинга
 */
public enum UnderwritingDecision {

    /**
     * Заявка одобрена автоматически
     */
    APPROVED("Approved"),

    /**
     * Требуется ручная проверка андеррайтером
     */
    REQUIRES_MANUAL_REVIEW("Requires Manual Review"),

    /**
     * Заявка отклонена
     */
    DECLINED("Declined");

    private final String description;

    UnderwritingDecision(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}