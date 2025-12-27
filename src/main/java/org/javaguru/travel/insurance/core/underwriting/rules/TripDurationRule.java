package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

/**
 * Правило проверки длительности поездки
 *
 * Логика:
 * - до 90 дней: одобрено
 * - 91-180 дней: требуется проверка
 * - >180 дней: отклонено
 */
@Slf4j
@Component
public class TripDurationRule implements UnderwritingRule {

    private static final long MAX_DAYS = 180;
    private static final long REVIEW_DAYS_THRESHOLD = 90;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        log.debug("Evaluating trip duration rule: {} days", days);

        // Блокирующее условие
        if (days > MAX_DAYS) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Trip duration of %d days exceeds maximum of %d days. " +
                            "Please apply for long-term insurance.", days, MAX_DAYS)
            );
        }

        // Требует проверки
        if (days > REVIEW_DAYS_THRESHOLD) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Trip duration of %d days requires manual review " +
                            "(threshold: %d days)", days, REVIEW_DAYS_THRESHOLD)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "Trip Duration Rule";
    }

    @Override
    public int getOrder() {
        return 50;
    }
}