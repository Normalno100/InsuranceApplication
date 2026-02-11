package org.javaguru.travel.insurance;

import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Улучшенный базовый класс для тестов.
 *
 * ПРИНЦИПЫ:
 * 1. Только фабричные методы для создания тестовых данных
 * 2. Никаких моков - моки настраиваются в самих тестах
 * 3. Говорящие имена методов, которые объясняют ЗАЧЕМ нужны эти данные
 * 4. Явные параметры вместо магических значений
 */
public abstract class BaseTestFixture {

    // ========== REQUEST BUILDERS WITH CLEAR INTENT ==========

    /**
     * Базовый запрос для взрослого человека 35 лет
     * Используется как основа для большинства тестов
     */
    protected TravelCalculatePremiumRequest standardAdultRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(35))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21)) // 14 days
                .countryIsoCode("ES") // Spain - low risk
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос для пожилого человека (75 лет) - используется для тестов age surcharge
     */
    protected TravelCalculatePremiumRequest elderlyPersonRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(75))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос для очень пожилого человека (80 лет) - граничное значение
     */
    protected TravelCalculatePremiumRequest maximumAgeRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(80))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с выбранными дополнительными рисками
     */
    protected TravelCalculatePremiumRequest requestWithSelectedRisks(String... riskCodes) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(35))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of(riskCodes))
                .build();
    }

    /**
     * Запрос с промо-кодом
     */
    protected TravelCalculatePremiumRequest requestWithPromoCode(String promoCode) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(35))
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .promoCode(promoCode)
                .build();
    }

    /**
     * Запрос с определенной длительностью поездки
     */
    protected TravelCalculatePremiumRequest requestWithDuration(int days) {
        LocalDate from = LocalDate.now().plusDays(7);
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.now().minusYears(35))
                .agreementDateFrom(from)
                .agreementDateTo(from.plusDays(days - 1))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    // ========== ENTITY BUILDERS WITH REALISTIC DATA ==========

    /**
     * Испания - страна с низким риском, коэффициент 1.0
     */
    protected CountryEntity spainLowRisk() {
        var country = new CountryEntity();
        country.setId(1L);
        country.setIsoCode("ES");
        country.setNameEn("Spain");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setRiskGroup("LOW");
        country.setValidFrom(LocalDate.of(2020, 1, 1));
        return country;
    }

    /**
     * Таиланд - страна со средним риском, коэффициент 1.3
     */
    protected CountryEntity thailandMediumRisk() {
        var country = new CountryEntity();
        country.setId(2L);
        country.setIsoCode("TH");
        country.setNameEn("Thailand");
        country.setRiskCoefficient(new BigDecimal("1.3"));
        country.setRiskGroup("MEDIUM");
        country.setValidFrom(LocalDate.of(2020, 1, 1));
        return country;
    }

    /**
     * Афганистан - страна с очень высоким риском, коэффициент 3.0
     */
    protected CountryEntity afghanistanVeryHighRisk() {
        var country = new CountryEntity();
        country.setId(3L);
        country.setIsoCode("AF");
        country.setNameEn("Afghanistan");
        country.setRiskCoefficient(new BigDecimal("3.0"));
        country.setRiskGroup("VERY_HIGH");
        country.setValidFrom(LocalDate.of(2020, 1, 1));
        return country;
    }

    /**
     * Медицинское покрытие 50,000 EUR - стандартный уровень
     */
    protected MedicalRiskLimitLevelEntity medicalLevel50k() {
        var level = new MedicalRiskLimitLevelEntity();
        level.setId(1L);
        level.setCode("50000");
        level.setDailyRate(new BigDecimal("4.50"));
        level.setCoverageAmount(new BigDecimal("50000"));
        level.setCurrency("EUR");
        level.setValidFrom(LocalDate.of(2020, 1, 1));
        return level;
    }

    /**
     * Медицинское покрытие 100,000 EUR - повышенный уровень
     */
    protected MedicalRiskLimitLevelEntity medicalLevel100k() {
        var level = new MedicalRiskLimitLevelEntity();
        level.setId(2L);
        level.setCode("100000");
        level.setDailyRate(new BigDecimal("7.50"));
        level.setCoverageAmount(new BigDecimal("100000"));
        level.setCurrency("EUR");
        level.setValidFrom(LocalDate.of(2020, 1, 1));
        return level;
    }

    /**
     * TRAVEL_MEDICAL - обязательный риск
     */
    protected RiskTypeEntity travelMedicalMandatoryRisk() {
        var risk = new RiskTypeEntity();
        risk.setId(1L);
        risk.setCode("TRAVEL_MEDICAL");
        risk.setNameEn("Medical Coverage");
        risk.setCoefficient(BigDecimal.ZERO); // Базовый риск, без коэффициента
        risk.setIsMandatory(true);
        risk.setValidFrom(LocalDate.of(2020, 1, 1));
        return risk;
    }

    /**
     * TRAVEL_BAGGAGE - опциональный риск, коэффициент 0.1
     */
    protected RiskTypeEntity travelBaggageOptionalRisk() {
        var risk = new RiskTypeEntity();
        risk.setId(2L);
        risk.setCode("TRAVEL_BAGGAGE");
        risk.setNameEn("Baggage Coverage");
        risk.setCoefficient(new BigDecimal("0.1"));
        risk.setIsMandatory(false);
        risk.setValidFrom(LocalDate.of(2020, 1, 1));
        return risk;
    }

    /**
     * EXTREME_SPORT - экстремальный спорт, коэффициент 0.5
     */
    protected RiskTypeEntity extremeSportOptionalRisk() {
        var risk = new RiskTypeEntity();
        risk.setId(3L);
        risk.setCode("EXTREME_SPORT");
        risk.setNameEn("Extreme Sport Coverage");
        risk.setCoefficient(new BigDecimal("0.5"));
        risk.setIsMandatory(false);
        risk.setValidFrom(LocalDate.of(2020, 1, 1));
        return risk;
    }

    // ========== AGE CALCULATION RESULTS ==========

    /**
     * Результат расчета возраста для взрослого человека (35 лет)
     * Коэффициент 1.1 согласно бизнес-правилам
     */
    protected AgeCalculator.AgeCalculationResult ageResult35Years() {
        return new AgeCalculator.AgeCalculationResult(
                35,
                new BigDecimal("1.1"),
                "Adults"
        );
    }

    /**
     * Результат расчета возраста для пожилого человека (75 лет)
     * Коэффициент 2.5 согласно бизнес-правилам
     */
    protected AgeCalculator.AgeCalculationResult ageResult75Years() {
        return new AgeCalculator.AgeCalculationResult(
                75,
                new BigDecimal("2.5"),
                "Very elderly"
        );
    }

    /**
     * Результат расчета возраста для молодого взрослого (25 лет)
     * Коэффициент 1.0 (базовый) согласно бизнес-правилам
     */
    protected AgeCalculator.AgeCalculationResult ageResult25Years() {
        return new AgeCalculator.AgeCalculationResult(
                25,
                new BigDecimal("1.0"),
                "Young adults"
        );
    }

    // ========== ASSERTION HELPERS ==========

    /**
     * Проверяет что премия находится в разумных пределах
     * Используется для smoke-тестов, когда точное значение не критично
     */
    protected boolean isReasonablePremiumAmount(BigDecimal premium) {
        return premium != null
                && premium.compareTo(BigDecimal.ZERO) > 0
                && premium.compareTo(new BigDecimal("10000")) < 0
                && premium.scale() == 2;
    }

    /**
     * Проверяет что значение находится в диапазоне (включительно)
     */
    protected boolean isInRange(BigDecimal value, BigDecimal min, BigDecimal max) {
        return value.compareTo(min) >= 0 && value.compareTo(max) <= 0;
    }
}