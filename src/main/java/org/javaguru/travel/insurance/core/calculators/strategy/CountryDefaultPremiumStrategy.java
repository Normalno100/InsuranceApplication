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
 * ИЗМЕНЕНИЯ (п. 3.2 плана рефакторинга — финальный шаг декомпозиции God Component):
 *   ❌ SharedCalculationComponents  →  ✅ прямые зависимости от конкретных калькуляторов:
 *     PersonAgeCalculator        — возраст и возрастной коэффициент
 *     TripDurationCalculator     — количество дней и коэффициент длительности
 *     AdditionalRisksCalculator  — дополнительные риски
 *     BundleDiscountCalculator   — пакетная скидка
 *     RiskDetailsBuilder         — детали по рискам для ответа API
 *
 * SharedCalculationComponents удалён из зависимостей.
 *
 * В режиме COUNTRY_DEFAULT лимит выплат не применяется (нет medicalRiskLimitLevel).
 * Поля medicalPayoutLimit, appliedPayoutLimit, payoutLimitApplied = null / false.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountryDefaultPremiumStrategy implements PremiumCalculationStrategy {

    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;

    // ✅ Domain port — правильная зависимость для core слоя
    private final ReferenceDataPort referenceDataPort;

    // ✅ Прямые зависимости от конкретных калькуляторов (SRP, нет God Component)
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

        // 2. Информация о стране через domain port
        Country country = referenceDataPort
                .findCountry(
                        new CountryCode(request.getCountryIsoCode()),
                        request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 3. task_116: определяем, применять ли возрастной коэффициент
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(),
                request.getAgreementDateFrom());

        // 4. Возраст и коэффициент — PersonAgeCalculator
        AgeCalculator.AgeCalculationResult ageResult = personAgeCalculator.calculate(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom(),
                ageCoefficientEnabled);

        // 5. Дни и коэффициент длительности — TripDurationCalculator
        long days = tripDurationCalculator.calculateDays(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo());
        BigDecimal durationCoefficient = tripDurationCalculator.getDurationCoefficient(
                days,
                request.getAgreementDateFrom());

        // 6. Дополнительные риски — AdditionalRisksCalculator
        AdditionalRisksCalculator.AdditionalRisksResult additionalRisks =
                additionalRisksCalculator.calculate(
                        request.getSelectedRisks(),
                        ageResult.age(),
                        request.getAgreementDateFrom());

        // 7. Базовая премия — БЕЗ countryCoefficient (уже включён в defaultDayPremium)
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
                request.getSelectedRisks(),
                basePremium,
                request.getAgreementDateFrom());

        // 9. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 10. Детали по рискам — RiskDetailsBuilder
        //     countryCoeff = ONE в COUNTRY_DEFAULT (коэффициент уже в baseRate)
        var riskDetails = riskDetailsBuilder.build(
                request.getSelectedRisks(),
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                BigDecimal.ONE,
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 11. Коэффициент страны из domain entity (для информации в ответе)
        BigDecimal countryRiskCoefficient = country.getRiskCoefficient().value();

        // 12. totalCoeff для информации (без countryCoeff — он в baseRate)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 13. Шаги расчёта (без лимита выплат — не применяется в COUNTRY_DEFAULT)
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
                countryRiskCoefficient,
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