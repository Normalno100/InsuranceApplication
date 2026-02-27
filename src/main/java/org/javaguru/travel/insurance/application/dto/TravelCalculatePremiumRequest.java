package org.javaguru.travel.insurance.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Запрос на расчёт страховой премии.
 */
@Schema(
        name = "TravelCalculatePremiumRequest",
        description = """
                Запрос на расчёт страховой премии по медицинскому риску.
                
                Поддерживает два режима расчёта:
                  • MEDICAL_LEVEL (стандартный) — расчёт через уровень медицинского покрытия.
                    Поле medicalRiskLimitLevel обязательно.
                  • COUNTRY_DEFAULT — расчёт через дефолтную дневную ставку страны.
                    Для активации передайте useCountryDefaultPremium=true.
                    Поле medicalRiskLimitLevel необязательно.
                """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCalculatePremiumRequest {

    // =========================================================
    // ПЕРСОНАЛЬНЫЕ ДАННЫЕ
    // =========================================================

    @Schema(
            description = "Имя застрахованного лица. Только латиница или кириллица, 1–100 символов.",
            example = "Ivan",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 100
    )
    private String personFirstName;

    @Schema(
            description = "Фамилия застрахованного лица. Только латиница или кириллица, 1–100 символов.",
            example = "Petrov",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 1,
            maxLength = 100
    )
    private String personLastName;

    /**
     * Дата рождения застрахованного.
     *
     * БИЗНЕС-ПРАВИЛА:
     * 1. Обязательное поле — не может быть null.
     * 2. Должна быть в прошлом (не сегодня, не будущее).
     * 3. Возраст на дату начала поездки: от 0 до 80 лет включительно.
     * 4. Используется для расчёта AgeCoefficient (если не отключён через applyAgeCoefficient).
     */
    @Schema(
            description = """
                    Дата рождения застрахованного лица. Формат: yyyy-MM-dd.
                    
                    Бизнес-правила:
                    • Обязательное поле.
                    • Должна быть в прошлом.
                    • Возраст на дату начала поездки (agreementDateFrom): от 0 до 80 лет.
                    • Возраст 75–80 лет: расчёт возможен, но может потребоваться ручная проверка.
                    
                    Используется для расчёта возрастного коэффициента (AgeCoefficient):
                      0–5 лет: ×1.1,  6–17: ×0.9,  18–30: ×1.0,  31–40: ×1.1,
                      41–50: ×1.3,  51–60: ×1.6,  61–70: ×2.0,  71–80: ×2.5
                    """,
            example = "1990-05-15",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    @Schema(
            description = "Email застрахованного. Необязательное поле.",
            example = "ivan.petrov@example.com",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String personEmail;

    @Schema(
            description = "Телефон застрахованного. Необязательное поле.",
            example = "+79001234567",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String personPhone;

    // =========================================================
    // ДАТЫ ПОЕЗДКИ
    // =========================================================

    @Schema(
            description = """
                    Дата начала действия страхового соглашения (начало поездки).
                    Формат: yyyy-MM-dd.
                    """,
            example = "2025-06-01",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @Schema(
            description = """
                    Дата окончания действия страхового соглашения (конец поездки).
                    Формат: yyyy-MM-dd.
                    """,
            example = "2025-06-15",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;

    // =========================================================
    // СТРАНА И ПОКРЫТИЕ
    // =========================================================

    @Schema(
            description = "ISO 3166-1 alpha-2 код страны назначения (2 заглавные латинские буквы).",
            example = "ES",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 2,
            pattern = "^[A-Z]{2}$"
    )
    private String countryIsoCode;

    @Schema(
            description = """
                    Уровень медицинского покрытия (код из справочника medical_risk_limit_levels).
                    
                    ⚠️ ОБЯЗАТЕЛЬНОСТЬ ЗАВИСИТ ОТ РЕЖИМА РАСЧЁТА:
                    • useCountryDefaultPremium = false (или null) → поле ОБЯЗАТЕЛЬНО.
                    • useCountryDefaultPremium = true → поле НЕ ОБЯЗАТЕЛЬНО (игнорируется).
                    
                    Известные значения: LEVEL_10000, LEVEL_30000, LEVEL_50000,
                    LEVEL_100000, LEVEL_200000.
                    """,
            example = "LEVEL_30000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String medicalRiskLimitLevel;

    @Schema(
            description = """
                    Режим расчёта премии.
                    
                    false / null → MEDICAL_LEVEL (стандартный):
                      Формула: DailyRate × AgeCoeff × CountryCoeff × DurationCoeff × Days
                      Требует: medicalRiskLimitLevel (обязательно)
                    
                    true → COUNTRY_DEFAULT:
                      Формула: DefaultDayPremium × AgeCoeff × DurationCoeff × Days
                      medicalRiskLimitLevel необязателен.
                    """,
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            defaultValue = "false"
    )
    private Boolean useCountryDefaultPremium;

    // =========================================================
    // УПРАВЛЕНИЕ ВОЗРАСТНЫМ КОЭФФИЦИЕНТОМ (task_116)
    // =========================================================

    /**
     * Switch on/off для возрастного коэффициента (AgeCoefficient).
     *
     * ПРИОРИТЕТ (Вариант D — комбинированный):
     *   null → использовать глобальную настройку из calculation_config
     *           (ключ AGE_COEFFICIENT_ENABLED, по умолчанию true).
     *   true → принудительно применить коэффициент для этого запроса.
     *   false → принудительно отключить коэффициент (AgeCoeff = 1.0).
     *
     * ВЛИЯНИЕ НА РАСЧЁТ:
     *   enabled=true  → ПРЕМИЯ = BaseRate × AgeCoeff × ...
     *   enabled=false → ПРЕМИЯ = BaseRate × 1.0 × ...  (возрастная надбавка не применяется)
     *
     * ТИПИЧНЫЕ СЛУЧАИ ИСПОЛЬЗОВАНИЯ:
     *   - Специальные корпоративные тарифы без возрастной дифференциации.
     *   - Тестирование расчётов без возрастного коэффициента.
     *   - Временное отключение через глобальную настройку в БД (не передавать поле).
     */
    @Schema(
            description = """
                    Switch on/off для возрастного коэффициента (AgeCoefficient).
                    
                    null (не передавать) → используется глобальная настройка из БД
                      (calculation_config: AGE_COEFFICIENT_ENABLED, по умолчанию true).
                    true  → коэффициент применяется для этого запроса (override).
                    false → коэффициент = 1.0 для этого запроса (override, отключён).
                    
                    При отключении:
                      PREMIUM = BaseRate × 1.0 × OtherCoefficients × Days
                    """,
            example = "null",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true
    )
    private Boolean applyAgeCoefficient;

    // =========================================================
    // ДОПОЛНИТЕЛЬНЫЕ РИСКИ
    // =========================================================

    @Schema(
            description = """
                    Список кодов дополнительных рисков (из справочника risk_types).
                    Обязательный риск TRAVEL_MEDICAL добавляется автоматически.
                    """,
            example = "[\"SPORT_ACTIVITIES\", \"LUGGAGE_LOSS\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<String> selectedRisks;

    // =========================================================
    // ВАЛЮТА И ПРОМО-КОД
    // =========================================================

    @Schema(
            description = "Валюта расчёта. Если не указана — используется EUR.",
            example = "EUR",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            allowableValues = {"EUR", "USD", "GBP", "CHF", "JPY"},
            defaultValue = "EUR"
    )
    private String currency;

    @Schema(
            description = "Промо-код для получения скидки. Необязательное поле.",
            example = "SUMMER2025",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String promoCode;

    // =========================================================
    // ГРУППОВОЕ / КОРПОРАТИВНОЕ СТРАХОВАНИЕ
    // =========================================================

    @Schema(
            description = """
                    Количество застрахованных лиц. Используется для расчёта групповых скидок.
                    Если не указано или ≤ 0 — считается как 1 (одно лицо).
                    """,
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1"
    )
    private Integer personsCount;

    @Schema(
            description = """
                    Признак корпоративного клиента. Если true — применяется корпоративная скидка −20%.
                    """,
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            defaultValue = "false"
    )
    private Boolean isCorporate;
}