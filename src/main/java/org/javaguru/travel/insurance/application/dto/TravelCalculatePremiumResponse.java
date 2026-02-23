package org.javaguru.travel.insurance.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ответ на расчет страховой премии
 *
 * ИЗМЕНЕНИЯ v2.1 (этап 4):
 * - TripSummary: добавлено поле countryDefaultDayPremium (дефолтная дневная премия страны)
 * - TripSummary: добавлено поле calculationMode (режим расчёта: MEDICAL_LEVEL / COUNTRY_DEFAULT)
 * - PricingDetails: добавлено поле countryDefaultDayPremium
 * - CountryInfo: новый вложенный класс с детальной информацией о стране
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelCalculatePremiumResponse {

    // ========================================
    // МЕТАДАННЫЕ ОТВЕТА
    // ========================================

    @Builder.Default
    private String apiVersion = "2.1";

    @Builder.Default
    private UUID requestId = UUID.randomUUID();

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Boolean success;

    // ========================================
    // СТАТУС И ОШИБКИ
    // ========================================

    private ResponseStatus status;

    @Builder.Default
    private List<ValidationError> errors = List.of();

    // ========================================
    // КРАТКАЯ ИНФОРМАЦИЯ (SUMMARY)
    // ========================================

    private PricingSummary pricing;
    private PersonSummary person;
    private TripSummary trip;

    // ========================================
    // ДЕТАЛЬНАЯ ИНФОРМАЦИЯ (ОПЦИОНАЛЬНО)
    // ========================================

    private PricingDetails pricingDetails;
    private UnderwritingInfo underwriting;
    private List<AppliedDiscount> appliedDiscounts;

    // ========================================
    // ВЛОЖЕННЫЕ КЛАССЫ
    // ========================================

    public enum ResponseStatus {
        SUCCESS,
        VALIDATION_ERROR,
        DECLINED,
        REQUIRES_REVIEW
    }

    /**
     * Режим расчёта премии
     */
    public enum CalculationMode {
        /** Расчёт через уровень медицинского покрытия (medical_risk_limit_levels) */
        MEDICAL_LEVEL,
        /** Расчёт через дефолтную дневную премию страны (country_default_day_premiums) */
        COUNTRY_DEFAULT
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricingSummary {

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal totalPremium;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal baseAmount;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal totalDiscount;

        private String currency;

        @Builder.Default
        private List<String> includedRisks = List.of();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PersonSummary {
        private String firstName;
        private String lastName;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate birthDate;

        private Integer age;
        private String ageGroup;
    }

    /**
     * Краткая информация о поездке.
     *
     * ИЗМЕНЕНИЯ v2.1:
     * - countryDefaultDayPremium — дефолтная дневная ставка страны (если применялась)
     * - calculationMode — режим расчёта (MEDICAL_LEVEL или COUNTRY_DEFAULT)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TripSummary {

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateFrom;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateTo;

        private Integer days;
        private String countryCode;
        private String countryName;

        /** Уровень медицинского покрытия (null если использовался COUNTRY_DEFAULT режим) */
        private String medicalCoverageLevel;

        /** Сумма покрытия (null если использовался COUNTRY_DEFAULT режим) */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal coverageAmount;

        /**
         * Дефолтная дневная премия страны.
         * Заполняется только при calculationMode = COUNTRY_DEFAULT.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        /**
         * Режим расчёта премии:
         * - MEDICAL_LEVEL: стандартный расчёт через уровень покрытия
         * - COUNTRY_DEFAULT: расчёт через дефолтную дневную премию страны
         */
        private String calculationMode;
    }

    /**
     * Детальная информация о ценообразовании.
     *
     * ИЗМЕНЕНИЯ v2.1:
     * - countryDefaultDayPremium — дефолтная дневная ставка (если применялась)
     * - countryInfo — детальная информация о стране
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricingDetails {

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal baseRate;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal ageCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal countryCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal durationCoefficient;

        /**
         * Дефолтная дневная премия страны.
         * Заполняется только в режиме COUNTRY_DEFAULT.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        /** Детальная информация о стране (заполняется всегда при успешном ответе) */
        private CountryInfo countryInfo;

        @Builder.Default
        private List<RiskBreakdown> riskBreakdown = List.of();

        private String calculationFormula;

        @Builder.Default
        private List<CalculationStep> steps = List.of();
    }

    /**
     * Детальная информация о стране — добавлена в этапе 4.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CountryInfo {
        private String isoCode;
        private String name;
        private String riskGroup;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal riskCoefficient;

        /**
         * Дефолтная дневная премия для этой страны (из country_default_day_premiums).
         * null если для страны нет записи в таблице.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal defaultDayPremium;

        /** Валюта дефолтной премии */
        private String defaultDayPremiumCurrency;

        /** Признак наличия дефолтной дневной премии */
        private Boolean hasDefaultDayPremium;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskBreakdown {
        private String riskCode;
        private String riskName;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal premium;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal baseCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal ageModifier;

        private Boolean isMandatory;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationStep {
        private Integer stepNumber;
        private String description;
        private String formula;
        private BigDecimal result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnderwritingInfo {
        private String decision;
        private String reason;

        @Builder.Default
        private List<RuleEvaluation> evaluatedRules = List.of();
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleEvaluation {
        private String ruleName;
        private String severity;
        private String message;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedDiscount {
        private String type;
        private String code;
        private String description;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal amount;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal percentage;
    }

    // ========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success) && status == ResponseStatus.SUCCESS;
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
        private String code;
    }
}