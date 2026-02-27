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
 * Ответ на расчет страховой премии.
 *
 * ИЗМЕНЕНИЯ task_117:
 * - TripSummary: добавлено поле medicalPayoutLimit (лимит выплат для информации)
 * - PricingDetails: добавлены appliedPayoutLimit, payoutLimitApplied
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelCalculatePremiumResponse {

    @Builder.Default
    private String apiVersion = "2.1";

    @Builder.Default
    private UUID requestId = UUID.randomUUID();

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Boolean success;

    private ResponseStatus status;

    @Builder.Default
    private List<ValidationError> errors = List.of();

    private PricingSummary pricing;
    private PersonSummary person;
    private TripSummary trip;

    private PricingDetails pricingDetails;
    private UnderwritingInfo underwriting;
    private List<AppliedDiscount> appliedDiscounts;

    public enum ResponseStatus {
        SUCCESS,
        VALIDATION_ERROR,
        DECLINED,
        REQUIRES_REVIEW
    }

    public enum CalculationMode {
        MEDICAL_LEVEL,
        COUNTRY_DEFAULT
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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
     * ИЗМЕНЕНИЯ task_117:
     * - medicalPayoutLimit — лимит страховых выплат по медицинскому риску.
     *   Заполняется в режиме MEDICAL_LEVEL.
     *   null в режиме COUNTRY_DEFAULT (нет привязки к уровню покрытия).
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TripSummary {

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateFrom;

        @JsonFormat(pattern = "yyyy-MM-dd")
        private LocalDate dateTo;

        private Integer days;
        private String countryCode;
        private String countryName;

        /** Уровень медицинского покрытия (null если COUNTRY_DEFAULT) */
        private String medicalCoverageLevel;

        /** Сумма покрытия (null если COUNTRY_DEFAULT) */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal coverageAmount;

        /**
         * task_117: Лимит страховых выплат по медицинскому риску.
         * Заполняется в режиме MEDICAL_LEVEL.
         * Если payoutLimitApplied=true — этот лимит меньше coverageAmount и была применена корректировка.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal medicalPayoutLimit;

        /**
         * Дефолтная дневная премия страны.
         * Заполняется только при calculationMode = COUNTRY_DEFAULT.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        private String calculationMode;
    }

    /**
     * Детальная информация о ценообразовании.
     *
     * ИЗМЕНЕНИЯ task_117:
     * - appliedPayoutLimit — фактически применённый лимит выплат
     * - payoutLimitApplied — true если премия была скорректирована
     */
    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class PricingDetails {

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal baseRate;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal ageCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal countryCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal durationCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        /** Детальная информация о стране */
        private CountryInfo countryInfo;

        @Builder.Default
        private List<RiskBreakdown> riskBreakdown = List.of();

        private String calculationFormula;

        @Builder.Default
        private List<CalculationStep> steps = List.of();

        /**
         * task_117: Фактически применённый лимит страховых выплат.
         * null в режиме COUNTRY_DEFAULT или если лимит не применялся.
         */
        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal appliedPayoutLimit;

        /**
         * task_117: Флаг — была ли применена корректировка премии из-за лимита выплат.
         * false = лимит не применялся (maxPayoutAmount >= coverageAmount или COUNTRY_DEFAULT режим).
         * true  = премия была уменьшена пропорционально лимиту.
         */
        private Boolean payoutLimitApplied;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CountryInfo {
        private String isoCode;
        private String name;
        private String riskGroup;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
        private BigDecimal riskCoefficient;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal defaultDayPremium;

        private String defaultDayPremiumCurrency;
        private Boolean hasDefaultDayPremium;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
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

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CalculationStep {
        private Integer stepNumber;
        private String description;
        private String formula;
        private BigDecimal result;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UnderwritingInfo {
        private String decision;
        private String reason;

        @Builder.Default
        private List<RuleEvaluation> evaluatedRules = List.of();
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RuleEvaluation {
        private String ruleName;
        private String severity;
        private String message;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AppliedDiscount {
        private String type;
        private String code;
        private String description;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal amount;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal percentage;
    }

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success) && status == ResponseStatus.SUCCESS;
    }

    @Getter @Setter @AllArgsConstructor @NoArgsConstructor @Builder
    public static class ValidationError {
        private String field;
        private String message;
        private String code;
    }
}