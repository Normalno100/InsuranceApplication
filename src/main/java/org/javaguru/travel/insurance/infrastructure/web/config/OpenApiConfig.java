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
 * ДОБАВЛЕНО в task_111:
 * - Описание API с двумя режимами расчёта (MEDICAL_LEVEL / COUNTRY_DEFAULT)
 * - Примеры запросов для Swagger UI
 * - Документация поля personBirthDate и возрастных коэффициентов
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
                .version("2.1")
                .description("""
                        ## Travel Insurance Premium Calculation API
                        
                        Сервис расчёта страховых премий для туристических поездок.
                        
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
                        
                        ### Андеррайтинг
                        - Возраст 75–80 лет → `REQUIRES_REVIEW`
                        - Возраст > 80 лет → `VALIDATION_ERROR`
                        - EXTREME_SPORT для 60–70 лет → `REQUIRES_REVIEW`
                        - EXTREME_SPORT для 70+ лет → `DECLINED`
                        - Страны с рейтингом HIGH → `REQUIRES_REVIEW`
                        - Страны с рейтингом VERY_HIGH → `DECLINED`
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
                        .name("Premium Calculation")
                        .description("Расчёт страховой премии с андеррайтингом и применением скидок"),
                new Tag()
                        .name("Health")
                        .description("Проверка состояния сервиса")
        );
    }

    private Components buildComponents() {
        return new Components()
                .examples(Map.of(
                        "MedicalLevelRequest", buildMedicalLevelExample(),
                        "CountryDefaultRequest", buildCountryDefaultExample(),
                        "ElderlyRequest", buildElderlyExample(),
                        "WithDiscountsRequest", buildWithDiscountsExample()
                ));
    }

    // ──────────────────────────────────────────────────────────
    // Примеры запросов для Swagger UI
    // ──────────────────────────────────────────────────────────

    private Example buildMedicalLevelExample() {
        return new Example()
                .summary("Стандартный запрос (MEDICAL_LEVEL)")
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
                .summary("COUNTRY_DEFAULT режим (без medicalRiskLimitLevel)")
                .description("""
                        Расчёт через дефолтную дневную ставку страны.
                        При useCountryDefaultPremium=true поле medicalRiskLimitLevel не требуется.
                        AgeCoefficient для 39 лет: 1.1 (Adults: 31–40).
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
                .summary("Пожилой застрахованный (REQUIRES_REVIEW)")
                .description("""
                        Возраст 77 лет → AgeCoefficient 2.5 (Very elderly).
                        Ожидаемый результат: REQUIRES_REVIEW (age 77 >= reviewThreshold 75).
                        """)
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
                .summary("С промо-кодом и групповой скидкой")
                .description("""
                        Возраст 24 года → AgeCoefficient 1.0 (Young adults: 18–30).
                        Промо-код SUMMER2025 + групповая скидка (10 человек = −15%).
                        Применяется наибольшая скидка.
                        """)
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
}
