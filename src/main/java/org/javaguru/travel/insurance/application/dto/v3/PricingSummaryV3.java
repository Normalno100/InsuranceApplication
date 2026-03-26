package org.javaguru.travel.insurance.application.dto.v3;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Расширенная сводка о ценообразовании для ответа V3.
 *
 * Создан как часть нового формата ответа V3.
 *
 * Отличается от V2 PricingSummary наличием поля {@link #totalPersonsPremium} —
 * суммарной базовой премии по всем застрахованным персонам до применения
 * скидок полиса. Позволяет клиенту понять вклад каждой персоны в итоговую сумму.
 *
 * СТРУКТУРА СТОИМОСТИ:
 *   totalPersonsPremium — сумма базовых премий всех персон (до скидок полиса)
 *   baseAmount          — то же самое (для обратной совместимости с V2 клиентами)
 *   totalDiscount       — общая сумма всех применённых скидок
 *   totalPremium        — итоговая сумма к оплате (baseAmount − totalDiscount)
 */
@Schema(
        name = "PricingSummaryV3",
        description = """
                Расширенная сводка о ценообразовании полиса V3.
                Включает totalPersonsPremium — суммарную базовую премию по всем персонам.
                """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PricingSummaryV3 {

    /**
     * Итоговая сумма к оплате — после применения всех скидок.
     * totalPremium = totalPersonsPremium − totalDiscount
     */
    @Schema(
            description = "Итоговая страховая премия к оплате после применения всех скидок.",
            example = "156.24"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal totalPremium;

    /**
     * Суммарная базовая премия по всем застрахованным персонам до скидок.
     *
     * Для одной персоны совпадает с её индивидуальной премией.
     * Для группы — сумма PersonPremium.premium по всем персонам.
     *
     * Ключевое поле V3: позволяет отслеживать вклад возраста каждой
     * персоны в общую стоимость полиса.
     */
    @Schema(
            description = """
                    Суммарная базовая премия по всем застрахованным персонам до применения скидок полиса.
                    Равна сумме значений PersonPremium.premium для всех персон в списке.
                    Для группы из N персон отражает общий вклад возрастных коэффициентов.
                    """,
            example = "195.30"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal totalPersonsPremium;

    /**
     * Базовая сумма премии (до скидок) — синоним totalPersonsPremium для V2-совместимости.
     */
    @Schema(
            description = "Базовая сумма страховой премии до применения скидок. Совпадает с totalPersonsPremium.",
            example = "195.30"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal baseAmount;

    /**
     * Общая сумма всех применённых скидок (промо-коды, групповые, корпоративные).
     */
    @Schema(
            description = "Суммарная скидка по всем применённым дисконтам.",
            example = "39.06"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal totalDiscount;

    @Schema(
            description = "Валюта расчёта (ISO 4217).",
            example = "EUR"
    )
    private String currency;

    @Schema(
            description = "Список кодов дополнительных рисков, включённых в полис.",
            example = "[\"SPORT_ACTIVITIES\", \"LUGGAGE_LOSS\"]"
    )
    @Builder.Default
    private List<String> includedRisks = List.of();
}