package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * Калькулятор риска спортивных активностей (SPORT_ACTIVITIES)
 *
 * Покрывает: лыжи, сноуборд, дайвинг, серфинг и другие активные виды спорта
 *
 * ФОРМУЛА:
 * Premium = Base_Medical_Premium × Risk_Coefficient × Age_Modifier
 *
 * ГДЕ:
 * - Base_Medical_Premium: базовая медицинская премия
 *   (Daily_Rate × Age × Country × Duration × Days)
 * - Risk_Coefficient: коэффициент риска SPORT_ACTIVITIES (обычно 0.30)
 * - Age_Modifier: возрастной модификатор (молодые = 1.0, пожилые = 1.5)
 *
 * ПРИМЕРЫ:
 * - 25 лет: modifier = 1.0 (стандартная ставка)
 * - 55 лет: modifier = 1.2 (+20%)
 * - 70 лет: modifier = 1.5 (+50%)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportActivitiesRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;
    private final AgeRiskPricingService ageRiskPricingService;

    @Override
    public String getRiskCode() {
        return "SPORT_ACTIVITIES";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating SPORT_ACTIVITIES premium for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        // 1. Получаем коэффициент риска
        var riskType = riskTypeRepository
                .findActiveByCode(getRiskCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Risk type not found: " + getRiskCode()
                ));

        BigDecimal riskCoefficient = riskType.getCoefficient();
        log.debug("Risk coefficient: {}", riskCoefficient);

        // 2. Рассчитываем базовую медицинскую премию
        BigDecimal baseMedicalPremium = calculateBaseMedicalPremium(request);
        log.debug("Base medical premium: {}", baseMedicalPremium);

        // 3. Получаем возраст
        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );
        log.debug("Person age: {}", age);

        // 4. Получаем возрастной модификатор для спортивных активностей
        BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                getRiskCode(),
                age,
                request.getAgreementDateFrom()
        );
        log.debug("Age modifier for SPORT_ACTIVITIES: {}", ageModifier);

        // 5. ФОРМУЛА: базовая_премия × риск_коэффициент × возрастной_модификатор
        BigDecimal premium = baseMedicalPremium
                .multiply(riskCoefficient)
                .multiply(ageModifier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("SPORT_ACTIVITIES premium: {} EUR " +
                        "(basePremium={}, riskCoeff={}, ageMod={})",
                premium, baseMedicalPremium, riskCoefficient, ageModifier);

        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        // Проверяем что риск явно выбран в запросе
        if (request.getSelectedRisks() == null) {
            return false;
        }

        boolean isSelected = request.getSelectedRisks().contains(getRiskCode());
        log.debug("SPORT_ACTIVITIES is applicable: {}", isSelected);

        return isSelected;
    }

    /**
     * Вычисляет базовую медицинскую премию
     * (используется как база для расчета дополнительных рисков)
     */
    private BigDecimal calculateBaseMedicalPremium(TravelCalculatePremiumRequest request) {
        // Получаем базовую дневную ставку
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(
                        request.getMedicalRiskLimitLevel(),
                        request.getAgreementDateFrom()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Medical level not found: " + request.getMedicalRiskLimitLevel()
                ));

        BigDecimal dailyRate = medicalLevel.getDailyRate();

        // Получаем коэффициент возраста
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        BigDecimal ageCoefficient = ageResult.coefficient();

        // Получаем коэффициент страны
        var country = countryRepository
                .findActiveByIsoCode(
                        request.getCountryIsoCode(),
                        request.getAgreementDateFrom()
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Country not found: " + request.getCountryIsoCode()
                ));

        BigDecimal countryCoefficient = country.getRiskCoefficient();

        // Количество дней
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(),
                request.getAgreementDateTo()
        ) + 1;

        // Базовая формула (БЕЗ duration coefficient - он применяется выше)
        return dailyRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}