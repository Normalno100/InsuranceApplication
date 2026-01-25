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
 * =================================================================
 * LUGGAGE_LOSS - Калькулятор риска потери багажа
 * =================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
class LuggageLossRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;

    @Override
    public String getRiskCode() {
        return "LUGGAGE_LOSS";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating LUGGAGE_LOSS premium");

        var riskType = riskTypeRepository
                .findActiveByCode(getRiskCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Risk type not found: " + getRiskCode()));

        BigDecimal basePremium = calculateBaseMedicalPremium(request);
        BigDecimal premium = basePremium
                .multiply(riskType.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("LUGGAGE_LOSS premium: {} EUR", premium);
        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        return request.getSelectedRisks() != null &&
                request.getSelectedRisks().contains(getRiskCode());
    }

    private BigDecimal calculateBaseMedicalPremium(TravelCalculatePremiumRequest request) {
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow();
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(), request.getAgreementDateFrom());
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow();
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(), request.getAgreementDateTo()) + 1;

        return medicalLevel.getDailyRate()
                .multiply(ageResult.coefficient())
                .multiply(country.getRiskCoefficient())
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

/**
 * =================================================================
 * FLIGHT_DELAY - Калькулятор риска задержки рейса
 * =================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
class FlightDelayRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;

    @Override
    public String getRiskCode() {
        return "FLIGHT_DELAY";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating FLIGHT_DELAY premium");

        var riskType = riskTypeRepository
                .findActiveByCode(getRiskCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Risk type not found: " + getRiskCode()));

        BigDecimal basePremium = calculateBaseMedicalPremium(request);
        BigDecimal premium = basePremium
                .multiply(riskType.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("FLIGHT_DELAY premium: {} EUR", premium);
        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        return request.getSelectedRisks() != null &&
                request.getSelectedRisks().contains(getRiskCode());
    }

    private BigDecimal calculateBaseMedicalPremium(TravelCalculatePremiumRequest request) {
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow();
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(), request.getAgreementDateFrom());
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow();
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(), request.getAgreementDateTo()) + 1;

        return medicalLevel.getDailyRate()
                .multiply(ageResult.coefficient())
                .multiply(country.getRiskCoefficient())
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}

/**
 * =================================================================
 * CIVIL_LIABILITY - Калькулятор гражданской ответственности
 * =================================================================
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CivilLiabilityRiskCalculator implements RiskPremiumCalculator {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalLevelRepository;
    private final CountryRepository countryRepository;
    private final RiskTypeRepository riskTypeRepository;

    @Override
    public String getRiskCode() {
        return "CIVIL_LIABILITY";
    }

    @Override
    public BigDecimal calculatePremium(TravelCalculatePremiumRequest request) {
        log.debug("Calculating CIVIL_LIABILITY premium");

        var riskType = riskTypeRepository
                .findActiveByCode(getRiskCode(), request.getAgreementDateFrom())
                .orElseThrow(() -> new IllegalArgumentException("Risk type not found: " + getRiskCode()));

        BigDecimal basePremium = calculateBaseMedicalPremium(request);
        BigDecimal premium = basePremium
                .multiply(riskType.getCoefficient())
                .setScale(2, RoundingMode.HALF_UP);

        log.info("CIVIL_LIABILITY premium: {} EUR", premium);
        return premium;
    }

    @Override
    public boolean isApplicable(TravelCalculatePremiumRequest request) {
        return request.getSelectedRisks() != null &&
                request.getSelectedRisks().contains(getRiskCode());
    }

    private BigDecimal calculateBaseMedicalPremium(TravelCalculatePremiumRequest request) {
        var medicalLevel = medicalLevelRepository
                .findActiveByCode(request.getMedicalRiskLimitLevel(), request.getAgreementDateFrom())
                .orElseThrow();
        var ageResult = ageCalculator.calculateAgeAndCoefficient(
                request.getPersonBirthDate(), request.getAgreementDateFrom());
        var country = countryRepository
                .findActiveByIsoCode(request.getCountryIsoCode(), request.getAgreementDateFrom())
                .orElseThrow();
        long days = ChronoUnit.DAYS.between(
                request.getAgreementDateFrom(), request.getAgreementDateTo()) + 1;

        return medicalLevel.getDailyRate()
                .multiply(ageResult.coefficient())
                .multiply(country.getRiskCoefficient())
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);
    }
}