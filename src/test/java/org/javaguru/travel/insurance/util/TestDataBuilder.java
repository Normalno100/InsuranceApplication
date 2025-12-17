package org.javaguru.travel.insurance.util;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Утилита для создания тестовых данных
 * Содержит билдеры для всех основных доменных объектов
 */
public class TestDataBuilder {

    // ========== REQUEST V2 BUILDERS ==========

    /**
     * Создаёт валидный базовый запрос V2
     */
    public static TravelCalculatePremiumRequestV2 createValidRequestV2() {
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
     * Создаёт запрос V2 с дополнительными рисками
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2WithRisks() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"))
                .build();
    }

    /**
     * Создаёт запрос V2 с промо-кодом
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2WithPromoCode(String promoCode) {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .promoCode(promoCode)
                .build();
    }

    /**
     * Создаёт запрос V2 для группового страхования
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2ForGroup(int personsCount) {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .personsCount(personsCount)
                .build();
    }

    /**
     * Создаёт запрос V2 для корпоративного клиента
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2Corporate() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .isCorporate(true)
                .build();
    }

    /**
     * Создаёт запрос V2 с валютой
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2WithCurrency(String currency) {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .currency(currency)
                .build();
    }

    /**
     * Билдер для кастомизации запроса V2
     */
    public static class RequestV2Builder {
        private final TravelCalculatePremiumRequestV2.Builder builder;

        public RequestV2Builder() {
            this.builder = TravelCalculatePremiumRequestV2.builder()
                    .personFirstName("John")
                    .personLastName("Doe")
                    .personBirthDate(LocalDate.of(1990, 1, 1))
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(LocalDate.of(2025, 6, 15))
                    .countryIsoCode("ES")
                    .medicalRiskLimitLevel("50000");
        }

        public RequestV2Builder personFirstName(String firstName) {
            builder.personFirstName(firstName);
            return this;
        }

        public RequestV2Builder personLastName(String lastName) {
            builder.personLastName(lastName);
            return this;
        }

        public RequestV2Builder personBirthDate(LocalDate birthDate) {
            builder.personBirthDate(birthDate);
            return this;
        }

        public RequestV2Builder agreementDateFrom(LocalDate dateFrom) {
            builder.agreementDateFrom(dateFrom);
            return this;
        }

        public RequestV2Builder agreementDateTo(LocalDate dateTo) {
            builder.agreementDateTo(dateTo);
            return this;
        }

        public RequestV2Builder countryIsoCode(String isoCode) {
            builder.countryIsoCode(isoCode);
            return this;
        }

        public RequestV2Builder medicalRiskLimitLevel(String level) {
            builder.medicalRiskLimitLevel(level);
            return this;
        }

        public RequestV2Builder selectedRisks(List<String> risks) {
            builder.selectedRisks(risks);
            return this;
        }

        public RequestV2Builder currency(String currency) {
            builder.currency(currency);
            return this;
        }

        public RequestV2Builder promoCode(String promoCode) {
            builder.promoCode(promoCode);
            return this;
        }

        public RequestV2Builder personsCount(Integer count) {
            builder.personsCount(count);
            return this;
        }

        public RequestV2Builder isCorporate(Boolean corporate) {
            builder.isCorporate(corporate);
            return this;
        }

        public TravelCalculatePremiumRequestV2 build() {
            return builder.build();
        }
    }

    // ========== ENTITY BUILDERS ==========

    /**
     * Создаёт тестовую сущность страны
     */
    public static CountryEntity createCountryEntity(String isoCode, String nameEn, BigDecimal coefficient) {
        CountryEntity entity = new CountryEntity();
        entity.setId(1L);
        entity.setIsoCode(isoCode);
        entity.setNameEn(nameEn);
        entity.setNameRu("Тестовая страна");
        entity.setRiskGroup("LOW");
        entity.setRiskCoefficient(coefficient);
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    /**
     * Создаёт тестовую страну Spain
     */
    public static CountryEntity createSpainEntity() {
        return createCountryEntity("ES", "Spain", new BigDecimal("1.0"));
    }

    /**
     * Создаёт тестовую страну Thailand
     */
    public static CountryEntity createThailandEntity() {
        return createCountryEntity("TH", "Thailand", new BigDecimal("1.3"));
    }

    /**
     * Создаёт тестовую сущность уровня медицинского покрытия
     */
    public static MedicalRiskLimitLevelEntity createMedicalLevelEntity(
            String code,
            BigDecimal coverage,
            BigDecimal dailyRate) {

        MedicalRiskLimitLevelEntity entity = new MedicalRiskLimitLevelEntity();
        entity.setId(1L);
        entity.setCode(code);
        entity.setCoverageAmount(coverage);
        entity.setDailyRate(dailyRate);
        entity.setCurrency("EUR");
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    /**
     * Создаёт тестовый уровень покрытия 50000
     */
    public static MedicalRiskLimitLevelEntity createMedicalLevel50000Entity() {
        return createMedicalLevelEntity(
                "50000",
                new BigDecimal("50000"),
                new BigDecimal("4.5")
        );
    }

    /**
     * Создаёт тестовую сущность типа риска
     */
    public static RiskTypeEntity createRiskTypeEntity(
            String code,
            String nameEn,
            BigDecimal coefficient,
            boolean mandatory) {

        RiskTypeEntity entity = new RiskTypeEntity();
        entity.setId(1L);
        entity.setCode(code);
        entity.setNameEn(nameEn);
        entity.setNameRu("Тестовый риск");
        entity.setCoefficient(coefficient);
        entity.setIsMandatory(mandatory);
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        return entity;
    }

    /**
     * Создаёт тестовый обязательный риск (TRAVEL_MEDICAL)
     */
    public static RiskTypeEntity createMandatoryRiskEntity() {
        return createRiskTypeEntity(
                "TRAVEL_MEDICAL",
                "Medical Coverage",
                BigDecimal.ZERO,
                true
        );
    }

    /**
     * Создаёт тестовый опциональный риск (SPORT_ACTIVITIES)
     */
    public static RiskTypeEntity createSportRiskEntity() {
        return createRiskTypeEntity(
                "SPORT_ACTIVITIES",
                "Sport Activities",
                new BigDecimal("0.3"),
                false
        );
    }

    /**
     * Создаёт тестовый опциональный риск (EXTREME_SPORT)
     */
    public static RiskTypeEntity createExtremeSportRiskEntity() {
        return createRiskTypeEntity(
                "EXTREME_SPORT",
                "Extreme Sport",
                new BigDecimal("0.6"),
                false
        );
    }

    // ========== COMMON TEST DATES ==========

    public static LocalDate getTodayDate() {
        return LocalDate.now();
    }

    public static LocalDate getTomorrowDate() {
        return LocalDate.now().plusDays(1);
    }

    public static LocalDate getNextWeekDate() {
        return LocalDate.now().plusWeeks(1);
    }

    public static LocalDate getNextMonthDate() {
        return LocalDate.now().plusMonths(1);
    }

    public static LocalDate getBirthDate1990() {
        return LocalDate.of(1990, 1, 1);
    }

    public static LocalDate getBirthDate1980() {
        return LocalDate.of(1980, 1, 1);
    }

    public static LocalDate getBirthDate1970() {
        return LocalDate.of(1970, 1, 1);
    }

    public static LocalDate getBirthDate1960() {
        return LocalDate.of(1960, 1, 1);
    }

    // ========== CONVENIENCE METHODS ==========

    /**
     * Создаёт новый билдер для кастомизации запроса V2
     */
    public static RequestV2Builder requestV2() {
        return new RequestV2Builder();
    }

    /**
     * Быстрое создание запроса с одним риском
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2WithSingleRisk(String riskCode) {
        return requestV2()
                .selectedRisks(List.of(riskCode))
                .build();
    }

    /**
     * Быстрое создание запроса для указанной страны
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2ForCountry(String isoCode) {
        return requestV2()
                .countryIsoCode(isoCode)
                .build();
    }

    /**
     * Быстрое создание запроса для указанного возраста
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2ForAge(int age) {
        return requestV2()
                .personBirthDate(LocalDate.now().minusYears(age))
                .build();
    }

    /**
     * Быстрое создание запроса с длительностью
     */
    public static TravelCalculatePremiumRequestV2 createRequestV2ForDays(int days) {
        LocalDate from = LocalDate.now().plusDays(1);
        return requestV2()
                .agreementDateFrom(from)
                .agreementDateTo(from.plusDays(days))
                .build();
    }
}