package org.javaguru.travel.insurance.core.underwriting.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.underwriting.config.UnderwritingConfigService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Правило проверки дополнительных рисков
 *
 * Логика:
 * - Экстремальный спорт для >70 лет: отклонено
 * - Экстремальный спорт в странах VERY_HIGH: отклонено
 * - Экстремальный спорт для 60-70 лет: требуется проверка
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdditionalRisksRule implements UnderwritingRule {

    private final AgeCalculator ageCalculator;
    private final CountryRepository countryRepository;
    private final UnderwritingConfigService configService;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        List<String> risks = request.getSelectedRisks();

        if (risks == null || risks.isEmpty() || !risks.contains("EXTREME_SPORT")) {
            return RuleResult.pass(getRuleName());
        }

        // Загружаем параметры из БД
        int maxAge = configService.getIntParameter(
                "AdditionalRisksRule", "MAX_AGE_FOR_EXTREME_SPORT", 70);
        int reviewAge = configService.getIntParameter(
                "AdditionalRisksRule", "REVIEW_AGE_FOR_EXTREME_SPORT", 60);

        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        var country = countryRepository.findActiveByIsoCode(
                request.getCountryIsoCode(),
                request.getAgreementDateFrom()
        ).orElseThrow();

        log.debug("Evaluating additional risks rule: EXTREME_SPORT, age={}, maxAge={}, reviewAge={}",
                age, maxAge, reviewAge);

        // Блокирующие условия
        if (age > maxAge) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Extreme sport coverage not available for age %d (max age: %d)",
                            age, maxAge)
            );
        }

        if ("VERY_HIGH".equals(country.getRiskGroup())) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Extreme sport coverage not available in %s (very high risk country)",
                            country.getNameEn())
            );
        }

        // Требует проверки
        if (age >= reviewAge) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Extreme sport coverage for age %d requires manual review", age)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "AdditionalRisksRule";
    }

    @Override
    public int getOrder() {
        return 40;
    }
}