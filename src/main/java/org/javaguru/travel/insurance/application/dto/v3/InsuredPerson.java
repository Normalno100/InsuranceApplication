package org.javaguru.travel.insurance.application.dto.v3;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;

/**
 * Вложенный DTO одной застрахованной персоны для запроса V3.
 *
 * task_137: Создан как часть нового формата запроса v3.
 *
 * Используется в {@link TravelCalculatePremiumRequestV3#persons} —
 * заменяет плоские поля personFirstName/personLastName/personBirthDate
 * из V2, позволяя передавать список застрахованных.
 *
 * Ошибки валидации адресуются с индексом персоны:
 *   persons[0].personBirthDate — Must not be empty
 */
@Schema(
        name = "InsuredPerson",
        description = "Данные одной застрахованной персоны в запросе V3."
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InsuredPerson {

    @Schema(
            description = "Имя застрахованного лица. Только латиница или кириллица, 1–100 символов.",
            example = "Ivan",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String personFirstName;

    @Schema(
            description = "Фамилия застрахованного лица. Только латиница или кириллица, 1–100 символов.",
            example = "Petrov",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String personLastName;

    /**
     * Дата рождения застрахованного.
     * Используется для расчёта возраста и возрастного коэффициента.
     */
    @Schema(
            description = """
                    Дата рождения застрахованного лица. Формат: yyyy-MM-dd.
                    Должна быть в прошлом. Возраст на дату начала поездки: от 0 до 80 лет.
                    """,
            example = "1990-05-15",
            requiredMode = Schema.RequiredMode.REQUIRED,
            format = "date"
    )
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    /**
     * Управление возрастным коэффициентом для конкретной персоны.
     *
     * null   → использовать глобальную настройку (AGE_COEFFICIENT_ENABLED).
     * true   → принудительно применить коэффициент для данной персоны.
     * false  → принудительно отключить коэффициент (AgeCoeff = 1.0).
     */
    @Schema(
            description = """
                    Switch on/off для возрастного коэффициента конкретной персоны.
                    null (по умолчанию) — используется глобальная настройка из БД.
                    true  — коэффициент применяется.
                    false — коэффициент = 1.0 (отключён).
                    """,
            example = "null",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED,
            nullable = true
    )
    private Boolean applyAgeCoefficient;
}