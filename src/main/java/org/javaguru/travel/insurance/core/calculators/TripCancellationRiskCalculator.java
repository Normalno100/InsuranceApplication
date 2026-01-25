package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;

/**
 * Калькулятор риска отмены поездки (TRIP_CANCELLATION)
 *
 * Покрывает: компенсацию расходов при отмене поездки по уважительным причинам
 * (болезнь, смерть родственника, форс-мажор и т.д.)
 *
 * ФОРМУЛА:
 * Premium = Base_Medical_Premium × Risk_Coefficient
 *
 * ГДЕ:
 * - Base_Medical_Premium: базовая медицинская премия
 * - Risk_Coefficient: коэффициент риска TRIP_CANCELLATION (обычно 0.15)
 *
 * ОСОБЕННОСТИ:
 * - НЕ зависит от возраста (возрастной модификатор = 1.0)
 * - Зависит от стоимости поездки (базовая премия как прокси)
 * - Популярен в пакетах (часто входит в Full Protection)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TripCancellationRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;

    @Override
    public String getRiskCode() {
        return "TRIP_CANCELLATION";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating TRIP_CANCELLATION premium for {} {}",
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

        // 3. ФОРМУЛА (БЕЗ возрастного модификатора!)
        BigDecimal premium = baseMedicalPremium
                .multiply(riskCoefficient)
                .setScale(2, RoundingMode.HALF_UP);

        log.info("TRIP_CANCELLATION premium: {} EUR " +
                        "(basePremium={}, riskCoeff={})",
                premium, baseMedicalPremium, riskCoefficient);

        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        if (request.getSelectedRisks() == null ||
                !request.getSelectedRisks().contains(getRiskCode())) {
            return false;
        }

        log.debug("TRIP_CANCELLATION risk is selected and applicable");
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