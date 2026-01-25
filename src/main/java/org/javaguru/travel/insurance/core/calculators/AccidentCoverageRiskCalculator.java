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
 * Калькулятор расширенного покрытия от несчастных случаев (ACCIDENT_COVERAGE)
 *
 * Покрывает: травмы, переломы, ожоги, инвалидность вследствие несчастного случая
 *
 * ФОРМУЛА:
 * Premium = Base_Medical_Premium × Risk_Coefficient × Age_Modifier
 *
 * ГДЕ:
 * - Base_Medical_Premium: базовая медицинская премия
 * - Risk_Coefficient: коэффициент риска ACCIDENT_COVERAGE (обычно 0.20)
 * - Age_Modifier: возрастной модификатор (U-shaped: дети и пожилые дороже)
 *
 * ВОЗРАСТНЫЕ МОДИФИКАТОРЫ (U-образная кривая):
 * - 0-12 лет: modifier = 1.2 (+20% - дети более подвержены травмам)
 * - 13-60 лет: modifier = 1.0 (стандарт)
 * - 61-80 лет: modifier = 1.4 (+40% - падения, медленное восстановление)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccidentCoverageRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;
    private final AgeRiskPricingService ageRiskPricingService;

    @Override
    public String getRiskCode() {
        return "ACCIDENT_COVERAGE";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating ACCIDENT_COVERAGE premium for {} {}",
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

        // 4. Получаем возрастной модификатор для несчастных случаев
        BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                getRiskCode(),
                age,
                request.getAgreementDateFrom()
        );
        log.debug("Age modifier for ACCIDENT_COVERAGE: {} (age: {})", ageModifier, age);

        // 5. ФОРМУЛА
        BigDecimal premium = baseMedicalPremium
                .multiply(riskCoefficient)
                .multiply(ageModifier)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("ACCIDENT_COVERAGE premium: {} EUR " +
                        "(basePremium={}, riskCoeff={}, ageMod={}, age={})",
                premium, baseMedicalPremium, riskCoefficient, ageModifier, age);

        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        if (request.getSelectedRisks() == null ||
                !request.getSelectedRisks().contains(getRiskCode())) {
            return false;
        }

        log.debug("ACCIDENT_COVERAGE risk is selected and applicable");
        return true;
    }

    private BigDecimal calculateBaseMedicalPremium(TravelCalculatePremiumRequest request) {
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(), request.getAgreementDateFrom());

        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Country not found"));

        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(), request.getAgreementDateTo()) + 1;

        return medicalLevel.getDailyRate()
                .multiply(ageResult.coefficient())
                .multiply(country.getRiskCoefficient())
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}