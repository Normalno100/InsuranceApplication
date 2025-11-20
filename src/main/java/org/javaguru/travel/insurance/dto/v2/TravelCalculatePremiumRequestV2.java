package org.javaguru.travel.insurance.dto.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Расширенный запрос на расчет страховой премии (версия 2)
 *
 * Включает все новые поля для полноценного расчета:
 * - Дата рождения для расчета возраста
 * - Страна назначения
 * - Уровень медицинского покрытия
 * - Дополнительные риски
 * - Валюта
 * - Промо-код
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TravelCalculatePremiumRequestV2 {

    // ========== ПЕРСОНАЛЬНЫЕ ДАННЫЕ ==========

    /**
     * Имя застрахованного
     * Обязательное поле
     */
    private String personFirstName;

    /**
     * Фамилия застрахованного
     * Обязательное поле
     */
    private String personLastName;

    /**
     * Дата рождения для расчета возраста
     * Обязательное поле
     * Формат: yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    /**
     * Email (опционально)
     */
    private String personEmail;

    /**
     * Телефон (опционально)
     */
    private String personPhone;

    // ========== ДАТЫ ПОЕЗДКИ ==========

    /**
     * Дата начала страхования
     * Обязательное поле
     * Формат: yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    /**
     * Дата окончания страхования
     * Обязательное поле
     * Формат: yyyy-MM-dd
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;

    // ========== СТРАНА И ПОКРЫТИЕ ==========

    /**
     * ISO код страны назначения (например, "ES" для Испании)
     * Обязательное поле
     */
    private String countryIsoCode;

    /**
     * Уровень медицинского покрытия
     * Обязательное поле
     * Возможные значения: LEVEL_5000, LEVEL_10000, LEVEL_20000,
     *                     LEVEL_50000, LEVEL_100000, LEVEL_200000, LEVEL_500000
     */
    private String medicalRiskLimitLevel;

    // ========== ДОПОЛНИТЕЛЬНЫЕ РИСКИ ==========

    /**
     * Список выбранных дополнительных рисков
     * Опциональное поле
     *
     * Возможные значения:
     * - SPORT_ACTIVITIES (активный спорт)
     * - EXTREME_SPORT (экстремальный спорт)
     * - PREGNANCY (беременность)
     * - CHRONIC_DISEASES (хронические заболевания)
     * - ACCIDENT_COVERAGE (от несчастных случаев)
     * - TRIP_CANCELLATION (отмена поездки)
     * - LUGGAGE_LOSS (потеря багажа)
     * - FLIGHT_DELAY (задержка рейса)
     * - CIVIL_LIABILITY (гражданская ответственность)
     */
    private List<String> selectedRisks;

    // ========== ВАЛЮТА И ПРОМО-КОД ==========

    /**
     * Валюта для расчета
     * Опциональное поле, по умолчанию EUR
     * Возможные значения: EUR, USD, GBP, CHF, JPY, CNY, RUB
     */
    private String currency;

    /**
     * Промо-код для получения скидки
     * Опциональное поле
     */
    private String promoCode;

    // ========== ГРУППОВОЕ СТРАХОВАНИЕ ==========

    /**
     * Количество застрахованных лиц (для групповых скидок)
     * Опциональное поле, по умолчанию 1
     */
    private Integer personsCount;

    /**
     * Признак корпоративного клиента
     * Опциональное поле
     */
    private Boolean isCorporate;

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверяет, является ли запрос базовым (без дополнительных полей)
     */
    public boolean isBasicRequest() {
        return personBirthDate == null
                && countryIsoCode == null
                && medicalRiskLimitLevel == null;
    }

    /**
     * Проверяет наличие дополнительных рисков
     */
    public boolean hasSelectedRisks() {
        return selectedRisks != null && !selectedRisks.isEmpty();
    }

    /**
     * Проверяет наличие промо-кода
     */
    public boolean hasPromoCode() {
        return promoCode != null && !promoCode.trim().isEmpty();
    }

    /**
     * Получает количество застрахованных (минимум 1)
     */
    public int getPersonsCountOrDefault() {
        return personsCount != null && personsCount > 0 ? personsCount : 1;
    }

    /**
     * Получает валюту или EUR по умолчанию
     */
    public String getCurrencyOrDefault() {
        return currency != null && !currency.trim().isEmpty() ? currency : "EUR";
    }

    /**
     * Проверяет, является ли клиент корпоративным
     */
    public boolean isCorporateClient() {
        return Boolean.TRUE.equals(isCorporate);
    }

    // ========== BUILDER PATTERN (ОПЦИОНАЛЬНО) ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TravelCalculatePremiumRequestV2 request = new TravelCalculatePremiumRequestV2();

        public Builder personFirstName(String personFirstName) {
            request.setPersonFirstName(personFirstName);
            return this;
        }

        public Builder personLastName(String personLastName) {
            request.setPersonLastName(personLastName);
            return this;
        }

        public Builder personBirthDate(LocalDate personBirthDate) {
            request.setPersonBirthDate(personBirthDate);
            return this;
        }

        public Builder personEmail(String personEmail) {
            request.setPersonEmail(personEmail);
            return this;
        }

        public Builder personPhone(String personPhone) {
            request.setPersonPhone(personPhone);
            return this;
        }

        public Builder agreementDateFrom(LocalDate agreementDateFrom) {
            request.setAgreementDateFrom(agreementDateFrom);
            return this;
        }

        public Builder agreementDateTo(LocalDate agreementDateTo) {
            request.setAgreementDateTo(agreementDateTo);
            return this;
        }

        public Builder countryIsoCode(String countryIsoCode) {
            request.setCountryIsoCode(countryIsoCode);
            return this;
        }

        public Builder medicalRiskLimitLevel(String medicalRiskLimitLevel) {
            request.setMedicalRiskLimitLevel(medicalRiskLimitLevel);
            return this;
        }

        public Builder selectedRisks(List<String> selectedRisks) {
            request.setSelectedRisks(selectedRisks);
            return this;
        }

        public Builder currency(String currency) {
            request.setCurrency(currency);
            return this;
        }

        public Builder promoCode(String promoCode) {
            request.setPromoCode(promoCode);
            return this;
        }

        public Builder personsCount(Integer personsCount) {
            request.setPersonsCount(personsCount);
            return this;
        }

        public Builder isCorporate(Boolean isCorporate) {
            request.setIsCorporate(isCorporate);
            return this;
        }

        public TravelCalculatePremiumRequestV2 build() {
            return request;
        }
    }
}
