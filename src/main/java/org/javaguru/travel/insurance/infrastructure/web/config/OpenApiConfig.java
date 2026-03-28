package org.javaguru.travel.insurance.infrastructure.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

/**
 * Конфигурация OpenAPI (Swagger) для Travel Insurance API.
 *
 * ОБНОВЛЕНО в task_135:
 * - Добавлены теги и примеры для V3 API (multi-person)
 * - Обновлена версия API до 3.0
 * - Добавлены примеры V3 запросов
 *
 * Доступ к Swagger UI: http://localhost:8080/swagger-ui.html
 * OpenAPI JSON:        http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI travelInsuranceOpenAPI() {
        return new OpenAPI()
                .info(buildApiInfo())
                .servers(buildServers())
                .tags(buildTags())
                .components(buildComponents());
    }

    private Info buildApiInfo() {
        return new Info()
                .title("Travel Insurance API")
                .version("3.0")
                .description("""
                        ## Travel Insurance Premium Calculation API
                        
                        Сервис расчёта страховых премий для туристических поездок.
                        
                        ### API Versions
                        
                        **V2 (legacy)** — `/insurance/travel/calculate`
                        - Одна застрахованная персона
                        - Поля: personFirstName, personLastName, personBirthDate
                        
                        **V3 (current)** — `/insurance/travel/v3/calculate`
                        - Несколько застрахованных персон
                        - Поле: persons[] с InduredPerson объектами
                        - Ответ содержит personPremiums[] с индивидуальными расчётами
                        
                        ### Два режима расчёта
                        
                        **MEDICAL_LEVEL** (стандартный):
                        ```
                        Premium = DailyRate × AgeCoeff × CountryCoeff × DurationCoeff
                                  × (1 + Σ riskCoeffs) × Days − BundleDiscount
                        ```
                        - Требует поле `medicalRiskLimitLevel`
                        - Источник ставки: таблица `medical_risk_limit_levels`
                        
                        **COUNTRY_DEFAULT** (useCountryDefaultPremium=true):
                        ```
                        Premium = DefaultDayPremium × AgeCoeff × DurationCoeff
                                  × (1 + Σ riskCoeffs) × Days − BundleDiscount
                        ```
                        - Поле `medicalRiskLimitLevel` не требуется
                        - Источник ставки: таблица `country_default_day_premiums`
                        - CountryCoeff уже включён в DefaultDayPremium
                        
                        ### HTTP-статусы (V2 и V3)
                        
                        | Статус | Описание |
                        |--------|----------|
                        | 200 OK | SUCCESS — расчёт успешен |
                        | 400 Bad Request | VALIDATION_ERROR — ошибки валидации |
                        | 202 Accepted | REQUIRES_REVIEW — требуется ручная проверка |
                        | 422 Unprocessable Entity | DECLINED — заявка отклонена |
                        
                        ### Возрастные коэффициенты (AgeCoefficient)
                        
                        | Возраст    | Коэффициент | Группа              |
                        |-----------|-------------|---------------------|
                        | 0 – 5     | 1.1         | Infants/toddlers    |
                        | 6 – 17    | 0.9         | Children/teenagers  |
                        | 18 – 30   | 1.0         | Young adults (base) |
                        | 31 – 40   | 1.1         | Adults              |
                        | 41 – 50   | 1.3         | Middle-aged         |
                        | 51 – 60   | 1.6         | Senior              |
                        | 61 – 70   | 2.0         | Elderly             |
                        | 71 – 80   | 2.5         | Very elderly        |
                        """)
                .contact(new Contact()
                        .name("JavaGuru Travel Insurance Team")
                        .email("dev@javaguru.org"))
                .license(new License()
                        .name("Internal Use Only"));
    }

    private List<Server> buildServers() {
        return List.of(
                new Server().url("http://localhost:8080").description("Local Development"),
                new Server().url("https://api.travel-insurance.javaguru.org").description("Production")
        );
    }

    private List<Tag> buildTags() {
        return List.of(
                new Tag()
                        .name("Premium Calculation V2")
                        .description("V2 API: расчёт страховой премии для одной персоны (legacy)"),
                new Tag()
                        .name("Premium Calculation V3")
                        .description("V3 API: расчёт страховой премии для нескольких персон (current)"),
                new Tag()
                        .name("Health")
                        .description("Проверка состояния сервиса")
        );
    }

    private Components buildComponents() {
        return new Components()
                .examples(Map.of(
                        // V2 примеры
                        "MedicalLevelRequest", buildMedicalLevelExample(),
                        "CountryDefaultRequest", buildCountryDefaultExample(),
                        "ElderlyRequest", buildElderlyExample(),
                        "WithDiscountsRequest", buildWithDiscountsExample(),
                        // V3 примеры
                        "V3SinglePersonRequest", buildV3SinglePersonExample(),
                        "V3MultiPersonRequest", buildV3MultiPersonExample(),
                        "V3WithDiscountsRequest", buildV3WithDiscountsExample()
                ));
    }

    // ── V2 примеры ────────────────────────────────────────────────────────────

    private Example buildMedicalLevelExample() {
        return new Example()
                .summary("[V2] Стандартный запрос (MEDICAL_LEVEL)")
                .description("Расчёт через уровень медицинского покрытия. medicalRiskLimitLevel обязателен.")
                .value("""
                        {
                          "personFirstName": "Ivan",
                          "personLastName": "Petrov",
                          "personBirthDate": "1990-05-15",
                          "agreementDateFrom": "2025-06-01",
                          "agreementDateTo": "2025-06-15",
                          "countryIsoCode": "ES",
                          "medicalRiskLimitLevel": "LEVEL_30000",
                          "useCountryDefaultPremium": false,
                          "selectedRisks": ["SPORT_ACTIVITIES"],
                          "currency": "EUR"
                        }
                        """);
    }

    private Example buildCountryDefaultExample() {
        return new Example()
                .summary("[V2] COUNTRY_DEFAULT режим")
                .description("""
                        Расчёт через дефолтную дневную ставку страны.
                        При useCountryDefaultPremium=true поле medicalRiskLimitLevel не требуется.
                        """)
                .value("""
                        {
                          "personFirstName": "Maria",
                          "personLastName": "Ivanova",
                          "personBirthDate": "1985-11-20",
                          "agreementDateFrom": "2025-07-10",
                          "agreementDateTo": "2025-07-20",
                          "countryIsoCode": "DE",
                          "useCountryDefaultPremium": true,
                          "currency": "EUR"
                        }
                        """);
    }

    private Example buildElderlyExample() {
        return new Example()
                .summary("[V2] Пожилой застрахованный (REQUIRES_REVIEW)")
                .description("Возраст 77 лет → AgeCoefficient 2.5 (Very elderly). Ожидаемый результат: REQUIRES_REVIEW.")
                .value("""
                        {
                          "personFirstName": "Nikolai",
                          "personLastName": "Smirnov",
                          "personBirthDate": "1948-03-10",
                          "agreementDateFrom": "2025-09-01",
                          "agreementDateTo": "2025-09-10",
                          "countryIsoCode": "IT",
                          "medicalRiskLimitLevel": "LEVEL_50000",
                          "currency": "EUR"
                        }
                        """);
    }

    private Example buildWithDiscountsExample() {
        return new Example()
                .summary("[V2] С промо-кодом и групповой скидкой")
                .description("Промо-код SUMMER2025 + групповая скидка (10 человек).")
                .value("""
                        {
                          "personFirstName": "Olga",
                          "personLastName": "Kozlova",
                          "personBirthDate": "2000-08-30",
                          "agreementDateFrom": "2025-08-01",
                          "agreementDateTo": "2025-08-14",
                          "countryIsoCode": "FR",
                          "medicalRiskLimitLevel": "LEVEL_100000",
                          "selectedRisks": ["LUGGAGE_LOSS", "FLIGHT_DELAY"],
                          "currency": "EUR",
                          "promoCode": "SUMMER2025",
                          "personsCount": 10,
                          "isCorporate": false
                        }
                        """);
    }

    // ── V3 примеры ────────────────────────────────────────────────────────────

    private Example buildV3SinglePersonExample() {
        return new Example()
                .summary("[V3] Одна персона — базовый запрос")
                .description("""
                        Минимальный V3 запрос с одной персоной.
                        Аналогичен V2 запросу, но использует поле persons[].
                        """)
                .value("""
                        {
                          "persons": [
                            {
                              "personFirstName": "Ivan",
                              "personLastName": "Petrov",
                              "personBirthDate": "1990-05-15"
                            }
                          ],
                          "agreementDateFrom": "2025-06-01",
                          "agreementDateTo": "2025-06-15",
                          "countryIsoCode": "ES",
                          "medicalRiskLimitLevel": "50000",
                          "currency": "EUR"
                        }
                        """);
    }

    private Example buildV3MultiPersonExample() {
        return new Example()
                .summary("[V3] Несколько персон — семейный полис")
                .description("""
                        V3 запрос для трёх персон разного возраста.
                        Каждая персона получает индивидуальный расчёт с учётом возраста.
                        Итоговая премия = сумма индивидуальных премий − скидки.
                        
                        Ошибки валидации адресуются с индексом:
                          persons[0].personBirthDate — Must not be empty
                        """)
                .value("""
                        {
                          "persons": [
                            {
                              "personFirstName": "Ivan",
                              "personLastName": "Petrov",
                              "personBirthDate": "1985-05-15",
                              "applyAgeCoefficient": null
                            },
                            {
                              "personFirstName": "Anna",
                              "personLastName": "Petrova",
                              "personBirthDate": "1988-11-20",
                              "applyAgeCoefficient": null
                            },
                            {
                              "personFirstName": "Alex",
                              "personLastName": "Petrov",
                              "personBirthDate": "2015-03-10",
                              "applyAgeCoefficient": null
                            }
                          ],
                          "agreementDateFrom": "2025-07-01",
                          "agreementDateTo": "2025-07-14",
                          "countryIsoCode": "ES",
                          "medicalRiskLimitLevel": "50000",
                          "selectedRisks": ["SPORT_ACTIVITIES"],
                          "currency": "EUR",
                          "personsCount": 3
                        }
                        """);
    }

    private Example buildV3WithDiscountsExample() {
        return new Example()
                .summary("[V3] Корпоративный клиент — 5 сотрудников")
                .description("""
                        V3 запрос для корпоративного клиента с 5 застрахованными.
                        Применяется корпоративная скидка 20%.
                        
                        Каждая персона может иметь отдельное управление возрастным коэффициентом
                        через поле applyAgeCoefficient.
                        """)
                .value("""
                        {
                          "persons": [
                            {
                              "personFirstName": "Alexei",
                              "personLastName": "Ivanov",
                              "personBirthDate": "1982-03-15"
                            },
                            {
                              "personFirstName": "Elena",
                              "personLastName": "Smirnova",
                              "personBirthDate": "1990-07-22"
                            },
                            {
                              "personFirstName": "Dmitry",
                              "personLastName": "Volkov",
                              "personBirthDate": "1975-12-01",
                              "applyAgeCoefficient": false
                            },
                            {
                              "personFirstName": "Olga",
                              "personLastName": "Kozlova",
                              "personBirthDate": "1995-04-18"
                            },
                            {
                              "personFirstName": "Sergei",
                              "personLastName": "Nikitin",
                              "personBirthDate": "1988-09-10"
                            }
                          ],
                          "agreementDateFrom": "2025-09-01",
                          "agreementDateTo": "2025-09-10",
                          "countryIsoCode": "DE",
                          "medicalRiskLimitLevel": "100000",
                          "currency": "EUR",
                          "isCorporate": true,
                          "personsCount": 5
                        }
                        """);
    }
}