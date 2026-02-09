package org.javaguru.travel.insurance.core.validation;

import org.javaguru.travel.insurance.core.services.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.rest.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@Import(GlobalExceptionHandler.class)
@DisplayName("Error Handling Tests")
class ErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TravelCalculatePremiumService travelCalculatePremiumService;

    @Test
    @DisplayName("Should return 400 for malformed JSON")
    void shouldReturn400ForMalformedJson() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid json}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Malformed JSON request"));
    }

    @Test
    @DisplayName("Should return 415 for unsupported media type")
    void shouldReturn415ForUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml></xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error").value("Unsupported Media Type"));
    }

    @Test
    @DisplayName("Should return 405 for unsupported HTTP method")
    void shouldReturn405ForUnsupportedMethod() throws Exception {
        mockMvc.perform(get("/insurance/travel/calculate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.error").value("Method Not Allowed"));
    }

    @Test
    @DisplayName("Should return 404 for non-existent endpoint")
    void shouldReturn404ForNonExistentEndpoint() throws Exception {
        mockMvc.perform(post("/insurance/travel/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Should handle empty request body")
    void shouldHandleEmptyRequestBody() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should include timestamp in error response")
    void shouldIncludeTimestampInErrorResponse() throws Exception {
        mockMvc.perform(post("/insurance/travel/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").isNumber());
    }
}
