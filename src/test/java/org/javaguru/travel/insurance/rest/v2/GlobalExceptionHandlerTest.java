package org.javaguru.travel.insurance.rest.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Тесты для GlobalExceptionHandler
 *
 * Проверяет обработку различных типов исключений:
 * 1. Malformed JSON (400)
 * 2. Unsupported Media Type (415)
 * 3. Method Not Allowed (405)
 * 4. Resource Not Found (404)
 * 5. Generic Exception (500)
 */
@WebMvcTest(TravelCalculatePremiumControllerV2.class)
@ContextConfiguration(classes = {
        TravelCalculatePremiumControllerV2.class,
        GlobalExceptionHandler.class
})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumServiceV2 calculatePremiumService;

    // ========== 1. MALFORMED JSON → 400 ==========

    @Test
    void shouldReturn400_whenJsonIsMalformed() throws Exception {
        // Given: невалидный JSON
        String malformedJson = "{\"personFirstName\": \"John\", invalid}";

        // When & Then
        MvcResult result = mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists())
                .andReturn();

        // Дополнительная проверка структуры ответа
        String responseBody = result.getResponse().getContentAsString();
        assertThat(responseBody).contains("Malformed JSON request");
    }

    @Test
    void shouldReturn400_whenJsonHasInvalidSyntax() throws Exception {
        // Given: JSON с синтаксической ошибкой
        String invalidJson = "{personFirstName: John}"; // отсутствуют кавычки

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    void shouldReturn400_whenJsonHasUnexpectedToken() throws Exception {
        // Given: JSON с неожиданным токеном
        String invalidJson = "{\"personFirstName\": \"John\",, \"personLastName\": \"Doe\"}";

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    void shouldReturn400_whenJsonHasInvalidDateFormat() throws Exception {
        // Given: JSON с невалидным форматом даты
        String invalidJson = "{\n" +
                "  \"personFirstName\": \"John\",\n" +
                "  \"personLastName\": \"Doe\",\n" +
                "  \"personBirthDate\": \"invalid-date\",\n" +
                "  \"agreementDateFrom\": \"2025-01-01\",\n" +
                "  \"agreementDateTo\": \"2025-01-10\",\n" +
                "  \"countryIsoCode\": \"ES\",\n" +
                "  \"medicalRiskLimitLevel\": \"10000\"\n" +
                "}";

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    // ========== 2. UNSUPPORTED MEDIA TYPE → 415 ==========

    @Test
    void shouldReturn415_whenContentTypeIsXml() throws Exception {
        // Given: попытка отправить XML вместо JSON
        String xmlContent = "<?xml version=\"1.0\"?><request><name>John</name></request>";

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content(xmlContent))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn415_whenContentTypeIsPlainText() throws Exception {
        // Given: попытка отправить plain text
        String textContent = "Some plain text";

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(textContent))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void shouldReturn415_whenContentTypeIsFormUrlEncoded() throws Exception {
        // Given: попытка отправить form data
        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "John"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    void shouldReturn415_whenNoContentType() throws Exception {
        // Given: запрос без Content-Type
        String validJson = createValidRequestJson();

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .content(validJson))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    // ========== 3. METHOD NOT ALLOWED → 405 ==========

    @Test
    void shouldReturn405_whenUsingGetInsteadOfPost() throws Exception {
        // Given: использование GET вместо POST
        // When & Then
        mockMvc.perform(get("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn405_whenUsingPutInsteadOfPost() throws Exception {
        // Given: использование PUT вместо POST
        String validJson = createValidRequestJson();

        // When & Then
        mockMvc.perform(put("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void shouldReturn405_whenUsingDeleteInsteadOfPost() throws Exception {
        // Given: использование DELETE вместо POST
        // When & Then
        mockMvc.perform(delete("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    void shouldReturn405_whenUsingPatchInsteadOfPost() throws Exception {
        // Given: использование PATCH вместо POST
        String validJson = createValidRequestJson();

        // When & Then
        mockMvc.perform(patch("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    // ========== 4. RESOURCE NOT FOUND → 404 ==========

    @Test
    void shouldReturn404_whenEndpointDoesNotExist() throws Exception {
        // Given: несуществующий endpoint
        // When & Then
        mockMvc.perform(get("/insurance/travel/v2/non-existent-endpoint")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn404_whenPathIsIncorrect() throws Exception {
        // Given: неправильный путь
        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/wrong-path")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void shouldReturn404_whenVersionIsWrong() throws Exception {
        // Given: неправильная версия API
        // When & Then
        mockMvc.perform(post("/insurance/travel/v99/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createValidRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ========== 5. GENERIC EXCEPTION → 500 ==========

    @Test
    void shouldReturn500_whenUnexpectedExceptionOccurs() throws Exception {
        // Given: сервис выбрасывает RuntimeException
        String validJson = createValidRequestJson();

        // Мокируем RuntimeException
        org.mockito.Mockito.when(calculatePremiumService.calculatePremium(
                        org.mockito.ArgumentMatchers.any(TravelCalculatePremiumRequestV2.class)))
                .thenThrow(new RuntimeException("Unexpected database error"));

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void shouldReturn500_whenNullPointerExceptionOccurs() throws Exception {
        // Given: NullPointerException
        String validJson = createValidRequestJson();

        org.mockito.Mockito.when(calculatePremiumService.calculatePremium(
                        org.mockito.ArgumentMatchers.any(TravelCalculatePremiumRequestV2.class)))
                .thenThrow(new NullPointerException("Null value encountered"));

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"));
    }

    @Test
    void shouldReturn500_whenArithmeticExceptionOccurs() throws Exception {
        // Given: ArithmeticException
        String validJson = createValidRequestJson();

        org.mockito.Mockito.when(calculatePremiumService.calculatePremium(
                        org.mockito.ArgumentMatchers.any(TravelCalculatePremiumRequestV2.class)))
                .thenThrow(new ArithmeticException("Division by zero"));

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validJson))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Internal server error"))
                .andExpect(jsonPath("$.message").value("Division by zero"));
    }

    // ========== ПРОВЕРКА СТРУКТУРЫ ОТВЕТА ==========

    @Test
    void errorResponseShouldHaveCorrectStructure() throws Exception {
        // Given: malformed JSON для получения ErrorResponse
        String malformedJson = "{invalid}";

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then: проверяем структуру ErrorResponse
        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse =
                objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.error()).isNotNull();
        assertThat(errorResponse.message()).isNotNull();
        assertThat(errorResponse.timestamp()).isGreaterThan(0);
    }

    @Test
    void errorResponseShouldContainReasonableTimestamp() throws Exception {
        // Given
        String malformedJson = "{invalid}";
        long beforeRequest = System.currentTimeMillis();

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest())
                .andReturn();

        long afterRequest = System.currentTimeMillis();

        // Then: timestamp должен быть между началом и концом запроса
        String responseBody = result.getResponse().getContentAsString();
        ErrorResponse errorResponse =
                objectMapper.readValue(responseBody, ErrorResponse.class);

        assertThat(errorResponse.timestamp()).isBetween(beforeRequest, afterRequest);
    }

    // ========== МНОЖЕСТВЕННЫЕ ОШИБКИ ==========

    @Test
    void shouldHandleMultipleConsecutiveErrors() throws Exception {
        // Given: несколько последовательных ошибочных запросов
        String malformedJson = "{invalid}";

        // When & Then: делаем 3 последовательных запроса
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/insurance/travel/v2/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error").value("Malformed JSON request"));
        }
    }

    @Test
    void shouldHandleDifferentErrorTypesInSequence() throws Exception {
        // 1. Malformed JSON
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));

        // 2. Wrong method
        mockMvc.perform(get("/insurance/travel/v2/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));

        // 3. Wrong content type
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));

        // 4. Not found
        mockMvc.perform(get("/insurance/travel/v2/wrong-endpoint"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    // ========== EDGE CASES ==========

    @Test
    void shouldHandleEmptyRequestBody() throws Exception {
        // Given: пустое тело запроса
        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleVeryLongErrorMessage() throws Exception {
        // Given: очень длинное сообщение об ошибке
        StringBuilder longJson = new StringBuilder("{");
        for (int i = 0; i < 1000; i++) {
            longJson.append("invalid");
        }

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(longJson.toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    void shouldHandleSpecialCharactersInErrorMessage() throws Exception {
        // Given: JSON со специальными символами
        String jsonWithSpecialChars = "{\"name\": \"Test<>&\"}invalid";

        // When & Then
        mockMvc.perform(post("/insurance/travel/v2/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonWithSpecialChars))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Создает валидный JSON запрос для тестов
     */
    private String createValidRequestJson() {
        return "{\n" +
                "  \"personFirstName\": \"John\",\n" +
                "  \"personLastName\": \"Doe\",\n" +
                "  \"personBirthDate\": \"1990-01-01\",\n" +
                "  \"agreementDateFrom\": \"2025-06-01\",\n" +
                "  \"agreementDateTo\": \"2025-06-15\",\n" +
                "  \"countryIsoCode\": \"ES\",\n" +
                "  \"medicalRiskLimitLevel\": \"10000\"\n" +
                "}";
    }

    /**
     * Создает объект запроса для тестов
     */
    private TravelCalculatePremiumRequestV2 createValidRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();
    }
}