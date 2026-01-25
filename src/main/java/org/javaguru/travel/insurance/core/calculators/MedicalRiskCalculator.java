package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * Калькулятор базового медицинского покрытия (TRAVEL_MEDICAL)
 *
 * ВАЖНО: Этот калькулятор рассчитывает ТОЛЬКО базовую медицинскую часть,
 * БЕЗ дополнительных рисков и БЕЗ пакетных скидок.
 *
 * Дополнительные риски и скидки применяются на уровне MedicalRiskPremiumCalculator!
 *
 * ФОРМУЛА (базовая часть):
 * Premium = Daily_Rate × Age_Coefficient × Country_Coefficient
 *           × Duration_Coefficient × Days
 *
 * ГДЕ:
 * - Daily_Rate: базовая дневная ставка из medical_risk_limit_levels
 * - Age_Coefficient: коэффициент возраста (из AgeCalculator)
 * - Country_Coefficient: коэффициент страны риска
 * - Duration_Coefficient: коэффициент длительности (скидка за долгие поездки)
 * - Days: количество дней поездки
 *
 * ПОЛНАЯ ФОРМУЛА (применяется в MedicalRiskPremiumCalculator):
 * ИТОГОВАЯ_ПРЕМИЯ = базовая_премия × (1 + СУММА_МОДИФИЦИРОВАННЫХ_РИСКОВ) - СКИДКА_ПАКЕТА
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;

    @Override
    public String getRiskCode() {
        return "TRAVEL_MEDICAL";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating TRAVEL_MEDICAL base premium for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        // 1. Получаем базовую дневную ставку
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(
                        request.getMedicalRiskLimitLevel(),
                        request.getAgreementDateFrom()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()
                ));

        BigDecimal dailyRate = medicalLevel.getDailyRate();
        log.debug("Daily rate: {}", dailyRate);

        // 2. Получаем коэффициент возраста
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        BigDecimal ageCoefficient = ageResult.coefficient();
        log.debug("Age: {}, coefficient: {}", ageResult.age(), ageCoefficient);

        // 3. Получаем коэффициент страны
        var country = countryRepository
                .findActiveByIsoCode(
                        request.getCountryIsoCode(),
                        request.getAgreementDateFrom()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()
                ));

        BigDecimal countryCoefficient = country.getRiskCoefficient();
        log.debug("Country: {}, coefficient: {}", country.getNameEn(), countryCoefficient);

        // 4. Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        ) + 1; // +1 чтобы включить последний день

        log.debug("Trip duration: {} days", days);

        // 5. ФОРМУЛА БАЗОВОЙ ПРЕМИИ (БЕЗ дополнительных рисков и скидок)
        // ПРИМЕЧАНИЕ: Duration coefficient и дополнительные риски
        // применяются в MedicalRiskPremiumCalculator
        BigDecimal basePremium = dailyRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        log.info("TRAVEL_MEDICAL base premium: {} EUR " +
                        "(dailyRate={}, age={}, country={}, days={})",
                basePremium, dailyRate, ageCoefficient, countryCoefficient, days);

        log.debug("Note: Additional risks and bundle discounts applied separately by MedicalRiskPremiumCalculator");

        return basePremium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        // Базовое медицинское покрытие всегда применимо
        // Это обязательный (mandatory) риск
        return true;
    }
}