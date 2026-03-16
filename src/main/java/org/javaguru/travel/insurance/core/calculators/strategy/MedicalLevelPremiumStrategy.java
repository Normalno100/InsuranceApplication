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
import org.javaguru.travel.insurance.core.services.PayoutLimitService;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Стратегия расчёта MEDICAL_LEVEL.
 *
 * ФОРМУЛА:
 *   ПРЕМИЯ = DailyRate × AgeCoeff × CountryCoeff × DurationCoeff
 *            × (1 + Σ riskCoeffs) × Days − BundleDiscount
 *
 * РЕФАКТОРИНГ (п. 4.3): PremiumCalculationResult собирается через
 * вложенные records AgeDetails, CountryDetails, TripDetails, RiskDetails,
 * PayoutLimitDetails вместо 22 плоских параметров.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalLevelPremiumStrategy implements PremiumCalculationStrategy {

    private final ReferenceDataPort referenceDataPort;
    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    private final PersonAgeCalculator personAgeCalculator;
    private final TripDurationCalculator tripDurationCalculator;
    private final AdditionalRisksCalculator additionalRisksCalculator;
    private final BundleDiscountCalculator bundleDiscountCalculator;
    private final RiskDetailsBuilder riskDetailsBuilder;
    private final CalculationStepsBuilder stepsBuilder;
    private final CalculationConfigService calculationConfigService;
    private final PayoutLimitService payoutLimitService;

    @Override
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.info("MEDICAL_LEVEL strategy: country={}, level={}, applyAgeCoefficient={}",
                request.getCountryIsoCode(),
                request.getMedicalRiskLimitLevel(),
                request.getApplyAgeCoefficient());

        // 1. Справочные данные
        MedicalRiskLimitLevel medicalLevel = referenceDataPort
                .findMedicalLevel(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()));

        Country country = referenceDataPort
                .findCountry(new CountryCode(request.getCountryIsoCode()), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 2. AgeCoefficient switch (task_116)
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(), request.getAgreementDateFrom());

        // 3. Возраст — PersonAgeCalculator
        AgeCalculator.AgeCalculationResult ageResult = personAgeCalculator.calculate(
                request.getPersonBirthDate(), request.getAgreementDateFrom(), ageCoefficientEnabled);

        // 4. Длительность — TripDurationCalculator
        long days = tripDurationCalculator.calculateDays(
                request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = tripDurationCalculator.getDurationCoefficient(
                days, request.getAgreementDateFrom());

        // 5. Дополнительные риски — AdditionalRisksCalculator
        AdditionalRisksCalculator.AdditionalRisksResult additionalRisks =
                additionalRisksCalculator.calculate(
                        request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 6. Коэффициент страны
        BigDecimal countryRiskCoefficient = country.getRiskCoefficient().value();

        // 7. Итоговый коэффициент
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(countryRiskCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 8. Базовая премия (до лимита выплат)
        BigDecimal rawBasePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 9. task_117: лимит выплат
        PayoutLimitService.PayoutLimitResult payoutResult = payoutLimitService.applyPayoutLimit(
                rawBasePremium,
                medicalLevel.getCoverageAmount(),
                medicalLevel.getMaxPayoutAmount());

        BigDecimal basePremium = payoutResult.adjustedPremium();

        // 10. Пакетная скидка — BundleDiscountCalculator
        BundleDiscountResult bundleDiscount = bundleDiscountCalculator.calculate(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 11. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 12. Детали по рискам — RiskDetailsBuilder
        var riskPremiumDetails = riskDetailsBuilder.build(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                countryRiskCoefficient,
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 13. CountryInfo для ответа
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremiumInfo =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElse(null);

        // 14. Шаги расчёта
        var steps = stepsBuilder.buildMedicalLevelSteps(
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                countryRiskCoefficient,
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                days,
                basePremium,
                bundleDiscount.discountAmount(),
                finalPremium,
                payoutResult.payoutLimitApplied() ? payoutResult.appliedPayoutLimit() : null,
                rawBasePremium);

        log.info("MEDICAL_LEVEL final premium: {} EUR (age={}, ageCoeff={}, ageCoefficientEnabled={}, " +
                        "country={}, duration={}, bundleDiscount={}, payoutLimitApplied={})",
                finalPremium, ageResult.age(), ageResult.coefficient(), ageCoefficientEnabled,
                countryRiskCoefficient, durationCoefficient, bundleDiscount.discountAmount(),
                payoutResult.payoutLimitApplied());

        // ── Сборка результата через вложенные records (п. 4.3) ──────────────

        AgeDetails ageDetails = new AgeDetails(
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description());

        CountryDetails countryDetails = new CountryDetails(
                country.getNameEn(),
                countryRiskCoefficient,
                null,   // countryDefaultDayPremium — не применяется в MEDICAL_LEVEL
                defaultPremiumInfo != null ? defaultPremiumInfo.defaultDayPremium() : null,
                defaultPremiumInfo != null ? defaultPremiumInfo.currency() : null);

        TripDetails tripDetails = new TripDetails(
                (int) days,
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                totalCoeff,
                medicalLevel.getCoverageAmount());

        RiskDetails riskDetails = new RiskDetails(riskPremiumDetails, bundleDiscount);

        PayoutLimitDetails payoutLimitDetails = new PayoutLimitDetails(
                medicalLevel.getCoverageAmount(),   // medicalPayoutLimit = coverageAmount для отображения
                payoutResult.appliedPayoutLimit(),
                payoutResult.payoutLimitApplied());

        return new PremiumCalculationResult(
                finalPremium,
                medicalLevel.getDailyRate(),
                ageDetails,
                countryDetails,
                tripDetails,
                riskDetails,
                CalculationMode.MEDICAL_LEVEL,
                steps,
                payoutLimitDetails);
    }
}