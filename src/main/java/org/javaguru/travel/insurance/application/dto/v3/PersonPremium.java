package org.javaguru.travel.insurance.application.dto.v3;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * Вложенный DTO с индивидуальной страховой премией одной застрахованной персоны.
 *
 * Создан как часть нового формата ответа V3.
 *
 * Используется в {@link TravelCalculatePremiumResponseV3#personPremiums} —
 * содержит детали расчёта для каждого застрахованного из списка запроса.
 *
 * Порядок элементов совпадает с порядком персон в запросе.
 */
@Schema(
        name = "PersonPremium",
        description = """
                Индивидуальная страховая премия одной застрахованной персоны в ответе V3.
                Содержит итоговую премию, возрастной коэффициент и сведения о персоне.
                """
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PersonPremium {

    @Schema(
            description = "Имя застрахованного лица.",
            example = "Ivan"
    )
    private String firstName;

    @Schema(
            description = "Фамилия застрахованного лица.",
            example = "Petrov"
    )
    private String lastName;

    @Schema(
            description = "Полных лет на дату начала поездки (agreementDateFrom).",
            example = "35"
    )
    private Integer age;

    @Schema(
            description = "Описание возрастной группы (например: Adults, Senior, Very elderly).",
            example = "Adults"
    )
    private String ageGroup;

    /**
     * Базовая страховая премия для данной персоны до применения скидок.
     * Рассчитывается по формуле: BaseRate × AgeCoeff × CountryCoeff × DurationCoeff × Days.
     */
    @Schema(
            description = """
                    Базовая страховая премия для данной персоны (до применения скидок к полису).
                    Рассчитывается с учётом возрастного коэффициента персоны.
                    """,
            example = "69.30"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.00")
    private BigDecimal premium;

    /**
     * Применённый возрастной коэффициент для данной персоны.
     * Значение 1.0 означает нейтральный коэффициент (отключён или возраст 18–30 лет).
     */
    @Schema(
            description = """
                    Возрастной коэффициент, применённый для данной персоны.
                    1.0 — нейтральный (Young adults 18–30 или коэффициент отключён).
                    Значения > 1.0 увеличивают премию (старший возраст).
                    Значение 0.9 — скидка для детей (6–17 лет).
                    """,
            example = "1.10"
    )
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, pattern = "0.0000")
    private BigDecimal ageCoefficient;
}