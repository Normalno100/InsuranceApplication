package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.CalculationMode;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
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
 * Источник базовой ставки: country_default_day_premiums.default_day_premium
 * CountryCoeff НЕ применяется — уже "запечён" в DefaultDayPremium.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CountryDefaultPremiumStrategy implements PremiumCalculationStrategy {

    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    private final CountryRepository countryRepository;
    private final SharedCalculationComponents shared;
    private final CalculationStepsBuilder stepsBuilder;

    @Override
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.info("COUNTRY_DEFAULT strategy: country={}", request.getCountryIsoCode());

        // 1. Дефолтная дневная ставка страны
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremium =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElseThrow(() -> new IllegalStateException(
                                "Country default day premium not found for: " + request.getCountryIsoCode()
                                        + ". shouldUseCountryDefaultMode() returned true — inconsistency."));

        // 2. Информация о стране (для countryCoefficient в деталях ответа)
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 3. Возраст и коэффициент (task_114: передаём agreementDateFrom для актуального коэфф. из БД)
        AgeCalculator.AgeCalculationResult ageResult = shared.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom());

        // 4. Дни и коэффициент длительности
        long days = shared.calculateDays(request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = shared.getDurationCoefficient(days, request.getAgreementDateFrom());

        // 5. Дополнительные риски с возрастными модификаторами
        SharedCalculationComponents.AdditionalRisksResult additionalRisks = shared.calculateAdditionalRisks(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 6. Базовая премия — БЕЗ countryCoefficient (он в DefaultDayPremium)
        BigDecimal basePremium = countryDefaultDayPremiumService.calculateBasePremium(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                (int) days);

        // Применяем коэффициент доп. рисков
        if (additionalRisks.totalCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            basePremium = basePremium
                    .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // 7. Пакетная скидка
        BundleDiscountResult bundleDiscount = shared.calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 8. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 9. Детали по рискам (countryCoeff = ONE, не участвует в формуле)
        var riskDetails = shared.buildRiskDetails(
                request.getSelectedRisks(),
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                BigDecimal.ONE,   // country coefficient не применяется в COUNTRY_DEFAULT
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 10. totalCoeff для информации (без countryCoeff)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 11. Шаги расчёта
        var steps = stepsBuilder.buildCountryDefaultSteps(
                defaultPremium.defaultDayPremium(),
                ageResult.coefficient(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                days,
                basePremium,
                bundleDiscount.discountAmount(),
                finalPremium);

        log.info("COUNTRY_DEFAULT final premium: {} EUR (age={}, ageCoeff={}, base={}, duration={}, " +
                "bundleDiscount={})",
                finalPremium, ageResult.age(), ageResult.coefficient(),
                defaultPremium.defaultDayPremium(), durationCoefficient, bundleDiscount.discountAmount());

        return new PremiumCalculationResult(
                finalPremium,
                defaultPremium.defaultDayPremium(),  // baseRate = defaultDayPremium
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),         // присутствует для CountryInfo, но не в формуле
                country.getNameEn(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                totalCoeff,
                (int) days,
                null,  // coverageAmount — не применимо в COUNTRY_DEFAULT
                riskDetails,
                bundleDiscount,
                steps,
                CalculationMode.COUNTRY_DEFAULT,
                defaultPremium.defaultDayPremium(),
                defaultPremium.defaultDayPremium(),
                defaultPremium.currency()
        );
    }
}
