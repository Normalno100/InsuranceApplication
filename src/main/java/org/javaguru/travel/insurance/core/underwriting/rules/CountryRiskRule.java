package org.javaguru.travel.insurance.core.underwriting.rules;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

/**
 * Правило проверки риска страны
 *
 * Логика:
 * - LOW, MEDIUM: одобрено
 * - HIGH: требуется проверка
 * - VERY_HIGH: отклонено
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountryRiskRule implements UnderwritingRule {

    private final CountryRepository countryRepository;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {
        var country = countryRepository.findActiveByIsoCode(
                request.getCountryIsoCode(),
                request.getAgreementDateFrom()
        ).orElseThrow(() -> new IllegalArgumentException("Country not found"));

        String riskGroup = country.getRiskGroup();

        log.debug("Evaluating country risk rule for country: {}, risk group: {}",
                country.getNameEn(), riskGroup);

        switch (riskGroup) {
            case "VERY_HIGH":
                return RuleResult.blocking(
                        getRuleName(),
                        String.format("Travel to %s is not covered due to very high risk " +
                                "(war zone, epidemic, etc.)", country.getNameEn())
                );

            case "HIGH":
                return RuleResult.reviewRequired(
                        getRuleName(),
                        String.format("Travel to %s requires manual review due to high risk",
                                country.getNameEn())
                );

            case "MEDIUM":
                return RuleResult.warning(
                        getRuleName(),
                        String.format("Travel to %s has medium risk level", country.getNameEn())
                );

            default: // LOW
                return RuleResult.pass(getRuleName());
        }
    }

    @Override
    public String getRuleName() {
        return "Country Risk Rule";
    }

    @Override
    public int getOrder() {
        return 20;
    }
}