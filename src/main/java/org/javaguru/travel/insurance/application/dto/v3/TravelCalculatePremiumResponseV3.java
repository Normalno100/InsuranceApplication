package org.javaguru.travel.insurance.application.dto.v3;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Ответ на расчёт страховой премии версии V3.
 *
 * Создан как часть нового формата ответа v3.
 *
 * КЛЮЧЕВЫЕ ОТЛИЧИЯ ОТ V2:
 *   1. {@link #personPremiums} — список индивидуальных премий по каждой персоне.
 *      Отсутствует в V2. Позволяет клиенту видеть вклад возраста каждого
 *      застрахованного в итоговую сумму полиса.
 *
 *   2. {@link #pricing} имеет тип {@link PricingSummaryV3} с дополнительным полем
 *      totalPersonsPremium — суммарная базовая премия до скидок полиса.
 *
 * СТРУКТУРА HTTP-СТАТУСОВ (те же, что и в V2):
 *   200 OK          → SUCCESS
 *   400 Bad Request → VALIDATION_ERROR
 *   202 Accepted    → REQUIRES_REVIEW
 *   422 Unprocessable → DECLINED
 *
 * СОВМЕСТИМОСТЬ:
 *   V2 API использует TravelCalculatePremiumResponse — этот класс не затронут.
 *   Данный класс используется только контроллером V3.
 *
 * НА ДАННОМ ЭТАПЕ: только классы без бизнес-логики (согласно task_137).
 * Сборка ответа будет реализована в task_135 (TravelCalculatePremiumServiceV3)
 * и task_134 (ResponseAssembler для V3).
 */
@Schema(
        name = "TravelCalculatePremiumResponseV3",
        description = """
                Ответ на расчёт страховой премии V3 с поддержкой нескольких застрахованных.
                
                Содержит personPremiums[] — индивидуальные премии по каждой персоне из запроса.
                HTTP-статусы: 200 SUCCESS, 400 VALIDATION_ERROR, 202 REQUIRES_REVIEW, 422 DECLINED.
                """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TravelCalculatePremiumResponseV3 {

    // =========================================================
    // МЕТА-ДАННЫЕ ОТВЕТА
    // =========================================================

    @Builder.Default
    private String apiVersion = "3.0";

    @Builder.Default
    private UUID requestId = UUID.randomUUID();

    @Builder.Default
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp = LocalDateTime.now();

    private Boolean success;

    private ResponseStatus status;

    @Builder.Default
    private List<ValidationError> errors = List.of();

    // =========================================================
    // ЦЕНООБРАЗОВАНИЕ
    // =========================================================

    /**
     * Расширенная сводка о ценообразовании полиса.
     * Включает totalPersonsPremium — суммарную базовую премию по всем персонам.
     */
    @Schema(description = "Сводная информация о ценообразовании полиса.")
    private PricingSummaryV3 pricing;

    // =========================================================
    // ПЕРСОНЫ И ИХ ИНДИВИДУАЛЬНЫЕ ПРЕМИИ (новое в V3)
    // =========================================================

    /**
     * Список индивидуальных страховых премий по каждой застрахованной персоне.
     *
     * Порядок элементов совпадает с порядком персон в запросе.
     * Для каждой персоны содержит:
     *   - персональные данные (firstName, lastName, age, ageGroup)
     *   - индивидуальную премию с учётом возрастного коэффициента
     *   - применённый ageCoefficient
     *
     * При VALIDATION_ERROR или DECLINED список может быть пустым или null.
     */
    @Schema(
            description = """
                    Список индивидуальных страховых премий по каждой застрахованной персоне.
                    Порядок совпадает с порядком персон в запросе.
                    Пуст при ошибках валидации или отказе андеррайтинга.
                    """
    )
    private List<PersonPremium> personPremiums;

    // =========================================================
    // ИНФОРМАЦИЯ О ПОЕЗДКЕ
    // =========================================================

    @Schema(description = "Краткая информация о поездке.")
    private TripSummary trip;

    // =========================================================
    // ДЕТАЛИ ЦЕНООБРАЗОВАНИЯ
    // =========================================================

    @Schema(description = "Детальная разбивка расчёта премии (коэффициенты, шаги, риски).")
    private PricingDetails pricingDetails;

    @Schema(description = "Результат андеррайтинга с решением и применёнными правилами.")
    private UnderwritingInfo underwriting;

    @Schema(description = "Список применённых скидок (промо-коды, групповые, корпоративные).")
    private List<AppliedDiscount> appliedDiscounts;

    // =========================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =========================================================

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public boolean isSuccessful() {
        return Boolean.TRUE.equals(success) && status == ResponseStatus.SUCCESS;
    }

    // =========================================================
    // ВЛОЖЕННЫЕ ТИПЫ
    // =========================================================

    public enum ResponseStatus {
        SUCCESS,
        VALIDATION_ERROR,
        DECLINED,
        REQUIRES_REVIEW
    }

    // ── TripSummary ──────────────────────────────────────────────────────────

    /**
     * Краткая информация о поездке.
     * Аналогична V2, но без personFirstName/personLastName (вынесены в personPremiums).
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

        /** Уровень медицинского покрытия (null в режиме COUNTRY_DEFAULT). */
        private String medicalCoverageLevel;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal coverageAmount;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal medicalPayoutLimit;

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        private String calculationMode;
    }

    // ── PricingDetails ───────────────────────────────────────────────────────

    /**
     * Детальная информация о ценообразовании.
     * Идентична V2 PricingDetails — коэффициенты и шаги расчёта.
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

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal countryDefaultDayPremium;

        private CountryInfo countryInfo;

        @Builder.Default
        private List<RiskBreakdown> riskBreakdown = List.of();

        private String calculationFormula;

        @Builder.Default
        private List<CalculationStep> steps = List.of();

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal appliedPayoutLimit;

        private Boolean payoutLimitApplied;
    }

    // ── CountryInfo ──────────────────────────────────────────────────────────

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

        @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
        private BigDecimal defaultDayPremium;

        private String defaultDayPremiumCurrency;
        private Boolean hasDefaultDayPremium;
    }

    // ── RiskBreakdown ────────────────────────────────────────────────────────

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

    // ── CalculationStep ──────────────────────────────────────────────────────

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

    // ── UnderwritingInfo ─────────────────────────────────────────────────────

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

    // ── RuleEvaluation ───────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RuleEvaluation {
        private String ruleName;
        private String severity;
        private String message;
    }

    // ── AppliedDiscount ──────────────────────────────────────────────────────

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

    // ── ValidationError ──────────────────────────────────────────────────────

    /**
     * Ошибка валидации.
     *
     * В V3 поле field адресуется с индексом персоны:
     *   persons[0].personBirthDate — Must not be empty
     *   persons[1].personFirstName — Field must not be null!
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ValidationError {
        private String field;
        private String message;
        private String code;
    }
}