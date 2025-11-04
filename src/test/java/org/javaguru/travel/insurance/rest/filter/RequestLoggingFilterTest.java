package org.javaguru.travel.insurance.rest.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.rest.TravelCalculatePremiumController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.javaguru.travel.insurance.util.TestDataLoader.loadRequestAsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Тесты для RequestLoggingFilter
 * Проверяют, что логируется корректная информация в JSON формате
 */
@WebMvcTest(TravelCalculatePremiumController.class)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("RequestLoggingFilter Tests")
public class RequestLoggingFilterTest {

    @Autowired

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumService calculatePremiumService;

    @Nested
    @DisplayName("Successful Request Logging")
    class SuccessfulRequestLogging {

        @Test
        @DisplayName("Should log request with all fields in JSON format")
        void shouldLogRequestWithAllFieldsInJson(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("REST API Call");
            assertThat(logOutput).contains("\"method\" : \"POST\"");
            assertThat(logOutput).contains("\"uri\" : \"/insurance/travel/calculate\"");
            assertThat(logOutput).contains("\"duration_ms\"");
            assertThat(logOutput).contains("\"timestamp\"");

            // Проверяем что body запроса логируется
            assertThat(logOutput).contains("\"personFirstName\" : \"John\"");
            assertThat(logOutput).contains("\"personLastName\" : \"Smith\"");
            assertThat(logOutput).contains("\"agreementDateFrom\"");
            assertThat(logOutput).contains("\"agreementDateTo\"");
        }

        @Test
        @DisplayName("Should log response with status and body")
        void shouldLogResponseWithStatusAndBody(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("\"response\"");
            assertThat(logOutput).contains("\"status\" : 200");
            assertThat(logOutput).contains("\"agreementPrice\" : 10");
        }

        @Test
        @DisplayName("Should log duration in milliseconds")
        void shouldLogDurationInMilliseconds(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("\"duration_ms\"");
            // Проверяем что duration - число
            assertThat(logOutput).containsPattern("\"duration_ms\"\\s*:\\s*\\d+");
        }

        @Test
        @DisplayName("Should log timestamp")
        void shouldLogTimestamp(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("\"timestamp\"");
            // Формат ISO: 2023-01-01T12:00:00
            assertThat(logOutput).containsPattern("\"timestamp\"\\s*:\\s*\"\\d{4}-\\d{2}-\\d{2}T");
        }

        @Test
        @DisplayName("Should log headers")
        void shouldLogHeaders(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Custom-Header", "test-value")
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("\"headers\"");
            assertThat(logOutput).contains("Content-Type");
            assertThat(logOutput).contains("application/json");
        }

        @Test
        @DisplayName("Should use INFO level for successful requests (2xx)")
        void shouldUseInfoLevelForSuccessfulRequests(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            // Проверяем что используется INFO уровень
            assertThat(logOutput).contains("INFO");
            assertThat(logOutput).doesNotContain("WARN");
            assertThat(logOutput).doesNotContain("ERROR");
        }
    }

    @Nested
    @DisplayName("Error Request Logging")
    class ErrorRequestLogging {

        @Test
        @DisplayName("Should use WARN level for client errors (4xx)")
        void shouldUseWarnLevelForClientErrors(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("empty-first-name-request.json");
            TravelCalculatePremiumResponse mockResponse = createErrorResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("WARN");
            assertThat(logOutput).contains("\"status\" : 400");
            assertThat(logOutput).contains("REST API Call");
        }

        @Test
        @DisplayName("Should log validation errors in response body")
        void shouldLogValidationErrorsInResponseBody(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("empty-first-name-request.json");
            TravelCalculatePremiumResponse mockResponse = createErrorResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isBadRequest());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("\"errors\"");
            assertThat(logOutput).contains("\"field\" : \"personFirstName\"");
            assertThat(logOutput).contains("\"message\" : \"Must not be empty!\"");
        }
    }

    @Nested
    @DisplayName("Special Cases")
    class SpecialCases {

        @Test
        @DisplayName("Should NOT log health check endpoint")
        void shouldNotLogHealthCheckEndpoint(CapturedOutput output) throws Exception {
            // Act
            mockMvc.perform(get("/insurance/travel/health"))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            // Не должно быть логирования REST API Call для health check
            assertThat(logOutput).doesNotContain("REST API Call");
            assertThat(logOutput).doesNotContain("/health");
        }

        @Test
        @DisplayName("Should log request with special characters in names")
        void shouldLogRequestWithSpecialCharacters(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("special-chars-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("Jean-Pierre");
            assertThat(logOutput).contains("O'Connor");
        }

        @Test
        @DisplayName("Should log request with Cyrillic characters")
        void shouldLogRequestWithCyrillicCharacters(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("cyrillic-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            assertThat(logOutput).contains("Иван");
            assertThat(logOutput).contains("Петров");
        }

        @Test
        @DisplayName("Should redact sensitive headers")
        void shouldRedactSensitiveHeaders(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Authorization", "Bearer secret-token")
                            .header("Cookie", "session=secret")
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            // Чувствительные данные должны быть скрыты
            assertThat(logOutput).contains("***REDACTED***");
            assertThat(logOutput).doesNotContain("secret-token");
            assertThat(logOutput).doesNotContain("session=secret");
        }
    }

    @Nested
    @DisplayName("JSON Format Validation")
    class JsonFormatValidation {

        @Test
        @DisplayName("Should produce valid JSON output")
        void shouldProduceValidJsonOutput(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            // Извлекаем JSON из лога (между REST API Call: и следующим логом)
            String jsonPart = extractJsonFromLog(logOutput);

            // Проверяем что это валидный JSON
            assertThat(jsonPart).isNotEmpty();

            // Пытаемся распарсить - если не упадет, значит валидный JSON
            ObjectMapper mapper = new ObjectMapper();
            Object parsed = mapper.readValue(jsonPart, Object.class);
            assertThat(parsed).isNotNull();
        }

        @Test
        @DisplayName("Should have pretty-printed JSON format")
        void shouldHavePrettyPrintedJsonFormat(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            // Assert
            String logOutput = output.getOut();

            // Pretty-printed JSON должен содержать переносы строк и отступы
            assertThat(logOutput).contains("\n");
            assertThat(logOutput).containsPattern("\\{\\s+\"timestamp\"");
        }
    }

    @Nested
    @DisplayName("Performance")
    class Performance {

        @Test
        @DisplayName("Should complete logging in reasonable time")
        void shouldCompleteLoggingInReasonableTime(CapturedOutput output) throws Exception {
            // Arrange
            String requestJson = loadRequestAsString("valid-request.json");
            TravelCalculatePremiumResponse mockResponse = createSuccessfulResponse();
            when(calculatePremiumService.calculatePremium(any())).thenReturn(mockResponse);

            // Act
            long startTime = System.currentTimeMillis();

            mockMvc.perform(post("/insurance/travel/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isOk());

            long endTime = System.currentTimeMillis();
            long totalTime = endTime - startTime;

            // Assert
            // Логирование не должно добавлять значительную задержку (< 500ms)
            assertThat(totalTime).isLessThan(500);
            assertThat(output.getOut()).contains("REST API Call");
        }
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumResponse createSuccessfulResponse() {
        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
        response.setPersonFirstName("John");
        response.setPersonLastName("Smith");
        response.setAgreementDateFrom(LocalDate.of(2023, 1, 1));
        response.setAgreementDateTo(LocalDate.of(2023, 1, 11));
        response.setAgreementPrice(new BigDecimal("10"));
        return response;
    }

    private TravelCalculatePremiumResponse createErrorResponse() {
        return new TravelCalculatePremiumResponse(
                java.util.List.of(
                        new org.javaguru.travel.insurance.dto.ValidationError(
                                "personFirstName",
                                "Must not be empty!"
                        )
                )
        );
    }

    private String extractJsonFromLog(String logOutput) {
        // Извлекаем JSON между "REST API Call:" и концом JSON
        int startIdx = logOutput.indexOf("REST API Call:");
        if (startIdx == -1) return "";

        startIdx = logOutput.indexOf("{", startIdx);
        if (startIdx == -1) return "";

        int braceCount = 0;
        int endIdx = startIdx;

        for (int i = startIdx; i < logOutput.length(); i++) {
            char c = logOutput.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;

            if (braceCount == 0) {
                endIdx = i + 1;
                break;
            }
        }

        return logOutput.substring(startIdx, endIdx);
    }
}