package org.javaguru.travel.insurance.core.underwriting.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.config.UnderwritingConfigService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
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
@RequiredArgsConstructor
public class TripDurationRule implements UnderwritingRule {

    private final UnderwritingConfigService configService;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        // Загружаем параметры из БД
        long maxDays = configService.getLongParameter("TripDurationRule", "MAX_DAYS", 180);
        long reviewThreshold = configService.getLongParameter("TripDurationRule", "REVIEW_DAYS_THRESHOLD", 90);

        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        log.debug("Evaluating trip duration rule: days={}, maxDays={}, reviewThreshold={}",
                days, maxDays, reviewThreshold);

        // Блокирующее условие
        if (days > maxDays) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Trip duration of %d days exceeds maximum of %d days", days, maxDays)
            );
        }

        // Требует проверки
        if (days > reviewThreshold) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Trip duration of %d days requires manual review (threshold: %d days)",
                            days, reviewThreshold)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "TripDurationRule";
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
