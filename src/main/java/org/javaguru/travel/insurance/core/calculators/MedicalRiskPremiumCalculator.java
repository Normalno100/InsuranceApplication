package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный калькулятор медицинской страховой премии
 *
 * Формула: ПРЕМИЯ = БАЗОВАЯ_СТАВКА × КОЭФФ_ВОЗРАСТА × КОЭФФ_СТРАНЫ × (1 + СУММА_КОЭФФ_РИСКОВ) × ДНИ
 */
@Component
@RequiredArgsConstructor
public class MedicalRiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;

    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        var details = calculatePremiumWithDetails(request);
        return details.premium();
    }

    public PremiumCalculationResult calculatePremiumWithDetails(TravelCalculatePremiumRequest request) {
        // 1. Получаем данные из БД
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));

        // 2. Расчёт возраста и коэффициента
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        // 3. Коэффициент дополнительных рисков
        BigDecimal additionalRisksCoeff = calculateAdditionalRisksCoefficient(
                request.getSelectedRisks(),
                request.getAgreementDateFrom()
        );

        // 4. Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        // 5. Итоговый коэффициент
        BigDecimal totalCoeff = ageResult.coefficient()
                .multiply(country.getRiskCoefficient())
                .multiply(BigDecimal.ONE.add(additionalRisksCoeff));

        // 6. Итоговая премия
        BigDecimal premium = medicalLevel.getDailyRate()
                .multiply(totalCoeff)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 7. Детали по рискам
        List<RiskPremiumDetail> riskDetails = calculateRiskDetails(
                request.getSelectedRisks(),
                medicalLevel.getDailyRate(),
                ageResult.coefficient(),
                country.getRiskCoefficient(),
                (int) days,
                request.getAgreementDateFrom()
        );

        // 8. Формируем результат
        return new PremiumCalculationResult(
                premium,
                medicalLevel.getDailyRate(),
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                country.getRiskCoefficient(),
                country.getNameEn(),
                additionalRisksCoeff,
                totalCoeff,
                (int) days,
                medicalLevel.getCoverageAmount(),
                riskDetails,
                buildCalculationSteps(medicalLevel.getDailyRate(), ageResult.coefficient(),
                        country.getRiskCoefficient(), additionalRisksCoeff, days, premium)
        );
    }

    private BigDecimal calculateAdditionalRisksCoefficient(List<String> selectedRiskCodes,
                                                           java.time.LocalDate agreementDate) {
        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return selectedRiskCodes.stream()
                .map(code -> riskTypeRepository.findActiveByCode(code, agreementDate))
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .filter(risk -> !risk.getIsMandatory())
                .map(risk -> risk.getCoefficient())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<RiskPremiumDetail> calculateRiskDetails(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            int days,
            java.time.LocalDate agreementDate) {

        List<RiskPremiumDetail> details = new ArrayList<>();

        // Базовый медицинский риск (всегда включен)
        BigDecimal basePremium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        var medicalRisk = riskTypeRepository.findActiveByCode("TRAVEL_MEDICAL", agreementDate)
                .orElseThrow();

        details.add(new RiskPremiumDetail(
                medicalRisk.getCode(),
                medicalRisk.getNameEn(),
                basePremium,
                BigDecimal.ZERO
        ));

        // Дополнительные риски
        if (selectedRiskCodes != null) {
            for (String riskCode : selectedRiskCodes) {
                var riskOpt = riskTypeRepository.findActiveByCode(riskCode, agreementDate);
                if (riskOpt.isPresent() && !riskOpt.get().getIsMandatory()) {
                    var risk = riskOpt.get();
                    BigDecimal riskPremium = basePremium
                            .multiply(risk.getCoefficient())
                            .setScale(2, RoundingMode.HALF_UP);

                    details.add(new RiskPremiumDetail(
                            risk.getCode(),
                            risk.getNameEn(),
                            riskPremium,
                            risk.getCoefficient()
                    ));
                }
            }
        }

        return details;
    }

    private List<CalculationStep> buildCalculationSteps(
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal additionalRisksCoefficient,
            long days,
            BigDecimal finalPremium) {

        List<CalculationStep> steps = new ArrayList<>();

        steps.add(new CalculationStep(
                "Base rate per day",
                "Base Rate",
                baseRate
        ));

        steps.add(new CalculationStep(
                "Age coefficient",
                String.format("Base Rate × Age Coeff = %.2f × %.2f",
                        baseRate, ageCoefficient),
                baseRate.multiply(ageCoefficient)
        ));

        steps.add(new CalculationStep(
                "Country risk coefficient",
                String.format("Previous × Country Coeff = %.2f × %.2f",
                        baseRate.multiply(ageCoefficient), countryCoefficient),
                baseRate.multiply(ageCoefficient).multiply(countryCoefficient)
        ));

        if (additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(new CalculationStep(
                    "Additional risks coefficient",
                    String.format("Previous × (1 + %.2f)", additionalRisksCoefficient),
                    baseRate.multiply(ageCoefficient)
                            .multiply(countryCoefficient)
                            .multiply(BigDecimal.ONE.add(additionalRisksCoefficient))
            ));
        }

        steps.add(new CalculationStep(
                "Multiply by number of days",
                String.format("Previous × %d days", days),
                finalPremium
        ));

        return steps;
    }

    public record PremiumCalculationResult(
            BigDecimal premium,
            BigDecimal baseRate,
            int age,
            BigDecimal ageCoefficient,
            String ageGroupDescription,
            BigDecimal countryCoefficient,
            String countryName,
            BigDecimal additionalRisksCoefficient,
            BigDecimal totalCoefficient,
            int days,
            BigDecimal coverageAmount,
            List<RiskPremiumDetail> riskDetails,
            List<CalculationStep> calculationSteps
    ) {}

    public record RiskPremiumDetail(
            String riskCode,
            String riskName,
            BigDecimal premium,
            BigDecimal coefficient
    ) {}

    public record CalculationStep(
            String description,
            String formula,
            BigDecimal result
    ) {}
}