package org.javaguru.travel.insurance;

import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Базовый класс для всех тестов с общими методами
 * Устраняет дублирование кода между тестовыми классами
 */
public abstract class BaseTestFixture {

    // ========== REQUEST BUILDERS ==========

    protected TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    protected TravelCalculatePremiumRequest requestWithPeriod(int days) {
        LocalDate from = LocalDate.of(2025, 6, 1);
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(from)
                .agreementDateTo(from.plusDays(days))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    protected TravelCalculatePremiumRequest requestWithRisks(String... risks) {
        var request = validRequest();
        request.setSelectedRisks(List.of(risks));
        return request;
    }

    protected TravelCalculatePremiumRequest requestWithPromoCode(String code) {
        var request = validRequest();
        request.setPromoCode(code);
        return request;
    }

    // ========== ENTITY BUILDERS ==========

    protected CountryEntity createCountryEntity(String isoCode, String name, BigDecimal coefficient) {
        var country = new CountryEntity();
        country.setIsoCode(isoCode);
        country.setNameEn(name);
        country.setRiskCoefficient(coefficient);
        country.setRiskGroup("LOW");
        country.setValidFrom(LocalDate.of(2020, 1, 1));
        return country;
    }

    protected CountryEntity defaultCountry() {
        return createCountryEntity("ES", "Spain", new BigDecimal("1.0"));
    }

    protected MedicalRiskLimitLevelEntity createMedicalLevel(String code, BigDecimal rate, BigDecimal coverage) {
        var level = new MedicalRiskLimitLevelEntity();
        level.setCode(code);
        level.setDailyRate(rate);
        level.setCoverageAmount(coverage);
        level.setCurrency("EUR");
        level.setValidFrom(LocalDate.of(2020, 1, 1));
        return level;
    }

    protected MedicalRiskLimitLevelEntity defaultMedicalLevel() {
        return createMedicalLevel("50000", new BigDecimal("4.50"), new BigDecimal("50000"));
    }

    protected RiskTypeEntity createRiskType(String code, String name, BigDecimal coefficient, boolean mandatory) {
        var risk = new RiskTypeEntity();
        risk.setCode(code);
        risk.setNameEn(name);
        risk.setCoefficient(coefficient);
        risk.setIsMandatory(mandatory);
        risk.setValidFrom(LocalDate.of(2020, 1, 1));
        return risk;
    }

    protected RiskTypeEntity mandatoryRisk() {
        return createRiskType("TRAVEL_MEDICAL", "Medical Coverage", BigDecimal.ZERO, true);
    }

    protected RiskTypeEntity optionalRisk(String code, BigDecimal coefficient) {
        return createRiskType(code, code, coefficient, false);
    }

    // ========== CALCULATION RESULT BUILDERS ==========

    protected MedicalRiskPremiumCalculator.PremiumCalculationResult createCalculationResult(
            BigDecimal premium) {

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,                    // premium
                new BigDecimal("4.5"),       // baseRate
                35,                          // age
                BigDecimal.ONE,              // ageCoefficient
                "Adults",                    // ageGroupDescription
                BigDecimal.ONE,              // countryCoefficient
                "Spain",                     // countryName
                BigDecimal.ONE,              // durationCoefficient
                BigDecimal.ZERO,             // additionalRisksCoefficient
                BigDecimal.ONE,              // totalCoefficient
                14,                          // days
                new BigDecimal("50000"),     // coverageAmount
                Collections.emptyList(),     // riskDetails
                null,                        // bundleDiscount
                Collections.emptyList()      // calculationSteps
        );
    }


    protected AgeCalculator.AgeCalculationResult createAgeResult(int age, BigDecimal coefficient) {
        return new AgeCalculator.AgeCalculationResult(age, coefficient, "Test Group");
    }

    // ========== PROMO CODE RESULT BUILDERS ==========

    protected PromoCodeService.PromoCodeResult validPromoCodeResult(
            String code, BigDecimal discount) {
        return new PromoCodeService.PromoCodeResult(
                true,
                null,
                code,
                "Test promo",
                PromoCodeService.DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                discount
        );
    }

    protected PromoCodeService.PromoCodeResult invalidPromoCodeResult(String message) {
        return new PromoCodeService.PromoCodeResult(
                false,
                message,
                null,
                null,
                null,
                null,
                null
        );
    }

    // ========== ASSERTION HELPERS ==========

    protected boolean isPremiumValid(BigDecimal premium) {
        return premium != null
                && premium.compareTo(BigDecimal.ZERO) >= 0
                && premium.scale() == 2;
    }

    protected boolean isInRange(BigDecimal value, String min, String max) {
        return value.compareTo(new BigDecimal(min)) >= 0
                && value.compareTo(new BigDecimal(max)) <= 0;
    }
}