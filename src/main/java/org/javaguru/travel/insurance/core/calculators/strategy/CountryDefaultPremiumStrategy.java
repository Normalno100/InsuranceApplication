package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.CalculationMode;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Стратегия расчёта COUNTRY_DEFAULT.
 *
 * ФОРМУЛА:
 *   ПРЕМИЯ = DefaultDayPremium × AgeCoeff × DurationCoeff
 *            × (1 + Σ riskCoeffs) × Days − BundleDiscount
 *
 * ИЗМЕНЕНИЯ task_117:
 *   В режиме COUNTRY_DEFAULT лимит выплат не применяется (нет medicalRiskLimitLevel).
 *   Поля medicalPayoutLimit, appliedPayoutLimit, payoutLimitApplied = null / false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountryDefaultPremiumStrategy implements PremiumCalculationStrategy {

    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    private final CountryRepository countryRepository;
    private final SharedCalculationComponents shared;
    private final CalculationStepsBuilder stepsBuilder;
    private final CalculationConfigService calculationConfigService;

    @Override
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.info("COUNTRY_DEFAULT strategy: country={}, applyAgeCoefficient={}",
                request.getCountryIsoCode(), request.getApplyAgeCoefficient());

        // 1. Дефолтная дневная ставка страны
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremium =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElseThrow(() -> new IllegalStateException(
                                "Country default day premium not found for: " + request.getCountryIsoCode()));

        // 2. Информация о стране
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 3. task_116: определяем, применять ли возрастной коэффициент
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(),
                request.getAgreementDateFrom());

        // 4. Возраст и коэффициент
        AgeCalculator.AgeCalculationResult ageResult = shared.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom(),
                ageCoefficientEnabled);

        // 5. Дни и коэффициент длительности
        long days = shared.calculateDays(request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = shared.getDurationCoefficient(days, request.getAgreementDateFrom());

        // 6. Дополнительные риски
        SharedCalculationComponents.AdditionalRisksResult additionalRisks = shared.calculateAdditionalRisks(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 7. Базовая премия — БЕЗ countryCoefficient
        BigDecimal basePremium = countryDefaultDayPremiumService.calculateBasePremium(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                (int) days);

        if (additionalRisks.totalCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            basePremium = basePremium
                    .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 8. Пакетная скидка
        BundleDiscountResult bundleDiscount = shared.calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 10. Детали по рискам (countryCoeff = ONE в COUNTRY_DEFAULT)
        var riskDetails = shared.buildRiskDetails(
                request.getSelectedRisks(),
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                BigDecimal.ONE,
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 11. totalCoeff для информации (без countryCoeff)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 12. Шаги расчёта (без лимита выплат — не применяется в COUNTRY_DEFAULT)
        var steps = stepsBuilder.buildCountryDefaultSteps(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                days,
                basePremium,
                bundleDiscount.discountAmount(),
                finalPremium);

        log.info("COUNTRY_DEFAULT final premium: {} EUR (age={}, ageCoeff={}, ageCoefficientEnabled={}, " +
                        "base={}, duration={}, bundleDiscount={})",
                finalPremium, ageResult.age(), ageResult.coefficient(), ageCoefficientEnabled,
                defaultPremium.defaultDayPremium(), durationCoefficient, bundleDiscount.discountAmount());

        return new PremiumCalculationResult(
                finalPremium,
                defaultPremium.defaultDayPremium(),
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),
                country.getNameEn(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                totalCoeff,
                (int) days,
                null,
                riskDetails,
                bundleDiscount,
                steps,
                CalculationMode.COUNTRY_DEFAULT,
                defaultPremium.defaultDayPremium(),
                defaultPremium.defaultDayPremium(),
                defaultPremium.currency(),
                // task_117: лимит выплат не применяется в COUNTRY_DEFAULT
                null,
                null,
                false
        );
    }
}