package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.underwriting.config.UnderwritingConfigService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

/**
 * Правило проверки возраста
 * Параметры загружаются из БД
 */
@Slf4j
@Component
public class AgeRule implements UnderwritingRule {

    private final AgeCalculator ageCalculator;
    private final UnderwritingConfigService configService;

    public AgeRule(AgeCalculator ageCalculator, UnderwritingConfigService configService) {
        this.ageCalculator = ageCalculator;
        this.configService = configService;
    }

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        // Загружаем параметры из БД
        int maxAge = configService.getIntParameter("AgeRule", "MAX_AGE", 80);
        int reviewThreshold = configService.getIntParameter("AgeRule", "REVIEW_AGE_THRESHOLD", 75);

        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        log.debug("Evaluating age rule: age={}, maxAge={}, reviewThreshold={}",
                age, maxAge, reviewThreshold);

        // Блокирующее условие
        if (age > maxAge) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Age %d exceeds maximum allowed age of %d", age, maxAge)
            );
        }

        // Требует проверки
        if (age >= reviewThreshold) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Age %d requires manual review (threshold: %d)", age, reviewThreshold)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "AgeRule";
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
