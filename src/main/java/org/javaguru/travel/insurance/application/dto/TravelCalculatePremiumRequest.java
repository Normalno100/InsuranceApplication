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
     * БИЗНЕС-ПРАВИЛА (задокументированы в task_111):
     * 1. Обязательное поле — не может быть null.
     * 2. Должна быть в прошлом (не сегодня, не будущее).
     * 3. Возраст на дату начала поездки: от 0 до 80 лет включительно.
     *    - Менее 0 лет: ошибка валидации.
     *    - Более 80 лет: ошибка валидации (страхование недоступно).
     *    - 75–80 лет: расчёт возможен, но андеррайтинг может потребовать
     *      ручной проверки (REQUIRES_REVIEW).
     * 4. Используется для:
     *    - Расчёта возрастного коэффициента (AgeCoefficient) в формуле премии.
     *    - Расчёта возрастных модификаторов дополнительных рисков.
     *    - Проверки правил андеррайтинга (AgeRule, AdditionalRisksRule).
     *    - Сохранения в БД (UnderwritingDecisionEntity.personBirthDate).
     *
     * ФОРМАТ: yyyy-MM-dd (ISO 8601), например "1990-05-15".
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
                    
                    Бизнес-правила:
                    • Обязательное поле.
                    • Должна быть не позже чем через 365 дней от сегодня.
                    • Должна быть ≤ agreementDateTo.
                    • Если дата в прошлом — предупреждение (WARNING), но расчёт выполняется.
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
                    
                    Бизнес-правила:
                    • Обязательное поле.
                    • Должна быть ≥ agreementDateFrom.
                    • Максимальная длительность поездки: 365 дней.
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
            description = """
                    ISO 3166-1 alpha-2 код страны назначения (2 заглавные латинские буквы).
                    Страна должна быть активна в справочнике на дату начала поездки.
                    """,
            example = "ES",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 2,
            pattern = "^[A-Z]{2}$"
    )
    private String countryIsoCode;

    /**
     * Уровень медицинского покрытия.
     *
     * ОБЯЗАТЕЛЬНОСТЬ ЗАВИСИТ ОТ РЕЖИМА РАСЧЁТА (задокументировано в task_111):
     *
     * Режим MEDICAL_LEVEL (useCountryDefaultPremium = false или null):
     *   → medicalRiskLimitLevel ОБЯЗАТЕЛЕН.
     *   → Определяет сумму покрытия (coverageAmount) и дневную ставку (dailyRate).
     *
     * Режим COUNTRY_DEFAULT (useCountryDefaultPremium = true):
     *   → medicalRiskLimitLevel НЕОБЯЗАТЕЛЕН (но если передан — игнорируется).
     *   → Базовая ставка берётся из country_default_day_premiums.
     *
     * Известные значения (из справочника):
     *   LEVEL_10000, LEVEL_30000, LEVEL_50000, LEVEL_100000, LEVEL_200000
     */
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

    /**
     * Флаг использования дефолтной дневной премии страны.
     *
     * РЕЖИМ РАСЧЁТА (задокументировано в task_111):
     *
     * false / null → MEDICAL_LEVEL (стандартный):
     *   ПРЕМИЯ = DailyRate × AgeCoeff × CountryCoeff × DurationCoeff × Days − BundleDiscount
     *   Источник базовой ставки: medical_risk_limit_levels.daily_rate
     *
     * true → COUNTRY_DEFAULT:
     *   ПРЕМИЯ = DefaultDayPremium × AgeCoeff × DurationCoeff × Days − BundleDiscount
     *   Источник базовой ставки: country_default_day_premiums.default_day_premium
     *   CountryCoeff НЕ применяется (уже включён в DefaultDayPremium)
     *
     * FALLBACK: Если для страны нет записи в country_default_day_premiums,
     * автоматически используется MEDICAL_LEVEL (с предупреждением в логах).
     */
    @Schema(
            description = """
                    Режим расчёта премии.
                    
                    false / null → MEDICAL_LEVEL (стандартный):
                      Формула: DailyRate × AgeCoeff × CountryCoeff × DurationCoeff × Days
                      Требует: medicalRiskLimitLevel (обязательно)
                    
                    true → COUNTRY_DEFAULT:
                      Формула: DefaultDayPremium × AgeCoeff × DurationCoeff × Days
                      medicalRiskLimitLevel необязателен.
                      Если для страны нет дефолтной ставки — fallback на MEDICAL_LEVEL.
                    """,
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            defaultValue = "false"
    )
    private Boolean useCountryDefaultPremium;

    // =========================================================
    // ДОПОЛНИТЕЛЬНЫЕ РИСКИ
    // =========================================================

    @Schema(
            description = """
                    Список кодов дополнительных рисков (из справочника risk_types).
                    
                    Правила:
                    • Необязательное поле.
                    • Обязательный риск TRAVEL_MEDICAL добавляется автоматически — не включайте его.
                    • Если включён TRAVEL_MEDICAL — будет ошибка валидации.
                    • Дубликаты в списке — ошибка валидации.
                    
                    Примеры доступных кодов: SPORT_ACTIVITIES, EXTREME_SPORT,
                    PREGNANCY, CHRONIC_DISEASES, ACCIDENT_COVERAGE,
                    TRIP_CANCELLATION, LUGGAGE_LOSS, FLIGHT_DELAY, CIVIL_LIABILITY.
                    """,
            example = "[\"SPORT_ACTIVITIES\", \"LUGGAGE_LOSS\"]",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private List<String> selectedRisks;

    // =========================================================
    // ВАЛЮТА И ПРОМО-КОД
    // =========================================================

    @Schema(
            description = """
                    Валюта расчёта. Если не указана — используется EUR.
                    Поддерживаемые валюты: EUR, USD, GBP, CHF, JPY.
                    """,
            example = "EUR",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            allowableValues = {"EUR", "USD", "GBP", "CHF", "JPY"},
            defaultValue = "EUR"
    )
    private String currency;

    @Schema(
            description = """
                    Промо-код для получения скидки. Необязательное поле.
                    Промо-коды проверяются по таблице promo_codes (активность, период, лимит).
                    """,
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
                    
                    Групповые скидки:
                      5–9 человек:  −10%
                      10–19 человек: −15%
                      20+ человек:   −20%
                    """,
            example = "1",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1"
    )
    private Integer personsCount;

    @Schema(
            description = """
                    Признак корпоративного клиента. Если true — применяется корпоративная скидка −20%
                    (при сумме премии от 100 EUR).
                    Групповые и корпоративные скидки не суммируются — берётся наибольшая.
                    """,
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            defaultValue = "false"
    )
    private Boolean isCorporate;
}