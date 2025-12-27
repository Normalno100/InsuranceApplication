package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

/**
 * Правило проверки возраста
 *
 * Логика:
 * - 0-74 года: одобрено
 * - 75-80 лет: требуется проверка
 * - >80 лет: отклонено
 */
@Slf4j
@Component
public class AgeRule implements UnderwritingRule {

    private static final int MAX_AGE = 80;
    private static final int REVIEW_AGE_THRESHOLD = 75;

    private final AgeCalculator ageCalculator;

    public AgeRule(AgeCalculator ageCalculator) {
        this.ageCalculator = ageCalculator;
    }

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        log.debug("Evaluating age rule for age: {}", age);

        // Блокирующее условие
        if (age > MAX_AGE) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Age %d exceeds maximum allowed age of %d", age, MAX_AGE)
            );
        }

        // Требует проверки
        if (age >= REVIEW_AGE_THRESHOLD) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Age %d requires manual review (threshold: %d)",
                            age, REVIEW_AGE_THRESHOLD)
            );
        }

        // Одобрено
        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "Age Rule";
    }

    @Override
    public int getOrder() {
        return 10; // Высокий приоритет (проверяем возраст первым)
    }
}