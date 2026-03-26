package org.javaguru.travel.insurance.application.dto.v3;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Запрос на расчёт страховой премии версии V3.
 *
 * task_137: Создан как часть нового формата запроса v3.
 *
 * КЛЮЧЕВОЕ ОТЛИЧИЕ ОТ V2:
 *   V2: одна персона задаётся плоскими полями запроса:
 *         personFirstName / personLastName / personBirthDate / applyAgeCoefficient
 *
 *   V3: список застрахованных {@link #persons} — поддержка нескольких персон
 *       в одном полисе. Каждая персона получает индивидуальный расчёт премии
 *       с учётом своего возраста.
 *
 * ОБЩИЕ ПАРАМЕТРЫ ПОЕЗДКИ остаются на уровне запроса (одинаковы для всех персон):
 *   agreementDateFrom, agreementDateTo, countryIsoCode, medicalRiskLimitLevel,
 *   selectedRisks, currency, promoCode, personsCount, isCorporate,
 *   useCountryDefaultPremium.
 *
 * ОШИБКИ ВАЛИДАЦИИ адресуются с индексом персоны:
 *   persons[0].personBirthDate — Must not be empty
 *   persons[1].personFirstName — Field must not be null!
 *
 * СОВМЕСТИМОСТЬ:
 *   V2 API (TravelCalculatePremiumRequest) не изменяется.
 *   Этот класс используется только контроллером V3 и сервисным слоем V3.
 *
 * НА ДАННОМ ЭТАПЕ: только классы без бизнес-логики (согласно task_137).
 * Логика расчёта будет добавлена в task_134 (MultiPersonPremiumCalculationService).
 */
@Schema(
        name = "TravelCalculatePremiumRequestV3",
        description = """
                Запрос на расчёт страховой премии V3 с поддержкой нескольких застрахованных.
                
                Отличие от V2: поле persons[] вместо одиночных полей personFirstName/personLastName/personBirthDate.
                Общие параметры поездки (страна, даты, риски) применяются ко всем персонам одинаково.
                """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCalculatePremiumRequestV3 {

    // =========================================================
    // СПИСОК ЗАСТРАХОВАННЫХ ПЕРСОН (новое в V3)
    // =========================================================

    /**
     * Список застрахованных персон.
     *
     * БИЗНЕС-ПРАВИЛА:
     * - Список не должен быть null или пустым.
     * - Минимум 1 персона, максимум — определяется бизнес-правилами.
     * - Для каждой персоны вычисляется индивидуальная премия с учётом возраста.
     * - Итоговая премия полиса = сумма индивидуальных премий − скидки полиса.
     *
     * ОШИБКИ ВАЛИДАЦИИ адресуются с индексом:
     *   persons[0].personBirthDate — Must not be empty
     */
    @Schema(
            description = """
                    Список застрахованных персон. Минимум 1 персона.
                    Для каждой персоны рассчитывается индивидуальная премия с учётом возраста.
                    Итоговая премия = сумма индивидуальных премий − скидки полиса.
                    
                    Ошибки валидации адресуются с индексом: persons[0].personBirthDate
                    """,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private List<InsuredPerson> persons;

    // =========================================================
    // ДАТЫ ПОЕЗДКИ (применяются ко всем персонам)
    // =========================================================

    @Schema(
            description = "Дата начала действия страхового соглашения. Формат: yyyy-MM-dd.",
            example = "2025-06-01",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @Schema(
            description = "Дата окончания действия страхового соглашения. Формат: yyyy-MM-dd.",
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
            description = "ISO 3166-1 alpha-2 код страны назначения (2 заглавные буквы).",
            example = "ES",
            requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = 2,
            maxLength = 2,
            pattern = "^[A-Z]{2}$"
    )
    private String countryIsoCode;

    @Schema(
            description = """
                    Уровень медицинского покрытия.
                    Обязателен в режиме MEDICAL_LEVEL (useCountryDefaultPremium = false/null).
                    Не требуется в режиме COUNTRY_DEFAULT (useCountryDefaultPremium = true).
                    """,
            example = "LEVEL_50000",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    private String medicalRiskLimitLevel;

    @Schema(
            description = """
                    Режим расчёта премии.
                    false/null → MEDICAL_LEVEL (стандартный, требует medicalRiskLimitLevel).
                    true       → COUNTRY_DEFAULT (через дефолтную ставку страны).
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
                    Обязательный риск TRAVEL_MEDICAL добавляется автоматически.
                    Применяются ко всем персонам полиса.
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

    /**
     * Количество застрахованных лиц для расчёта групповых скидок.
     *
     * ПРИМЕЧАНИЕ: В V3 фактическое количество персон определяется списком {@link #persons}.
     * Поле personsCount оставлено для обратной совместимости и групповых скидок
     * при использовании personsCount > persons.size() (например, дополнительные
     * незарегистрированные участники).
     * Если personsCount не задан или ≤ 0, используется persons.size().
     */
    @Schema(
            description = """
                    Количество застрахованных лиц для расчёта групповых скидок.
                    Если не задан — используется размер списка persons.
                    """,
            example = "3",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            minimum = "1"
    )
    private Integer personsCount;

    @Schema(
            description = "Признак корпоративного клиента. Если true — применяется корпоративная скидка −20%.",
            example = "false",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            defaultValue = "false"
    )
    private Boolean isCorporate;
}