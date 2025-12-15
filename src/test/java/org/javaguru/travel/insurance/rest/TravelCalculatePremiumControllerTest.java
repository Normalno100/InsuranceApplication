package org.javaguru.travel.insurance.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Упрощённые controller тесты - фокус на контракте, а не на деталях
 */
@WebMvcTest(TravelCalculatePremiumController.class)
@DisplayName("Travel Insurance Controller - Simplified Tests")
class TravelCalculatePremiumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumService service;

    // ========== SUCCESSFUL SCENARIOS ==========

    @Test
    @DisplayName("Should return 200 with calculated premium for valid request")
    void shouldReturn200WithCalculatedPremium() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = successResponse();
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertEquals("John", actual.getPersonFirstName());
        assertEquals("Smith", actual.getPersonLastName());
        assertEquals(new BigDecimal("10"), actual.getAgreementPrice());
        assertFalse(actual.hasErrors());
    }

    @Test
    @DisplayName("Should calculate zero premium for same-day trip")
    void shouldCalculateZeroPremiumForSameDayTrip() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = successResponse();
        mockResponse.setAgreementPrice(BigDecimal.ZERO);
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sameDateRequestJson()))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertEquals(0, actual.getAgreementPrice().compareTo(BigDecimal.ZERO));
    }

    // ========== VALIDATION ERRORS ==========

    @Test
    @DisplayName("Should return 400 when firstName is empty")
    void shouldReturn400WhenFirstNameIsEmpty() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = errorResponse(
                "personFirstName", "Must not be empty!");
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyFirstNameJson()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertTrue(actual.hasErrors());
        assertEquals(1, actual.getErrors().size());
        assertEquals("personFirstName", actual.getErrors().get(0).getField());
    }

    @Test
    @DisplayName("Should return 400 when lastName is empty")
    void shouldReturn400WhenLastNameIsEmpty() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = errorResponse(
                "personLastName", "Must not be empty!");
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(emptyLastNameJson()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertTrue(actual.hasErrors());
        assertEquals("personLastName", actual.getErrors().get(0).getField());
    }

    @Test
    @DisplayName("Should return 400 when dateFrom is null")
    void shouldReturn400WhenDateFromIsNull() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = errorResponse(
                "agreementDateFrom", "Must not be empty!");
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(nullDateFromJson()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertTrue(actual.hasErrors());
        assertEquals("agreementDateFrom", actual.getErrors().get(0).getField());
    }

    @Test
    @DisplayName("Should return 400 when dateTo is before dateFrom")
    void shouldReturn400WhenDateToIsBeforeDateFrom() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = errorResponse(
                "agreementDateTo", "Must be after agreementDateFrom!");
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidDateOrderJson()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertTrue(actual.hasErrors());
        assertEquals("agreementDateTo", actual.getErrors().get(0).getField());
    }

    @Test
    @DisplayName("Should return 400 with multiple errors for completely invalid request")
    void shouldReturn400WithMultipleErrorsForInvalidRequest() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = multipleErrorsResponse();
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(allInvalidJson()))
                .andExpect(status().isBadRequest())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertTrue(actual.hasErrors());
        assertEquals(4, actual.getErrors().size());
    }

    // ========== SPECIAL CHARACTERS ==========

    @Test
    @DisplayName("Should preserve special characters in names")
    void shouldPreserveSpecialCharactersInNames() throws Exception {
        // Given
        TravelCalculatePremiumResponse mockResponse = successResponse();
        mockResponse.setPersonFirstName("Jean-Pierre");
        mockResponse.setPersonLastName("O'Connor");
        when(service.calculatePremium(any())).thenReturn(mockResponse);

        // When
        MvcResult result = mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(specialCharsJson()))
                .andExpect(status().isOk())
                .andReturn();

        // Then
        TravelCalculatePremiumResponse actual = parseResponse(result);
        assertEquals("Jean-Pierre", actual.getPersonFirstName());
        assertEquals("O'Connor", actual.getPersonLastName());
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumResponse successResponse() {
        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
        response.setPersonFirstName("John");
        response.setPersonLastName("Smith");
        response.setAgreementDateFrom(LocalDate.of(2023, 1, 1));
        response.setAgreementDateTo(LocalDate.of(2023, 1, 11));
        response.setAgreementPrice(new BigDecimal("10"));
        return response;
    }

    private TravelCalculatePremiumResponse errorResponse(String field, String message) {
        return new TravelCalculatePremiumResponse(
                List.of(new org.javaguru.travel.insurance.dto.ValidationError(field, message))
        );
    }

    private TravelCalculatePremiumResponse multipleErrorsResponse() {
        return new TravelCalculatePremiumResponse(
                List.of(
                        new org.javaguru.travel.insurance.dto.ValidationError("personFirstName", "Must not be empty!"),
                        new org.javaguru.travel.insurance.dto.ValidationError("personLastName", "Must not be empty!"),
                        new org.javaguru.travel.insurance.dto.ValidationError("agreementDateFrom", "Must not be empty!"),
                        new org.javaguru.travel.insurance.dto.ValidationError("agreementDateTo", "Must not be empty!")
                )
        );
    }

    private TravelCalculatePremiumResponse parseResponse(MvcResult result) throws Exception {
        String json = result.getResponse().getContentAsString();
        return objectMapper.readValue(json, TravelCalculatePremiumResponse.class);
    }

    private String validRequestJson() {
        return """
                {
                    "personFirstName": "John",
                    "personLastName": "Smith",
                    "agreementDateFrom": "2023-01-01",
                    "agreementDateTo": "2023-01-11"
                }
                """;
    }

    private String sameDateRequestJson() {
        return """
                {
                    "personFirstName": "John",
                    "personLastName": "Smith",
                    "agreementDateFrom": "2023-01-01",
                    "agreementDateTo": "2023-01-01"
                }
                """;
    }

    private String emptyFirstNameJson() {
        return """
                {
                    "personFirstName": "",
                    "personLastName": "Smith",
                    "agreementDateFrom": "2023-01-01",
                    "agreementDateTo": "2023-01-11"
                }
                """;
    }

    private String emptyLastNameJson() {
        return """
                {
                    "personFirstName": "John",
                    "personLastName": "",
                    "agreementDateFrom": "2023-01-01",
                    "agreementDateTo": "2023-01-11"
                }
                """;
    }

    private String nullDateFromJson() {
        return """
                {
                    "personFirstName": "John",
                    "personLastName": "Smith",
                    "agreementDateFrom": null,
                    "agreementDateTo": "2023-01-11"
                }
                """;
    }

    private String invalidDateOrderJson() {
        return """
                {
                    "personFirstName": "John",
                    "personLastName": "Smith",
                    "agreementDateFrom": "2023-01-11",
                    "agreementDateTo": "2023-01-01"
                }
                """;
    }

    private String allInvalidJson() {
        return """
                {
                    "personFirstName": null,
                    "personLastName": null,
                    "agreementDateFrom": null,
                    "agreementDateTo": null
                }
                """;
    }

    private String specialCharsJson() {
        return """
                {
                    "personFirstName": "Jean-Pierre",
                    "personLastName": "O'Connor",
                    "agreementDateFrom": "2023-01-01",
                    "agreementDateTo": "2023-01-11"
                }
                """;
    }
}