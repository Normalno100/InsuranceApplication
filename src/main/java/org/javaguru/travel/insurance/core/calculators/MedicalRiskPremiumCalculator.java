package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.DateTimeService;
import org.javaguru.travel.insurance.core.domain.Country;
import org.javaguru.travel.insurance.core.domain.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.core.domain.RiskType;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Главный калькулятор медицинской страховой премии
 *
 * Формула расчета:
 * ПРЕМИЯ = БАЗОВАЯ_СТАВКА × КОЭФФИЦИЕНТ_ВОЗРАСТА × КОЭФФИЦИЕНТ_СТРАНЫ × (1 + СУММА_КОЭФФ_РИСКОВ) × КОЛИЧЕСТВО_ДНЕЙ
 */
@Component
@RequiredArgsConstructor
public class MedicalRiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final DateTimeService dateTimeService;

    /**
     * Рассчитывает премию по медицинскому риску
     *
     * @param request запрос с параметрами
     * @return рассчитанная премия с деталями
     */
    public BigDecimal calculatePremium(TravelCalculatePremiumRequestV2 request) {
        // 1. Получаем базовую ставку
        BigDecimal baseRate = getBaseRate(request.getMedicalRiskLimitLevel());

        // 2. Рассчитываем коэффициент возраста
        BigDecimal ageCoefficient = calculateAgeCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        // 3. Получаем коэффициент страны
        BigDecimal countryCoefficient = getCountryCoefficient(request.getCountryIsoCode());

        // 4. Рассчитываем коэффициент дополнительных рисков
        BigDecimal additionalRisksCoefficient = calculateAdditionalRisksCoefficient(
                request.getSelectedRisks()
        );

        // 5. Считаем количество дней
        long days = dateTimeService.getDaysBetween(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        // 6. Применяем формулу
        BigDecimal premium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisksCoefficient))
                .multiply(BigDecimal.valueOf(days));

        // 7. Округляем до 2 знаков
        return premium.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Рассчитывает премию с полной детализацией
     *
     * @param request запрос
     * @return детализированный результат расчета
     */
    public PremiumCalculationResult calculatePremiumWithDetails(TravelCalculatePremiumRequestV2 request) {
        // 1. Базовая ставка
        MedicalRiskLimitLevel limitLevel = MedicalRiskLimitLevel.fromCode(
                request.getMedicalRiskLimitLevel()
        );
        BigDecimal baseRate = limitLevel.getDailyRate();

        // 2. Возраст и коэффициент
        AgeCalculator.AgeCalculationResult ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        // 3. Страна и коэффициент
        Country country = Country.fromIsoCode(request.getCountryIsoCode());
        BigDecimal countryCoefficient = country.getRiskCoefficient();

        // 4. Дополнительные риски
        BigDecimal additionalRisksCoefficient = calculateAdditionalRisksCoefficient(
                request.getSelectedRisks()
        );
        List<RiskPremiumDetail> riskDetails = calculateRiskDetails(
                request.getSelectedRisks(),
                baseRate,
                ageResult.coefficient(),
                countryCoefficient,
                (int) dateTimeService.getDaysBetween(
                        request.getAgreementDateFrom(),
                        request.getAgreementDateTo()
                )
        );

        // 5. Количество дней
        long days = dateTimeService.getDaysBetween(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        );

        // 6. Итоговый коэффициент
        BigDecimal totalCoefficient = ageResult.coefficient()
                .multiply(countryCoefficient)
                .multiply(BigDecimal.ONE.add(additionalRisksCoefficient));

        // 7. Итоговая премия
        BigDecimal premium = baseRate
                .multiply(totalCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // 8. Формируем результат
        return new PremiumCalculationResult(
                premium,
                baseRate,
                ageResult.age(),
                ageResult.coefficient(),
                ageResult.description(),
                countryCoefficient,
                country.getNameEn(),
                additionalRisksCoefficient,
                totalCoefficient,
                (int) days,
                limitLevel.getCoverage(),
                riskDetails,
                buildCalculationSteps(baseRate, ageResult.coefficient(), countryCoefficient,
                        additionalRisksCoefficient, days, premium)
        );
    }

    /**
     * Получает базовую ставку по уровню покрытия
     */
    private BigDecimal getBaseRate(String levelCode) {
        MedicalRiskLimitLevel level = MedicalRiskLimitLevel.fromCode(levelCode);
        return level.getDailyRate();
    }

    /**
     * Рассчитывает коэффициент возраста
     */
    private BigDecimal calculateAgeCoefficient(LocalDate birthDate, LocalDate referenceDate) {
        int age = ageCalculator.calculateAge(birthDate, referenceDate);
        return ageCalculator.getAgeCoefficient(age);
    }

    /**
     * Получает коэффициент страны
     */
    private BigDecimal getCountryCoefficient(String isoCode) {
        Country country = Country.fromIsoCode(isoCode);
        return country.getRiskCoefficient();
    }

    /**
     * Рассчитывает суммарный коэффициент дополнительных рисков
     * Коэффициенты складываются, а не перемножаются!
     */
    private BigDecimal calculateAdditionalRisksCoefficient(List<String> selectedRiskCodes) {
        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = BigDecimal.ZERO;
        for (String riskCode : selectedRiskCodes) {
            RiskType risk = RiskType.fromCode(riskCode);
            // Пропускаем обязательный медицинский риск (он уже в базовой ставке)
            if (!risk.isMandatory()) {
                total = total.add(risk.getCoefficient());
            }
        }

        return total;
    }

    /**
     * Рассчитывает детали по каждому риску
     */
    private List<RiskPremiumDetail> calculateRiskDetails(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            int days) {

        List<RiskPremiumDetail> details = new ArrayList<>();

        // Базовый медицинский риск (всегда включен)
        BigDecimal basePremium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        details.add(new RiskPremiumDetail(
                RiskType.TRAVEL_MEDICAL.getCode(),
                RiskType.TRAVEL_MEDICAL.getNameEn(),
                basePremium,
                BigDecimal.ZERO
        ));

        // Дополнительные риски
        if (selectedRiskCodes != null) {
            for (String riskCode : selectedRiskCodes) {
                RiskType risk = RiskType.fromCode(riskCode);
                if (!risk.isMandatory()) {
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

    /**
     * Формирует пошаговый расчет
     */
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

    /**
     * Результат расчета премии с деталями
     */
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

    /**
     * Детали премии по риску
     */
    public record RiskPremiumDetail(
            String riskCode,
            String riskName,
            BigDecimal premium,
            BigDecimal coefficient
    ) {}

    /**
     * Шаг расчета
     */
    public record CalculationStep(
            String description,
            String formula,
            BigDecimal result
    ) {}
}
