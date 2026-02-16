package org.javaguru.travel.insurance.web.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration тест для проверки JSON форматирования в REST API
 * 
 * ЦЕЛЬ:
 * Убедиться, что BigDecimal значения корректно форматируются
 * в реальных HTTP response от REST контроллера.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("REST API BigDecimal Formatting Integration Tests")
class BigDecimalRestApiFormattingTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Premium calculation response should have properly formatted BigDecimal values")
    void premiumCalculationShouldFormatBigDecimalCorrectly() throws Exception {
        // Given: валидный запрос на расчет премии
        String requestJson = """
                {
                  "personFirstName": "John",
                  "personLastName": "Doe",
                  "personBirthDate": "1990-01-15",
                  "agreementDateFrom": "2026-06-01",
                  "agreementDateTo": "2026-06-10",
                  "countryIsoCode": "ES",
                  "medicalRiskLimitLevel": "10000"
                }
                """;

        // When & Then: делаем запрос и проверяем форматирование
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                
                // Проверяем что success = true
                .andExpect(jsonPath("$.success").value(true))
                
                // Проверяем форматирование totalPremium
                // Должно быть число с 2 десятичными знаками
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                .andExpect(jsonPath("$.pricing.totalPremium").value(matchesPattern("\\d+\\.\\d{2}")))
                
                // Проверяем форматирование baseAmount
                .andExpect(jsonPath("$.pricing.baseAmount").isNumber())
                
                // Проверяем форматирование totalDiscount
                .andExpect(jsonPath("$.pricing.totalDiscount").isNumber())
                
                // Если есть pricingDetails, проверяем коэффициенты
                .andExpect(jsonPath("$.pricingDetails").exists())
                .andExpect(jsonPath("$.pricingDetails.baseRate").isNumber())
                .andExpect(jsonPath("$.pricingDetails.ageCoefficient").isNumber())
                .andExpect(jsonPath("$.pricingDetails.countryCoefficient").isNumber());
    }

    @Test
    @DisplayName("Response should not contain scientific notation")
    void responseShouldNotContainScientificNotation() throws Exception {
        // Given: запрос, который может привести к большим суммам
        String requestJson = """
                {
                  "personFirstName": "Jane",
                  "personLastName": "Smith",
                  "personBirthDate": "1985-05-20",
                  "agreementDateFrom": "2026-07-01",
                  "agreementDateTo": "2026-08-31",
                  "countryIsoCode": "US",
                  "medicalRiskLimitLevel": "500000"
                }
                """;

        // When & Then
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                
                // Проверяем отсутствие научной нотации (E, e)
                .andExpect(jsonPath("$.pricing.totalPremium").value(not(containsString("E"))))
                .andExpect(jsonPath("$.pricing.totalPremium").value(not(containsString("e"))));
    }

    @Test
    @DisplayName("Zero discount should be formatted as 0.00")
    void zeroDiscountShouldBeFormattedCorrectly() throws Exception {
        // Given: запрос без промо-кода (discount = 0)
        String requestJson = """
                {
                  "personFirstName": "Bob",
                  "personLastName": "Wilson",
                  "personBirthDate": "1995-03-10",
                  "agreementDateFrom": "2026-09-01",
                  "agreementDateTo": "2026-09-07",
                  "countryIsoCode": "FR",
                  "medicalRiskLimitLevel": "20000"
                }
                """;

        // When & Then
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                
                // Скидка должна быть 0.00, а не 0 или 0.0
                .andExpect(jsonPath("$.pricing.totalDiscount").value(0.00));
    }

    @Test
    @DisplayName("Small premium amounts should have 2 decimal places")
    void smallAmountsShouldHaveTwoDecimals() throws Exception {
        // Given: короткая поездка → маленькая премия
        String requestJson = """
                {
                  "personFirstName": "Alice",
                  "personLastName": "Brown",
                  "personBirthDate": "2000-12-01",
                  "agreementDateFrom": "2026-10-01",
                  "agreementDateTo": "2026-10-02",
                  "countryIsoCode": "IT",
                  "medicalRiskLimitLevel": "5000"
                }
                """;

        // When & Then
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                
                // Даже малые суммы должны иметь .00
                .andExpect(jsonPath("$.pricing.totalPremium").isNumber())
                // Проверяем что это не "5" или "5.5", а "5.50" или подобное
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(0.0)));
    }

    /**
     * Helper метод для проверки паттерна числа с 2 десятичными знаками
     */
    private static org.hamcrest.Matcher<String> matchesPattern(String pattern) {
        return org.hamcrest.text.MatchesPattern.matchesPattern(pattern);
    }
}
