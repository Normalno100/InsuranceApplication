package org.javaguru.travel.insurance.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E Integration Tests
 * Тестируют весь стек: REST API → Service → Repository → H2 Database
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = {
        "/test-data/countries.sql",
        "/test-data/medical-risk-limit-levels.sql",
        "/test-data/risk-types.sql"
})
@DisplayName("Travel Insurance E2E Tests")
class IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // ========================================
    // HAPPY PATH TESTS
    // ========================================

    @Test
    @DisplayName("Should calculate premium for simple trip")
    void shouldCalculateSimpleTrip() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1)) // ✅ Будущая дата
                .agreementDateTo(LocalDate.now().plusDays(15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.agreementPrice").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.personFirstName").value("John"))
                .andExpect(jsonPath("$.personLastName").value("Doe"))
                .andExpect(jsonPath("$.countryName").value("Spain"))
                .andExpect(jsonPath("$.underwritingDecision").value("APPROVED")) // ✅ Проверяем андеррайтинг
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should calculate premium with additional risks")
    void shouldCalculateWithAdditionalRisks() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("SPORT_ACTIVITIES"))
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.selectedRisks[0]").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.riskPremiums").isArray())
                .andExpect(jsonPath("$.underwritingDecision").value("APPROVED"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should apply promo code discount")
    void shouldApplyPromoCodeDiscount() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2026, 6, 1)) // ✅ Дата в пределах действия FAMILY20 (2025-01-01 до 2025-12-31)
                .agreementDateTo(LocalDate.of(2026, 7, 15)) // ✅ 44 дня - достаточно для минимальной суммы
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000") // ✅ Высокая ставка для достижения минимума
                .promoCode("FAMILY20") // ✅ Промо-код требует минимум 150 EUR
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedDiscounts").isArray())
                .andExpect(jsonPath("$.appliedDiscounts[0].amount").value(21.78))
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.discountAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwritingDecision").value("APPROVED"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    @DisplayName("Should apply group discount")
    void shouldApplyGroupDiscount() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .personsCount(10)
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appliedDiscounts").isArray())
                .andExpect(jsonPath("$.discountAmount").isNumber())
                .andExpect(jsonPath("$.discountAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwritingDecision").value("APPROVED"))
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // ========================================
    // VALIDATION TESTS
    // ========================================

    @Test
    @DisplayName("Should reject invalid request with clear errors")
    void shouldRejectInvalidRequest() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("") // ✅ Пустое имя
                .personLastName("")  // ✅ Пустая фамилия
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(1)) // ✅ Критичная ошибка останавливает валидацию
                .andExpect(jsonPath("$.agreementPrice").doesNotExist());
    }

    @Test
    @DisplayName("Should reject when date_to before date_from")
    void shouldRejectInvalidDateOrder() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 15))
                .agreementDateTo(LocalDate.of(2025, 6, 1))  // ✅ Раньше dateFrom
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*greater.*/i)]").exists()); // ✅ Новое сообщение
    }

    @Test
    @DisplayName("Should reject unknown country")
    void shouldRejectUnknownCountry() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ZZ") // ✅ Несуществующая страна
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'countryIsoCode')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*not found.*/i)]").exists());
    }

    @Test
    @DisplayName("Should reject when age exceeds 80")
    void shouldRejectAgeTooOld() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1940, 1, 1)) // ✅ Возраст 85
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'personBirthDate')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*80.*/)]").exists());
    }

    @Test
    @DisplayName("Should reject trip longer than 365 days")
    void shouldRejectTooLongTrip() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(400)) // ✅ Слишком долго
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[?(@.field == 'agreementDateTo')]").exists())
                .andExpect(jsonPath("$.errors[?(@.message =~ /.*365.*/)]").exists());
    }

    @Test
    @DisplayName("Should calculate premium even for past trip dates")
    void shouldCalculatePremiumForPastTrip() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2026, 10, 7)) // ✅ Дата из лога (в прошлом на момент теста)
                .agreementDateTo(LocalDate.of(2026, 10, 22))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()) // ✅ Прошлые даты не блокируют расчёт
                .andExpect(jsonPath("$.agreementPrice").exists())
                .andExpect(jsonPath("$.agreementPrice").isNumber())
                .andExpect(jsonPath("$.underwritingDecision").value("APPROVED"))
                .andExpect(jsonPath("$.errors").doesNotExist()); // ✅ Нет ошибок
    }

    // ========================================
    // UNDERWRITING TESTS
    // ========================================

    @Test
    @DisplayName("Should decline extreme sport for age 75+")
    void shouldDeclineExtremeSportForOld() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1950, 1, 1)) // ✅ Возраст 80
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("EXTREME_SPORT")) // ✅ Экстремальный спорт
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.underwritingDecision").value("DECLINED"))
                .andExpect(jsonPath("$.declineReason").exists())
                .andExpect(jsonPath("$.errors[0].field").value("underwriting"));
    }

    @Test
    @DisplayName("Should require manual review for high coverage + old age")
    void shouldRequireReviewForHighCoverageOldAge() throws Exception {
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1950, 1, 1)) // ✅ Возраст 75
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("200000") // ✅ Высокое покрытие
                .build();

        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.underwritingDecision").value("REQUIRES_MANUAL_REVIEW"))
                .andExpect(jsonPath("$.reviewReason").exists());
    }

    // ========================================
    // ERROR HANDLING TESTS
    // ========================================

    @Test
    @DisplayName("Should handle malformed JSON")
    void shouldHandleMalformedJson() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("malformed")));
    }

    @Test
    @DisplayName("Should handle wrong content type")
    void shouldHandleWrongContentType() throws Exception {
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml>data</xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("unsupported")));
    }

    @Test
    @DisplayName("Should handle wrong HTTP method")
    void shouldHandleWrongMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/v2/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value(containsStringIgnoringCase("method not allowed")));
    }

    // ========================================
    // HEALTH CHECK TEST
    // ========================================

    @Test
    @DisplayName("Health check returns 200")
    void shouldReturnHealthCheck() throws Exception {
        mockMvc.perform(get("/insurance/travel/health"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("running")));
    }
}