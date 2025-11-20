package org.javaguru.travel.insurance.dto.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javaguru.travel.insurance.dto.CoreResponse;
import org.javaguru.travel.insurance.dto.ValidationError;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Расширенный ответ на расчет страховой премии (версия 2)
 *
 * Включает детализацию расчета и все промежуточные коэффициенты
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelCalculatePremiumResponseV2 extends CoreResponse {

    // ========== ПЕРСОНАЛЬНЫЕ ДАННЫЕ ==========

    private String personFirstName;
    private String personLastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    /**
     * Рассчитанный возраст на момент начала поездки
     */
    private Integer personAge;

    // ========== ДАТЫ И ПЕРИОД ==========

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;

    /**
     * Количество дней страхования
     */
    private Integer agreementDays;

    // ========== СТРАНА И ПОКРЫТИЕ ==========

    /**
     * ISO код страны
     */
    private String countryIsoCode;

    /**
     * Название страны (на английском)
     */
    private String countryName;

    /**
     * Уровень медицинского покрытия
     */
    private String medicalRiskLimitLevel;

    /**
     * Сумма покрытия
     */
    private BigDecimal coverageAmount;

    // ========== ВЫБРАННЫЕ РИСКИ ==========

    /**
     * Список выбранных рисков
     */
    private List<String> selectedRisks;

    /**
     * Детальная информация по каждому риску
     */
    private List<RiskPremium> riskPremiums;

    // ========== ЦЕНЫ ==========

    /**
     * Итоговая премия (без скидок)
     */
    private BigDecimal agreementPriceBeforeDiscount;

    /**
     * Сумма скидки
     */
    private BigDecimal discountAmount;

    /**
     * Итоговая премия (после скидок)
     */
    private BigDecimal agreementPrice;

    /**
     * Валюта
     */
    private String currency;

    // ========== ДЕТАЛИ РАСЧЕТА ==========

    /**
     * Подробная информация о расчете
     */
    private CalculationDetails calculation;

    /**
     * Информация о примененном промо-коде
     */
    private PromoCodeInfo promoCodeInfo;

    /**
     * Информация о примененных скидках
     */
    private List<DiscountInfo> appliedDiscounts;

    // ========== КОНСТРУКТОРЫ ==========

    /**
     * Конструктор для ответа с ошибками
     */
    public TravelCalculatePremiumResponseV2(List<ValidationError> errors) {
        super(errors);
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    /**
     * Информация о премии по конкретному риску
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskPremium {
        /**
         * Тип риска
         */
        private String riskType;

        /**
         * Название риска
         */
        private String riskName;

        /**
         * Премия по этому риску
         */
        private BigDecimal premium;

        /**
         * Коэффициент риска
         */
        private BigDecimal coefficient;
    }

    /**
     * Детальная информация о расчете премии
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationDetails {
        /**
         * Базовая ставка за день
         */
        private BigDecimal baseRate;

        /**
         * Коэффициент возраста
         */
        private BigDecimal ageCoefficient;

        /**
         * Коэффициент страны
         */
        private BigDecimal countryCoefficient;

        /**
         * Суммарный коэффициент дополнительных рисков
         */
        private BigDecimal additionalRisksCoefficient;

        /**
         * Итоговый коэффициент (произведение всех коэффициентов)
         */
        private BigDecimal totalCoefficient;

        /**
         * Количество дней
         */
        private Integer days;

        /**
         * Формула расчета (текстовое представление)
         */
        private String formula;

        /**
         * Пошаговый расчет
         */
        private List<CalculationStep> steps;
    }

    /**
     * Шаг расчета
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationStep {
        private String description;
        private String formula;
        private BigDecimal result;
    }

    /**
     * Информация о промо-коде
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromoCodeInfo {
        /**
         * Код промо-кода
         */
        private String code;

        /**
         * Описание
         */
        private String description;

        /**
         * Тип скидки (PERCENTAGE или FIXED_AMOUNT)
         */
        private String discountType;

        /**
         * Значение скидки
         */
        private BigDecimal discountValue;

        /**
         * Фактическая сумма скидки
         */
        private BigDecimal actualDiscountAmount;
    }

    /**
     * Информация о скидке
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountInfo {
        /**
         * Тип скидки (GROUP, CORPORATE, SEASONAL, LOYALTY)
         */
        private String discountType;

        /**
         * Название скидки
         */
        private String name;

        /**
         * Процент скидки
         */
        private BigDecimal percentage;

        /**
         * Сумма скидки
         */
        private BigDecimal amount;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверяет наличие скидок
     */
    public boolean hasDiscounts() {
        return discountAmount != null && discountAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Проверяет наличие промо-кода
     */
    public boolean hasPromoCode() {
        return promoCodeInfo != null;
    }

    /**
     * Получает процент скидки
     */
    public BigDecimal getDiscountPercentage() {
        if (!hasDiscounts() || agreementPriceBeforeDiscount == null) {
            return BigDecimal.ZERO;
        }
        return discountAmount
                .multiply(BigDecimal.valueOf(100))
                .divide(agreementPriceBeforeDiscount, 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Получает количество выбранных дополнительных рисков
     */
    public int getSelectedRisksCount() {
        return selectedRisks != null ? selectedRisks.size() : 0;
    }

    // ========== BUILDER PATTERN ==========

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final TravelCalculatePremiumResponseV2 response = new TravelCalculatePremiumResponseV2();

        public Builder personFirstName(String personFirstName) {
            response.setPersonFirstName(personFirstName);
            return this;
        }

        public Builder personLastName(String personLastName) {
            response.setPersonLastName(personLastName);
            return this;
        }

        public Builder personBirthDate(LocalDate personBirthDate) {
            response.setPersonBirthDate(personBirthDate);
            return this;
        }

        public Builder personAge(Integer personAge) {
            response.setPersonAge(personAge);
            return this;
        }

        public Builder agreementDateFrom(LocalDate agreementDateFrom) {
            response.setAgreementDateFrom(agreementDateFrom);
            return this;
        }

        public Builder agreementDateTo(LocalDate agreementDateTo) {
            response.setAgreementDateTo(agreementDateTo);
            return this;
        }

        public Builder agreementDays(Integer agreementDays) {
            response.setAgreementDays(agreementDays);
            return this;
        }

        public Builder countryIsoCode(String countryIsoCode) {
            response.setCountryIsoCode(countryIsoCode);
            return this;
        }

        public Builder countryName(String countryName) {
            response.setCountryName(countryName);
            return this;
        }

        public Builder medicalRiskLimitLevel(String medicalRiskLimitLevel) {
            response.setMedicalRiskLimitLevel(medicalRiskLimitLevel);
            return this;
        }

        public Builder coverageAmount(BigDecimal coverageAmount) {
            response.setCoverageAmount(coverageAmount);
            return this;
        }

        public Builder selectedRisks(List<String> selectedRisks) {
            response.setSelectedRisks(selectedRisks);
            return this;
        }

        public Builder riskPremiums(List<RiskPremium> riskPremiums) {
            response.setRiskPremiums(riskPremiums);
            return this;
        }

        public Builder agreementPriceBeforeDiscount(BigDecimal agreementPriceBeforeDiscount) {
            response.setAgreementPriceBeforeDiscount(agreementPriceBeforeDiscount);
            return this;
        }

        public Builder discountAmount(BigDecimal discountAmount) {
            response.setDiscountAmount(discountAmount);
            return this;
        }

        public Builder agreementPrice(BigDecimal agreementPrice) {
            response.setAgreementPrice(agreementPrice);
            return this;
        }

        public Builder currency(String currency) {
            response.setCurrency(currency);
            return this;
        }

        public Builder calculation(CalculationDetails calculation) {
            response.setCalculation(calculation);
            return this;
        }

        public Builder promoCodeInfo(PromoCodeInfo promoCodeInfo) {
            response.setPromoCodeInfo(promoCodeInfo);
            return this;
        }

        public Builder appliedDiscounts(List<DiscountInfo> appliedDiscounts) {
            response.setAppliedDiscounts(appliedDiscounts);
            return this;
        }

        public TravelCalculatePremiumResponseV2 build() {
            return response;
        }
    }
}