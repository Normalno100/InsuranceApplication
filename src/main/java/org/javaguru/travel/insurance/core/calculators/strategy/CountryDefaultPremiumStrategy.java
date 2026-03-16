package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.AgeDetails;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.CalculationMode;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.CountryDetails;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PayoutLimitDetails;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.RiskDetails;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.TripDetails;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
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
 * РЕФАКТОРИНГ (п. 4.3): PremiumCalculationResult собирается через
 * вложенные records AgeDetails, CountryDetails, TripDetails, RiskDetails,
 * PayoutLimitDetails вместо 22 плоских параметров.
 *
 * В режиме COUNTRY_DEFAULT лимит выплат не применяется (нет medicalRiskLimitLevel).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountryDefaultPremiumStrategy implements PremiumCalculationStrategy {

    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    private final ReferenceDataPort referenceDataPort;
    private final PersonAgeCalculator personAgeCalculator;
    private final TripDurationCalculator tripDurationCalculator;
    private final AdditionalRisksCalculator additionalRisksCalculator;
    private final BundleDiscountCalculator bundleDiscountCalculator;
    private final RiskDetailsBuilder riskDetailsBuilder;
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
        Country country = referenceDataPort
                .findCountry(new CountryCode(request.getCountryIsoCode()), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 3. AgeCoefficient switch (task_116)
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(), request.getAgreementDateFrom());

        // 4. Возраст — PersonAgeCalculator
        AgeCalculator.AgeCalculationResult ageResult = personAgeCalculator.calculate(
                request.getPersonBirthDate(), request.getAgreementDateFrom(), ageCoefficientEnabled);

        // 5. Длительность — TripDurationCalculator
        long days = tripDurationCalculator.calculateDays(
                request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = tripDurationCalculator.getDurationCoefficient(
                days, request.getAgreementDateFrom());

        // 6. Дополнительные риски — AdditionalRisksCalculator
        AdditionalRisksCalculator.AdditionalRisksResult additionalRisks =
                additionalRisksCalculator.calculate(
                        request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 7. Базовая премия (без countryCoefficient — уже в defaultDayPremium)
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

        // 8. Пакетная скидка — BundleDiscountCalculator
        BundleDiscountResult bundleDiscount = bundleDiscountCalculator.calculate(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 10. Детали по рискам (countryCoeff = ONE — уже в baseRate)
        var riskPremiumDetails = riskDetailsBuilder.build(
                request.getSelectedRisks(),
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                BigDecimal.ONE,
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 11. totalCoeff для информации (без countryCoeff — он в baseRate)
        BigDecimal countryRiskCoefficient = country.getRiskCoefficient().value();
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 12. Шаги расчёта
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

        // ── Сборка результата через вложенные records (п. 4.3) ──────────────

        AgeDetails ageDetails = new AgeDetails(
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description());

        CountryDetails countryDetails = new CountryDetails(
                country.getNameEn(),
                countryRiskCoefficient,
                defaultPremium.defaultDayPremium(),   // applied in calculation
                defaultPremium.defaultDayPremium(),   // for info display
                defaultPremium.currency());

        TripDetails tripDetails = new TripDetails(
                (int) days,
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                totalCoeff,
                null);   // coverageAmount — нет в COUNTRY_DEFAULT

        RiskDetails riskDetails = new RiskDetails(riskPremiumDetails, bundleDiscount);

        // В COUNTRY_DEFAULT лимит выплат не применяется
        PayoutLimitDetails payoutLimitDetails = new PayoutLimitDetails(null, null, false);

        return new PremiumCalculationResult(
                finalPremium,
                defaultPremium.defaultDayPremium(),
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                CalculationMode.COUNTRY_DEFAULT,
                steps,
                payoutLimitDetails);
    }
}