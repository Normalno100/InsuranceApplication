package org.javaguru.travel.insurance.core.underwriting.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.underwriting.config.UnderwritingConfigService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalCoverageRule implements UnderwritingRule {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalRepository;
    private final UnderwritingConfigService configService;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        // Загружаем параметры из БД
        int reviewAge = configService.getIntParameter("MedicalCoverageRule", "REVIEW_AGE", 70);
        int blockingAge = configService.getIntParameter("MedicalCoverageRule", "BLOCKING_AGE", 75);
        BigDecimal reviewThreshold = configService.getBigDecimalParameter(
                "MedicalCoverageRule", "REVIEW_COVERAGE_THRESHOLD", new BigDecimal("100000"));
        BigDecimal blockingThreshold = configService.getBigDecimalParameter(
                "MedicalCoverageRule", "BLOCKING_COVERAGE_THRESHOLD", new BigDecimal("200000"));

        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        var medicalLevel = medicalRepository.findActiveByCode(
                request.getMedicalRiskLimitLevel(),
                request.getAgreementDateFrom()
        ).orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        BigDecimal coverage = medicalLevel.getCoverageAmount();

        log.debug("Evaluating medical coverage rule: age={}, coverage={}, reviewAge={}, blockingAge={}",
                age, coverage, reviewAge, blockingAge);

        // Блокирующее условие
        if (age > blockingAge && coverage.compareTo(blockingThreshold) > 0) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Coverage of %s EUR is too high for age %d (max %s EUR)",
                            coverage, age, blockingThreshold)
            );
        }

        // Требует проверки
        if (age >= reviewAge && coverage.compareTo(reviewThreshold) > 0) {
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
        return "MedicalCoverageRule";
    }

    @Override
    public int getOrder() {
        return 30;
    }
}