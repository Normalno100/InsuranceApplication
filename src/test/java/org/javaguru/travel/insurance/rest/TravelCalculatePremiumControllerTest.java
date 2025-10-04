package org.javaguru.travel.insurance.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TravelCalculatePremiumController.class)
class TravelCalculatePremiumControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TravelCalculatePremiumService calculatePremiumService;

    @Test
    void shouldReturnSuccessfulResponse() throws Exception {
        TravelCalculatePremiumRequest request = createValidRequest();
        TravelCalculatePremiumResponse response = createSuccessfulResponse(request);

        when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.personFirstName").value("John"))
                .andExpect(jsonPath("$.personLastName").value("Smith"))
                .andExpect(jsonPath("$.agreementPrice").value(10));
    }

    @Test
    void shouldReturnValidationErrors() throws Exception {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("");
        request.setPersonLastName("");

        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse(
                List.of(
                        new ValidationError("personFirstName", "Must not be empty!"),
                        new ValidationError("personLastName", "Must not be empty!")
                )
        );

        when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("personFirstName"))
                .andExpect(jsonPath("$.errors[0].message").value("Must not be empty!"))
                .andExpect(jsonPath("$.errors[1].field").value("personLastName"))
                .andExpect(jsonPath("$.errors[1].message").value("Must not be empty!"));
    }

    @Test
    void shouldHandleEmptyRequestBody() throws Exception {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();

        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse(
                List.of(
                        new ValidationError("personFirstName", "Must not be empty!"),
                        new ValidationError("personLastName", "Must not be empty!"),
                        new ValidationError("agreementDateFrom", "Must not be empty!"),
                        new ValidationError("agreementDateTo", "Must not be empty!")
                )
        );

        when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(4));
    }

    @Test
    void shouldHandleDateValidation() throws Exception {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Smith");
        request.setAgreementDateFrom(new Date(System.currentTimeMillis() + 100000));
        request.setAgreementDateTo(new Date());

        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse(
                List.of(new ValidationError("agreementDateTo", "Must be after agreementDateFrom!"))
        );

        when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errors[0].field").value("agreementDateTo"))
                .andExpect(jsonPath("$.errors[0].message").value("Must be after agreementDateFrom!"));
    }

    @Test
    void shouldAcceptValidRequestWithAllFields() throws Exception {
        TravelCalculatePremiumRequest request = createValidRequest();
        TravelCalculatePremiumResponse response = createSuccessfulResponse(request);

        when(calculatePremiumService.calculatePremium(any(TravelCalculatePremiumRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/insurance/travel/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personFirstName").exists())
                .andExpect(jsonPath("$.personLastName").exists())
                .andExpect(jsonPath("$.agreementDateFrom").exists())
                .andExpect(jsonPath("$.agreementDateTo").exists())
                .andExpect(jsonPath("$.agreementPrice").exists());
    }

    private TravelCalculatePremiumRequest createValidRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Smith");
        request.setAgreementDateFrom(new Date());
        request.setAgreementDateTo(new Date(System.currentTimeMillis() + 864000000L)); // +10 days
        return request;
    }

    private TravelCalculatePremiumResponse createSuccessfulResponse(TravelCalculatePremiumRequest request) {
        TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
        response.setPersonFirstName(request.getPersonFirstName());
        response.setPersonLastName(request.getPersonLastName());
        response.setAgreementDateFrom(request.getAgreementDateFrom());
        response.setAgreementDateTo(request.getAgreementDateTo());
        response.setAgreementPrice(new BigDecimal("10"));
        return response;
    }
}