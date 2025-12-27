package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
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

    private static final int MAX_AGE_FOR_EXTREME_SPORT = 70;
    private static final int REVIEW_AGE_FOR_EXTREME_SPORT = 60;

    private final AgeCalculator ageCalculator;
    private final CountryRepository countryRepository;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        List<String> risks = request.getSelectedRisks();

        // Если нет дополнительных рисков, правило пройдено
        if (risks == null || risks.isEmpty()) {
            return RuleResult.pass(getRuleName());
        }

        // Проверяем только экстремальный спорт
        if (!risks.contains("EXTREME_SPORT")) {
            return RuleResult.pass(getRuleName());
        }

        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        var country = countryRepository.findActiveByIsoCode(
                request.getCountryIsoCode(),
                request.getAgreementDateFrom()
        ).orElseThrow();

        log.debug("Evaluating additional risks rule: EXTREME_SPORT, age={}, country={}",
                age, country.getNameEn());

        // Блокирующие условия
        if (age > MAX_AGE_FOR_EXTREME_SPORT) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Extreme sport coverage not available for age %d (max age: %d)",
                            age, MAX_AGE_FOR_EXTREME_SPORT)
            );
        }

        if ("VERY_HIGH".equals(country.getRiskGroup())) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Extreme sport coverage not available in %s " +
                            "(very high risk country)", country.getNameEn())
            );
        }

        // Требует проверки
        if (age >= REVIEW_AGE_FOR_EXTREME_SPORT) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("Extreme sport coverage for age %d requires manual review", age)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "Additional Risks Rule";
    }

    @Override
    public int getOrder() {
        return 40;
    }
}