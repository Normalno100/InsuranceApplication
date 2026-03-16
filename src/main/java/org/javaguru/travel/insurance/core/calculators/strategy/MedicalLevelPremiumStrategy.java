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
 * ИЗМЕНЕНИЯ (п. 3.2 плана рефакторинга — финальный шаг декомпозиции God Component):
 *   ❌ SharedCalculationComponents  →  ✅ прямые зависимости от конкретных калькуляторов:
 *     PersonAgeCalculator        — возраст и возрастной коэффициент
 *     TripDurationCalculator     — количество дней и коэффициент длительности
 *     AdditionalRisksCalculator  — дополнительные риски
 *     BundleDiscountCalculator   — пакетная скидка
 *     RiskDetailsBuilder         — детали по рискам для ответа API
 *
 * SharedCalculationComponents удалён из зависимостей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalLevelPremiumStrategy implements PremiumCalculationStrategy {

    // ✅ Domain port — правильная зависимость для core слоя
    private final ReferenceDataPort referenceDataPort;

    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;

    // ✅ Прямые зависимости от конкретных калькуляторов (SRP, нет God Component)
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

        // 1. Справочные данные через domain port
        MedicalRiskLimitLevel medicalLevel = referenceDataPort
                .findMedicalLevel(
                        request.getMedicalRiskLimitLevel(),
                        request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()));

        Country country = referenceDataPort
                .findCountry(
                        new CountryCode(request.getCountryIsoCode()),
                        request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 2. task_116: определяем, применять ли возрастной коэффициент
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(),
                request.getAgreementDateFrom());

        // 3. Возраст и коэффициент — PersonAgeCalculator
        AgeCalculator.AgeCalculationResult ageResult = personAgeCalculator.calculate(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom(),
                ageCoefficientEnabled);

        // 4. Дни и коэффициент длительности — TripDurationCalculator
        long days = tripDurationCalculator.calculateDays(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo());
        BigDecimal durationCoefficient = tripDurationCalculator.getDurationCoefficient(
                days,
                request.getAgreementDateFrom());

        // 5. Дополнительные риски — AdditionalRisksCalculator
        AdditionalRisksCalculator.AdditionalRisksResult additionalRisks =
                additionalRisksCalculator.calculate(
                        request.getSelectedRisks(),
                        ageResult.age(),
                        request.getAgreementDateFrom());

        // 6. Коэффициент страны из domain entity
        BigDecimal countryRiskCoefficient = country.getRiskCoefficient().value();

        // 7. Итоговый коэффициент (MEDICAL_LEVEL включает countryCoeff)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(countryRiskCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 8. Базовая премия (до лимита выплат)
        BigDecimal rawBasePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 9. task_117: применяем лимит выплат
        PayoutLimitService.PayoutLimitResult payoutResult = payoutLimitService.applyPayoutLimit(
                rawBasePremium,
                medicalLevel.getCoverageAmount(),
                medicalLevel.getMaxPayoutAmount());

        BigDecimal basePremium = payoutResult.adjustedPremium();

        // 10. Пакетная скидка — BundleDiscountCalculator
        BundleDiscountResult bundleDiscount = bundleDiscountCalculator.calculate(
                request.getSelectedRisks(),
                basePremium,
                request.getAgreementDateFrom());

        // 11. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 12. Детали по рискам — RiskDetailsBuilder
        var riskDetails = riskDetailsBuilder.build(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                countryRiskCoefficient,
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 13. Дефолтная ставка страны (только для CountryInfo в ответе)
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

        return new PremiumCalculationResult(
                finalPremium,
                medicalLevel.getDailyRate(),
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                countryRiskCoefficient,
                country.getNameEn(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                totalCoeff,
                (int) days,
                medicalLevel.getCoverageAmount(),
                riskDetails,
                bundleDiscount,
                steps,
                CalculationMode.MEDICAL_LEVEL,
                null,
                defaultPremiumInfo != null ? defaultPremiumInfo.defaultDayPremium() : null,
                defaultPremiumInfo != null ? defaultPremiumInfo.currency() : null,
                // task_117
                medicalLevel.getCoverageAmount(),
                payoutResult.appliedPayoutLimit(),
                payoutResult.payoutLimitApplied()
        );
    }
}