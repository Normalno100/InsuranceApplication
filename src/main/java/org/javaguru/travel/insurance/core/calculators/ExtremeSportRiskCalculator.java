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
 * Калькулятор риска экстремальных видов спорта (EXTREME_SPORT)
 *
 * Покрывает: альпинизм, парашютный спорт, BASE-jumping, скалолазание и т.д.
 *
 * ФОРМУЛА:
 * Premium = Base_Medical_Premium × Risk_Coefficient × Age_Modifier
 *
 * ГДЕ:
 * - Base_Medical_Premium: базовая медицинская премия
 * - Risk_Coefficient: коэффициент риска EXTREME_SPORT (обычно 0.60)
 * - Age_Modifier: возрастной модификатор (сильно растет с возрастом!)
 *
 * ВОЗРАСТНЫЕ МОДИФИКАТОРЫ (примеры):
 * - 18-35 лет: modifier = 1.0 (стандартная ставка)
 * - 36-50 лет: modifier = 1.3 (+30%)
 * - 51-65 лет: modifier = 1.8 (+80%)
 * - 66-70 лет: modifier = 2.5 (+150%)
 * - 71+ лет: НЕ ДОСТУПНО (блокируется андеррайтингом)
 *
 * ОГРАНИЧЕНИЯ:
 * - Максимальный возраст: 70 лет
 * - Не доступно в странах VERY_HIGH риска
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExtremeSportRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;
    private final AgeRiskPricingService ageRiskPricingService;

    private static final int MAX_AGE_FOR_EXTREME_SPORT = 70;

    @Override
    public String getRiskCode() {
        return "EXTREME_SPORT";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating EXTREME_SPORT premium for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        // 1. Проверяем применимость (возраст и страна)
        if (!isApplicable(request)) {
            log.warn("EXTREME_SPORT is not applicable for this request");
            return BigDecimal.ZERO;
        }

        // 2. Получаем коэффициент риска
        var riskType = riskTypeRepository
                .findActiveByCode(getRiskCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Risk type not found: " + getRiskCode()
                ));

        BigDecimal riskCoefficient = riskType.getCoefficient();
        log.debug("Risk coefficient: {}", riskCoefficient);

        // 3. Рассчитываем базовую медицинскую премию
        BigDecimal baseMedicalPremium = calculateBaseMedicalPremium(request);
        log.debug("Base medical premium: {}", baseMedicalPremium);

        // 4. Получаем возраст
        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );
        log.debug("Person age: {}", age);

        // 5. Получаем возрастной модификатор для экстремального спорта
        BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                getRiskCode(),
                age,
                request.getAgreementDateFrom()
        );
        log.debug("Age modifier for EXTREME_SPORT: {} (age: {})", ageModifier, age);

        // 6. ФОРМУЛА: базовая_премия × риск_коэффициент × возрастной_модификатор
        BigDecimal premium = baseMedicalPremium
                .multiply(riskCoefficient)
                .multiply(ageModifier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("EXTREME_SPORT premium: {} EUR " +
                        "(basePremium={}, riskCoeff={}, ageMod={}, age={})",
                premium, baseMedicalPremium, riskCoefficient, ageModifier, age);

        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        // 1. Проверяем что риск явно выбран
        if (request.getSelectedRisks() == null ||
                !request.getSelectedRisks().contains(getRiskCode())) {
            log.debug("EXTREME_SPORT not selected in request");
            return false;
        }

        // 2. Проверяем возраст
        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        if (age > MAX_AGE_FOR_EXTREME_SPORT) {
            log.warn("EXTREME_SPORT not applicable: age {} exceeds maximum {}",
                    age, MAX_AGE_FOR_EXTREME_SPORT);
            return false;
        }

        // 3. Проверяем страну (не доступно в VERY_HIGH risk странах)
        var country = countryRepository
                .findActiveByIsoCode(
                        request.getCountryIsoCode(),
                        request.getAgreementDateFrom()
                )
                .orElse(null);

        if (country != null && "VERY_HIGH".equals(country.getRiskGroup())) {
            log.warn("EXTREME_SPORT not applicable: country {} has VERY_HIGH risk",
                    country.getNameEn());
            return false;
        }

        log.debug("EXTREME_SPORT is applicable (age={}, country={})",
                age, country != null ? country.getNameEn() : "unknown");
        return true;
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

        // Базовая формула
        return dailyRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}