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
 * ОСОБЕННОСТИ:
 * 1. Метаданные запроса (requestId, timestamp, version)
 * 2. Разделение на summary (краткая инфо) и details (детали)
 * 3. Четкое разделение concerns: pricing, underwriting, discounts
 * 4. Опциональные детали расчета через includeDetails параметр
 * 5. Единый список errors без наследования
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
    private String apiVersion = "2.0";

    @Builder.Default
    private UUID requestId = UUID.randomUUID();

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Boolean success;  // true если премия рассчитана, false если есть ошибки

    // ========================================
    // СТАТУС И ОШИБКИ
    // ========================================

    private ResponseStatus status;  // SUCCESS, VALIDATION_ERROR, DECLINED, REQUIRES_REVIEW

    @Builder.Default
    private List<ValidationError> errors = List.of();  // Список ошибок валидации

    // ========================================
    // КРАТКАЯ ИНФОРМАЦИЯ (SUMMARY)
    // ========================================

    private PricingSummary pricing;  // Краткая информация о цене

    private PersonSummary person;  // Краткая информация о персоне

    private TripSummary trip;  // Краткая информация о поездке

    // ========================================
    // ДЕТАЛЬНАЯ ИНФОРМАЦИЯ (ОПЦИОНАЛЬНО)
    // ========================================

    private PricingDetails pricingDetails;  // Подробная разбивка расчета

    private UnderwritingInfo underwriting;  // Информация об андеррайтинге

    private List<AppliedDiscount> appliedDiscounts;  // Примененные скидки

    // ========================================
    // ВЛОЖЕННЫЕ КЛАССЫ
    // ========================================

    /**
     * Статус ответа
     */
    public enum ResponseStatus {
        SUCCESS,              // Успешный расчет
        VALIDATION_ERROR,     // Ошибки валидации
        DECLINED,            // Заявка отклонена
        REQUIRES_REVIEW      // Требуется ручная проверка
    }

    /**
     * Краткая информация о ценообразовании
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricingSummary {
        private BigDecimal totalPremium;  // Итоговая премия
        private BigDecimal baseAmount;    // Базовая сумма до скидок
        private BigDecimal totalDiscount; // Общая скидка
        private String currency;

        @Builder.Default
        private List<String> includedRisks = List.of();  // Список включенных рисков
    }

    /**
     * Краткая информация о персоне
     */
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
        private String ageGroup;  // "Young adults", "Elderly", etc.
    }

    /**
     * Краткая информация о поездке
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

        private String medicalCoverageLevel;
        private BigDecimal coverageAmount;
    }

    /**
     * Детальная информация о ценообразовании
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PricingDetails {
        // Коэффициенты
        private BigDecimal baseRate;
        private BigDecimal ageCoefficient;
        private BigDecimal countryCoefficient;
        private BigDecimal durationCoefficient;

        // Разбивка по рискам
        @Builder.Default
        private List<RiskBreakdown> riskBreakdown = List.of();

        // Формула расчета (опционально, для отладки)
        private String calculationFormula;

        // Пошаговые расчеты (опционально, для аудита)
        @Builder.Default
        private List<CalculationStep> steps = List.of();
    }

    /**
     * Разбивка по риску
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RiskBreakdown {
        private String riskCode;
        private String riskName;
        private BigDecimal premium;
        private BigDecimal baseCoefficient;
        private BigDecimal ageModifier;  // Возрастной модификатор
        private Boolean isMandatory;
    }

    /**
     * Шаг расчета (для детального аудита)
     */
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

    /**
     * Информация об андеррайтинге
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnderwritingInfo {
        private String decision;  // APPROVED, DECLINED, REQUIRES_MANUAL_REVIEW
        private String reason;    // Причина отклонения или запроса проверки

        @Builder.Default
        private List<RuleEvaluation> evaluatedRules = List.of();
    }

    /**
     * Результат оценки правила
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleEvaluation {
        private String ruleName;
        private String severity;  // PASS, WARNING, REVIEW_REQUIRED, BLOCKING
        private String message;
    }

    /**
     * Примененная скидка
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AppliedDiscount {
        private String type;        // PROMO_CODE, BUNDLE, GROUP, CORPORATE, etc.
        private String code;        // Код промо/пакета
        private String description;
        private BigDecimal amount;
        private BigDecimal percentage;
    }

    // ========================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Проверка на наличие ошибок
     */
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    /**
     * Проверка на успешность
     */
    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success) && status == ResponseStatus.SUCCESS;
    }


    /**
     * ValidationError - встроенный класс для ошибок валидации
     */
    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
        private String code;  // Опциональный код ошибки для i18n
    }
}