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
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
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
 * ИЗМЕНЕНИЯ task_117:
 *   После расчёта базовой премии применяется PayoutLimitService:
 *   если maxPayoutAmount < coverageAmount, премия корректируется пропорционально.
 *   Результат включает поля medicalPayoutLimit, appliedPayoutLimit, payoutLimitApplied.
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
    private final CalculationConfigService calculationConfigService;
    private final PayoutLimitService payoutLimitService;   // task_117

    @Override
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.info("MEDICAL_LEVEL strategy: country={}, level={}, applyAgeCoefficient={}",
                request.getCountryIsoCode(),
                request.getMedicalRiskLimitLevel(),
                request.getApplyAgeCoefficient());

        // 1. Справочные данные
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()));

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()));

        // 2. task_116: определяем, применять ли возрастной коэффициент
        boolean ageCoefficientEnabled = calculationConfigService.resolveAgeCoefficientEnabled(
                request.getApplyAgeCoefficient(),
                request.getAgreementDateFrom());

        // 3. Возраст и коэффициент
        AgeCalculator.AgeCalculationResult ageResult = shared.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom(),
                ageCoefficientEnabled);

        // 4. Дни и коэффициент длительности
        long days = shared.calculateDays(request.getAgreementDateFrom(), request.getAgreementDateTo());
        BigDecimal durationCoefficient = shared.getDurationCoefficient(days, request.getAgreementDateFrom());

        // 5. Дополнительные риски
        SharedCalculationComponents.AdditionalRisksResult additionalRisks = shared.calculateAdditionalRisks(
                request.getSelectedRisks(), ageResult.age(), request.getAgreementDateFrom());

        // 6. Итоговый коэффициент (MEDICAL_LEVEL включает countryCoeff)
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(country.getRiskCoefficient())
                .multiply(durationCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisks.totalCoefficient()));

        // 7. Базовая премия (до лимита выплат)
        BigDecimal rawBasePremium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 8. task_117: применяем лимит выплат
        PayoutLimitService.PayoutLimitResult payoutResult = payoutLimitService.applyPayoutLimit(
                rawBasePremium,
                medicalLevel.getCoverageAmount(),
                medicalLevel.getMaxPayoutAmount());

        BigDecimal basePremium = payoutResult.adjustedPremium();

        // 9. Пакетная скидка
        BundleDiscountResult bundleDiscount = shared.calculateBundleDiscount(
                request.getSelectedRisks(), basePremium, request.getAgreementDateFrom());

        // 10. Итоговая премия
        BigDecimal finalPremium = basePremium.subtract(bundleDiscount.discountAmount())
                .setScale(2, RoundingMode.HALF_UP);

        // 11. Детали по рискам
        var riskDetails = shared.buildRiskDetails(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                durationCoefficient,
                (int) days,
                ageResult.age(),
                request.getAgreementDateFrom());

        // 12. Дефолтная ставка страны (только для CountryInfo в ответе)
        CountryDefaultDayPremiumService.DefaultPremiumResult defaultPremiumInfo =
                countryDefaultDayPremiumService
                        .findDefaultDayPremium(request.getCountryIsoCode(), request.getAgreementDateFrom())
                        .orElse(null);

        // 13. Шаги расчёта (добавляем шаг лимита выплат если применялся)
        var steps = stepsBuilder.buildMedicalLevelSteps(
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
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
                country.getRiskCoefficient(), durationCoefficient, bundleDiscount.discountAmount(),
                payoutResult.payoutLimitApplied());

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
                defaultPremiumInfo != null ? defaultPremiumInfo.currency() : null,
                // task_117
                medicalLevel.getCoverageAmount(),           // medicalPayoutLimit (для отображения)
                payoutResult.appliedPayoutLimit(),          // appliedPayoutLimit
                payoutResult.payoutLimitApplied()           // payoutLimitApplied
        );
    }
}