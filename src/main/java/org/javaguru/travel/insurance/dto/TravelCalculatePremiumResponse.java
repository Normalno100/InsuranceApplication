package org.javaguru.travel.insurance.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Ответ на расчет страховой премии
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelCalculatePremiumResponse extends CoreResponse {

    // Персональные данные
    private String personFirstName;
    private String personLastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;
    private Integer personAge;

    // Даты и период
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;
    private Integer agreementDays;

    // Страна и покрытие
    private String countryIsoCode;
    private String countryName;
    private String medicalRiskLimitLevel;
    private BigDecimal coverageAmount;

    // Выбранные риски
    private List<String> selectedRisks;
    private List<RiskPremium> riskPremiums;

    // Цены
    private BigDecimal agreementPriceBeforeDiscount;
    private BigDecimal discountAmount;
    private BigDecimal agreementPrice;
    private String currency;

    // Детали расчета
    private CalculationDetails calculation;

    // Промо-коды и скидки
    private PromoCodeInfo promoCodeInfo;
    private List<DiscountInfo> appliedDiscounts;

    //  Информация о пакете рисков (ИДЕЯ #2)
    private BundleInfo appliedBundle;

    // Андеррайтинг
    private String underwritingDecision;
    private String declineReason;
    private String reviewReason;

    /**
     *  Детали премии по риску (с возрастным модификатором)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskPremium {
        private String riskType;
        private String riskName;
        private BigDecimal premium;
        private BigDecimal coefficient;

        // 🆕 НОВОЕ: возрастной модификатор (ИДЕЯ #5)
        private BigDecimal ageModifier;
    }

    /**
     *  Детали расчета
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationDetails {
        private BigDecimal baseRate;
        private BigDecimal ageCoefficient;
        private BigDecimal countryCoefficient;

        // коэффициент длительности
        private BigDecimal durationCoefficient;

        private BigDecimal additionalRisksCoefficient;
        private BigDecimal totalCoefficient;
        private Integer days;
        private String formula;
        private List<CalculationStep> steps;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CalculationStep {
        private String description;
        private String formula;
        private BigDecimal result;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PromoCodeInfo {
        private String code;
        private String description;
        private String discountType;
        private BigDecimal discountValue;
        private BigDecimal actualDiscountAmount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiscountInfo {
        private String discountType;
        private String name;
        private BigDecimal percentage;
        private BigDecimal amount;
    }

    /**
     * Информация о примененном пакете рисков
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BundleInfo {
        private String code;
        private String name;
        private BigDecimal discountPercentage;
        private BigDecimal discountAmount;
        private List<String> includedRisks;
    }
}