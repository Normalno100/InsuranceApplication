package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Правило проверки медицинского покрытия в зависимости от возраста
 *
 * Логика:
 * - <70 лет: любое покрытие одобрено
 * - 70-75 лет + покрытие >100,000: требуется проверка
 * - >75 лет + покрытие >200,000: отклонено
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalCoverageRule implements UnderwritingRule {

    private static final int REVIEW_AGE = 70;
    private static final int BLOCKING_AGE = 75;
    private static final BigDecimal REVIEW_COVERAGE_THRESHOLD = new BigDecimal("100000");
    private static final BigDecimal BLOCKING_COVERAGE_THRESHOLD = new BigDecimal("200000");

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalRepository;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        var medicalLevel = medicalRepository.findActiveByCode(
                request.getMedicalRiskLimitLevel(),
                request.getAgreementDateFrom()
        ).orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        BigDecimal coverage = medicalLevel.getCoverageAmount();

        log.debug("Evaluating medical coverage rule: age={}, coverage={}", age, coverage);

        // Блокирующее условие
        if (age > BLOCKING_AGE && coverage.compareTo(BLOCKING_COVERAGE_THRESHOLD) > 0) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Coverage of %s EUR is too high for age %d (max %s EUR)",
                            coverage, age, BLOCKING_COVERAGE_THRESHOLD)
            );
        }

        // Требует проверки
        if (age >= REVIEW_AGE && coverage.compareTo(REVIEW_COVERAGE_THRESHOLD) > 0) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("High coverage (%s EUR) for age %d requires manual review",
                            coverage, age)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "Medical Coverage Rule";
    }

    @Override
    public int getOrder() {
        return 30;
    }
}