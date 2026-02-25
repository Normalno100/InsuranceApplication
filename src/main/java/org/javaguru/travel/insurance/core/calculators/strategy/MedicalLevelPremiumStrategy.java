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
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Стратегия расчёта MEDICAL_LEVEL.
 *
 * ФОРМУЛА (task_115):
 *   ПРЕМИЯ = DailyRate × AgeCoeff × CountryCoeff × DurationCoeff
 *            × (1 + Σ riskCoeffs) × Days − BundleDiscount
 *
 * Источник базовой ставки: medical_risk_limit_levels.daily_rate
 *
 * ИЗМЕНЕНИЯ task_114:
 * - Шаги расчёта теперь показывают явное промежуточное значение после
 *   применения ageCoefficient через {@link CalculationStepsBuilder}.
 * - referenceDate (agreementDateFrom) передаётся в AgeCalculator для
 *   выбора актуальной версии коэффициента из таблицы age_coefficients.
 *
 * ИЗМЕНЕНИЯ task_115:
 * - Логика MEDICAL_LEVEL выделена из MedicalRiskPremiumCalculator в этот класс.
 * - MedicalRiskPremiumCalculator теперь является фасадом, делегирующим сюда.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalLevelPremiumStrategy implements PremiumCalculationStrategy {

    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    private final SharedCalculationComponents shared;
    private final CalculationStepsBuilder stepsBuilder;

    @Override
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.info("MEDICAL_LEVEL strategy: country={}, level={}",
                request.getCountryIsoCode(), request.getMedicalRiskLimitLevel());

        // 1. Справочные данные
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()));

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 2. Возраст и коэффициент (передаём agreementDateFrom для актуального коэфф. из БД)
        AgeCalculator.AgeCalculationResult ageResult = shared.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom());

        // 3. Дни и коэффициент длительности
        long days = shared.calculateDays(request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = shared.getDurationCoefficient(days, request.getAgreementDateFrom());

        // 4. Дополнительные риски с возрастными модификаторами
        SharedCalculationComponents.AdditionalRisksResult additionalRisks = shared.calculateAdditionalRisks(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 5. Итоговый коэффициент (MEDICAL_LEVEL включает countryCoeff)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(country.getRiskCoefficient())
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 6. Базовая премия
        BigDecimal basePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Пакетная скидка
        BundleDiscountResult bundleDiscount = shared.calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 8. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 9. Детали по рискам
        var riskDetails = shared.buildRiskDetails(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 10. Дефолтная ставка страны (только для отображения в ответе — CountryInfo)
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremiumInfo =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElse(null);

        // 11. Шаги расчёта (task_114: явные промежуточные значения для ageCoefficient)
        var steps = stepsBuilder.buildMedicalLevelSteps(
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                durationCoefficient,
                additionalRisks.totalCoefficient(),
                days,
                basePremium,
                bundleDiscount.discountAmount(),
                finalPremium);

        log.info("MEDICAL_LEVEL final premium: {} EUR (age={}, ageCoeff={}, country={}, duration={}, " +
                "bundleDiscount={})",
                finalPremium, ageResult.age(), ageResult.coefficient(),
                country.getRiskCoefficient(), durationCoefficient, bundleDiscount.discountAmount());

        return new PremiumCalculationResult(
                finalPremium,
                medicalLevel.getDailyRate(),
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),
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
                defaultPremiumInfo != null ? defaultPremiumInfo.currency() : null
        );
    }
}
