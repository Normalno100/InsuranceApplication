package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Фикстуры для тестов - готовые тестовые данные
 * Используется для быстрого создания типовых тестовых сценариев
 */
public class TestFixtures {

    // ========== REQUEST V2 FIXTURES ==========

    /**
     * Минимально валидный запрос V2
     */
    public static TravelCalculatePremiumRequestV2 validMinimalRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с полным набором полей
     */
    public static TravelCalculatePremiumRequestV2 validFullRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .personEmail("john.doe@example.com")
                .personPhone("+1234567890")
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"))
                .currency("EUR")
                .promoCode("SUMMER2025")
                .personsCount(2)
                .isCorporate(false)
                .build();
    }

    /**
     * Запрос для страхования спортсмена
     */
    public static TravelCalculatePremiumRequestV2 sportInsuranceRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Mike")
                .personLastName("Athlete")
                .personBirthDate(LocalDate.of(1995, 3, 15))
                .agreementDateFrom(LocalDate.of(2025, 12, 1))
                .agreementDateTo(LocalDate.of(2025, 12, 20))
                .countryIsoCode("AT") // Austria - ski resort
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"))
                .build();
    }

    /**
     * Запрос для экстремального туризма
     */
    public static TravelCalculatePremiumRequestV2 extremeSportRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Alex")
                .personLastName("Extreme")
                .personBirthDate(LocalDate.of(1992, 7, 20))
                .agreementDateFrom(LocalDate.of(2025, 7, 1))
                .agreementDateTo(LocalDate.of(2025, 7, 10))
                .countryIsoCode("NZ") // New Zealand
                .medicalRiskLimitLevel("200000")
                .selectedRisks(List.of("EXTREME_SPORT", "ACCIDENT_COVERAGE"))
                .build();
    }

    /**
     * Запрос для пожилого путешественника
     */
    public static TravelCalculatePremiumRequestV2 elderlyTravelerRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Robert")
                .personLastName("Senior")
                .personBirthDate(LocalDate.of(1960, 5, 10))
                .agreementDateFrom(LocalDate.of(2025, 9, 1))
                .agreementDateTo(LocalDate.of(2025, 9, 21))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("100000")
                .selectedRisks(List.of("CHRONIC_DISEASES"))
                .build();
    }

    /**
     * Запрос для группового путешествия
     */
    public static TravelCalculatePremiumRequestV2 groupTravelRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Group")
                .personLastName("Leader")
                .personBirthDate(LocalDate.of(1985, 4, 15))
                .agreementDateFrom(LocalDate.of(2025, 8, 1))
                .agreementDateTo(LocalDate.of(2025, 8, 14))
                .countryIsoCode("TH") // Thailand
                .medicalRiskLimitLevel("50000")
                .personsCount(10)
                .build();
    }

    /**
     * Запрос для корпоративного клиента
     */
    public static TravelCalculatePremiumRequestV2 corporateClientRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Business")
                .personLastName("Executive")
                .personBirthDate(LocalDate.of(1980, 11, 25))
                .agreementDateFrom(LocalDate.of(2025, 10, 1))
                .agreementDateTo(LocalDate.of(2025, 10, 7))
                .countryIsoCode("US")
                .medicalRiskLimitLevel("100000")
                .isCorporate(true)
                .build();
    }

    /**
     * Запрос с промо-кодом
     */
    public static TravelCalculatePremiumRequestV2 requestWithPromoCodeV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Promo")
                .personLastName("User")
                .personBirthDate(LocalDate.of(1992, 6, 18))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 6, 30))
                .countryIsoCode("FR")
                .medicalRiskLimitLevel("50000")
                .promoCode("SUMMER2025")
                .build();
    }

    /**
     * Запрос для опасной страны
     */
    public static TravelCalculatePremiumRequestV2 highRiskCountryRequestV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("Brave")
                .personLastName("Traveler")
                .personBirthDate(LocalDate.of(1988, 9, 12))
                .agreementDateFrom(LocalDate.of(2025, 11, 1))
                .agreementDateTo(LocalDate.of(2025, 11, 10))
                .countryIsoCode("IN") // India - HIGH risk
                .medicalRiskLimitLevel("200000")
                .selectedRisks(List.of("CHRONIC_DISEASES", "ACCIDENT_COVERAGE"))
                .build();
    }

    // ========== INVALID REQUEST FIXTURES ==========

    /**
     * Запрос без имени
     */
    public static TravelCalculatePremiumRequestV2 requestWithoutFirstNameV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName(null)
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос без фамилии
     */
    public static TravelCalculatePremiumRequestV2 requestWithoutLastNameV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName(null)
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с некорректными датами (dateFrom > dateTo)
     */
    public static TravelCalculatePremiumRequestV2 requestWithInvalidDatesV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 6, 1)) // dateFrom > dateTo
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с несуществующей страной
     */
    public static TravelCalculatePremiumRequestV2 requestWithInvalidCountryV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("XX") // Invalid country
                .medicalRiskLimitLevel("50000")
                .build();
    }

    /**
     * Запрос с несуществующим уровнем покрытия
     */
    public static TravelCalculatePremiumRequestV2 requestWithInvalidMedicalLevelV2() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("999999") // Invalid level
                .build();
    }

    // ========== RESPONSE V2 FIXTURES ==========

    /**
     * Успешный ответ V2 с минимальными данными
     */
    public static TravelCalculatePremiumResponseV2 validSuccessResponseV2() {
        return TravelCalculatePremiumResponseV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .personAge(35)
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .agreementDays(14)
                .countryIsoCode("ES")
                .countryName("Spain")
                .medicalRiskLimitLevel("50000")
                .coverageAmount(new BigDecimal("50000"))
                .agreementPriceBeforeDiscount(new BigDecimal("63.00"))
                .discountAmount(BigDecimal.ZERO)
                .agreementPrice(new BigDecimal("63.00"))
                .currency("EUR")
                .build();
    }

    /**
     * Ответ V2 с ошибками валидации
     */
    public static TravelCalculatePremiumResponseV2 validationErrorResponseV2() {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError("personFirstName", "Must not be empty!"));
        errors.add(new ValidationError("personLastName", "Must not be empty!"));
        return new TravelCalculatePremiumResponseV2(errors);
    }

    /**
     * Ответ V2 с одной ошибкой
     */
    public static TravelCalculatePremiumResponseV2 singleValidationErrorResponseV2(
            String field, String message) {
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError(field, message));
        return new TravelCalculatePremiumResponseV2(errors);
    }

    // ========== ENTITY FIXTURES ==========

    /**
     * Стандартный набор стран для тестов
     */
    public static List<CountryEntity> standardCountries() {
        List<CountryEntity> countries = new ArrayList<>();
        countries.add(createCountry("ES", "Spain", "1.0"));
        countries.add(createCountry("TH", "Thailand", "1.3"));
        countries.add(createCountry("IN", "India", "1.8"));
        countries.add(createCountry("AF", "Afghanistan", "2.5"));
        return countries;
    }

    /**
     * Стандартный набор уровней покрытия для тестов
     */
    public static List<MedicalRiskLimitLevelEntity> standardMedicalLevels() {
        List<MedicalRiskLimitLevelEntity> levels = new ArrayList<>();
        levels.add(createMedicalLevel("5000", "5000", "1.5"));
        levels.add(createMedicalLevel("50000", "50000", "4.5"));
        levels.add(createMedicalLevel("100000", "100000", "7.0"));
        levels.add(createMedicalLevel("200000", "200000", "12.0"));
        return levels;
    }

    /**
     * Стандартный набор рисков для тестов
     */
    public static List<RiskTypeEntity> standardRisks() {
        List<RiskTypeEntity> risks = new ArrayList<>();
        risks.add(createRisk("TRAVEL_MEDICAL", "Medical Coverage", "0", true));
        risks.add(createRisk("SPORT_ACTIVITIES", "Sport Activities", "0.3", false));
        risks.add(createRisk("EXTREME_SPORT", "Extreme Sport", "0.6", false));
        risks.add(createRisk("LUGGAGE_LOSS", "Luggage Loss", "0.1", false));
        return risks;
    }

    // ========== HELPER METHODS ==========

    private static CountryEntity createCountry(String isoCode, String name, String coefficient) {
        CountryEntity entity = new CountryEntity();
        entity.setId(1L);
        entity.setIsoCode(isoCode);
        entity.setNameEn(name);
        entity.setNameRu(name);
        entity.setRiskCoefficient(new BigDecimal(coefficient));
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    private static MedicalRiskLimitLevelEntity createMedicalLevel(
            String code, String coverage, String rate) {
        MedicalRiskLimitLevelEntity entity = new MedicalRiskLimitLevelEntity();
        entity.setId(1L);
        entity.setCode(code);
        entity.setCoverageAmount(new BigDecimal(coverage));
        entity.setDailyRate(new BigDecimal(rate));
        entity.setCurrency("EUR");
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    private static RiskTypeEntity createRisk(
            String code, String name, String coefficient, boolean mandatory) {
        RiskTypeEntity entity = new RiskTypeEntity();
        entity.setId(1L);
        entity.setCode(code);
        entity.setNameEn(name);
        entity.setNameRu(name);
        entity.setCoefficient(new BigDecimal(coefficient));
        entity.setIsMandatory(mandatory);
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    // ========== VALIDATION ERROR FIXTURES ==========

    /**
     * Типовые ошибки валидации
     */
    public static ValidationError firstNameEmptyError() {
        return new ValidationError("personFirstName", "Must not be empty!");
    }

    public static ValidationError lastNameEmptyError() {
        return new ValidationError("personLastName", "Must not be empty!");
    }

    public static ValidationError birthDateEmptyError() {
        return new ValidationError("personBirthDate", "Must not be empty!");
    }

    public static ValidationError dateFromEmptyError() {
        return new ValidationError("agreementDateFrom", "Must not be empty!");
    }

    public static ValidationError dateToEmptyError() {
        return new ValidationError("agreementDateTo", "Must not be empty!");
    }

    public static ValidationError dateToBeforeDateFromError() {
        return new ValidationError("agreementDateTo", "Must be after agreementDateFrom!");
    }

    public static ValidationError countryNotFoundError(String isoCode) {
        return new ValidationError("countryIsoCode",
                "Country '" + isoCode + "' not found or not active!");
    }

    public static ValidationError medicalLevelNotFoundError(String level) {
        return new ValidationError("medicalRiskLimitLevel",
                "Level '" + level + "' not found or not active!");
    }

    public static ValidationError riskNotFoundError(String riskCode) {
        return new ValidationError("selectedRisks",
                "Risk '" + riskCode + "' not found or not active!");
    }

    public static ValidationError mandatoryRiskError(String riskCode) {
        return new ValidationError("selectedRisks",
                "Risk '" + riskCode + "' is mandatory and cannot be selected manually!");
    }

    // ========== DATE FIXTURES ==========

    public static LocalDate today() {
        return LocalDate.now();
    }

    public static LocalDate tomorrow() {
        return LocalDate.now().plusDays(1);
    }

    public static LocalDate nextWeek() {
        return LocalDate.now().plusWeeks(1);
    }

    public static LocalDate nextMonth() {
        return LocalDate.now().plusMonths(1);
    }

    public static LocalDate lastYear() {
        return LocalDate.now().minusYears(1);
    }

    // ========== AGE FIXTURES ==========

    public static LocalDate birthDateForAge(int age) {
        return LocalDate.now().minusYears(age);
    }

    public static LocalDate childBirthDate() {
        return birthDateForAge(10);
    }

    public static LocalDate adultBirthDate() {
        return birthDateForAge(30);
    }

    public static LocalDate seniorBirthDate() {
        return birthDateForAge(65);
    }
}