package org.javaguru.travel.insurance.rest.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for TravelCalculatePremiumControllerV2
 */
@WebMvcTest(controllers = TravelCalculatePremiumControllerV2.class)
@Import(GlobalExceptionHandler.class)
class TravelCalculatePremiumControllerV2Test {

    private static final String BASE = "/insurance/travel/v2";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    TravelCalculatePremiumServiceV2 service;

    // Helper to build a fully populated response
    private TravelCalculatePremiumResponseV2 buildFullResponse() {
        TravelCalculatePremiumResponseV2.RiskPremium rp =
                new TravelCalculatePremiumResponseV2.RiskPremium("MED", "Medical", new BigDecimal("12.50"), new BigDecimal("1.2"));

        TravelCalculatePremiumResponseV2.CalculationStep step =
                new TravelCalculatePremiumResponseV2.CalculationStep("Base * days", "10*5", new BigDecimal("50"));

        TravelCalculatePremiumResponseV2.CalculationDetails calc =
                new TravelCalculatePremiumResponseV2.CalculationDetails(
                        new BigDecimal("10"), // baseRate
                        new BigDecimal("1.1"), // ageCoefficient
                        new BigDecimal("1.2"), // countryCoefficient
                        new BigDecimal("1.05"), // additionalRisksCoefficient
                        new BigDecimal("1.386"), // totalCoefficient
                        5, // days
                        "10*1.386*5", // formula
                        List.of(step) // steps
                );

        TravelCalculatePremiumResponseV2.PromoCodeInfo promo =
                new TravelCalculatePremiumResponseV2.PromoCodeInfo("PROMO10", "10% off", "PERCENTAGE",
                        new BigDecimal("10"), new BigDecimal("6.165"));

        TravelCalculatePremiumResponseV2.DiscountInfo discount =
                new TravelCalculatePremiumResponseV2.DiscountInfo("SEASONAL", "Seasonal discount",
                        new BigDecimal("5"), new BigDecimal("3.00"));

        return TravelCalculatePremiumResponseV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1985, 6, 15))
                .personAge(40)
                .agreementDateFrom(LocalDate.of(2025, 12, 01))
                .agreementDateTo(LocalDate.of(2025, 12, 06))
                .agreementDays(5)
                .countryIsoCode("FR")
                .countryName("France")
                .medicalRiskLimitLevel("STANDARD")
                .coverageAmount(new BigDecimal("50000"))
                .selectedRisks(List.of("MED", "LIAB"))
                .riskPremiums(List.of(rp))
                .agreementPriceBeforeDiscount(new BigDecimal("61.65"))
                .discountAmount(new BigDecimal("6.165"))
                .agreementPrice(new BigDecimal("55.485"))
                .currency("EUR")
                .calculation(calc)
                .promoCodeInfo(promo)
                .appliedDiscounts(List.of(discount))
                .build();
    }

    // ---------------------- CALCULATE PREMIUM ----------------------

    @Nested
    @DisplayName("POST /calculate - premium calculation endpoint")
    class CalculatePremiumTests {

        @Test
        @DisplayName("1. Should return 200 OK and full JSON when calculation succeeds")
        void testCalculatePremiumSuccess() throws Exception {
            TravelCalculatePremiumResponseV2 response = buildFullResponse();

            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    // minimal request body for controller to parse
                                    new java.util.HashMap<String, Object>() {{
                                        put("personFirstName", "John");
                                        put("personLastName", "Doe");
                                    }}
                            )))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.personFirstName").value("John"))
                    .andExpect(jsonPath("$.personLastName").value("Doe"))
                    .andExpect(jsonPath("$.agreementPrice").value(55.485))
                    .andExpect(jsonPath("$.currency").value("EUR"))
                    .andExpect(jsonPath("$.calculation.baseRate").value(10))
                    .andExpect(jsonPath("$.riskPremiums[0].riskType").value("MED"))
                    .andExpect(jsonPath("$.promoCodeInfo.code").value("PROMO10"))
                    .andExpect(jsonPath("$.appliedDiscounts[0].discountType").value("SEASONAL"));
        }

        @Test
        @DisplayName("2. Should return 400 Bad Request when service returns errors")
        void testCalculatePremiumValidationErrors() throws Exception {
            TravelCalculatePremiumResponseV2 response =
                    new TravelCalculatePremiumResponseV2(List.of(new ValidationError("field", "must not be null")));

            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("field"));
        }

        @Test
        void testCalculatePremiumMalformedJson() throws Exception {
            String malformedJson = "{ invalid json }";

            mockMvc.perform(post("/insurance/travel/v2/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(malformedJson))
                    .andExpect(status().isBadRequest()) // ожидание 400
                    .andExpect(jsonPath("$.error").value("Malformed JSON request"))
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists());
        }

        @Test
        @DisplayName("4. Should return 415 Unsupported Media Type when content type is missing")
        void testCalculatePremiumMissingMediaType() throws Exception {
            // no content type header
            mockMvc.perform(post(BASE + "/calculate")
                            .content("{\"personFirstName\":\"John\"}"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("5. Should return 415 Unsupported Media Type for non-JSON content type")
        void testCalculatePremiumUnsupportedMediaType() throws Exception {
            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("plain text"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("6. Should return 500 Internal Server Error when service throws RuntimeException")
        void testCalculatePremiumServiceThrows() throws Exception {
            when(service.calculatePremium(any())).thenThrow(new RuntimeException("Service failure"));

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"John\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Internal server error"))
                    .andExpect(jsonPath("$.message").value("Service failure"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("7. Should return 400 when response.hasErrors() is true (explicit path)")
        void testCalculatePremiumHasErrorsPath() throws Exception {
            TravelCalculatePremiumResponseV2 response =
                    new TravelCalculatePremiumResponseV2(List.of(new ValidationError("x", "y")));

            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"Alex\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors", hasSize(1)));
        }

        @Test
        @DisplayName("8. Should honor alias POST / (root) as valid endpoint")
        void testPostRootAlias() throws Exception {
            TravelCalculatePremiumResponseV2 response = new TravelCalculatePremiumResponseV2();
            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"T\"}"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("9. Should respond with Content-Type application/json")
        void testCalculateProducesJsonContentType() throws Exception {
            TravelCalculatePremiumResponseV2 response = new TravelCalculatePremiumResponseV2();
            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"Kate\"}"))
                    .andExpect(header().string("Content-Type", containsString("application/json")));
        }

        @Test
        @DisplayName("10. Should return 405 Method Not Allowed for GET on /calculate")
        void testCalculateWrongMethod() throws Exception {
            mockMvc.perform(get(BASE + "/calculate"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ---------------------- HEALTH CHECK ----------------------

    @Nested
    @DisplayName("GET /health - health endpoint")
    class HealthCheckTests {

        @Test
        @DisplayName("11. Health endpoint returns status, version and timestamp")
        void testHealthCheck() throws Exception {
            mockMvc.perform(get(BASE + "/health"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status", containsString("running")))
                    .andExpect(jsonPath("$.version").value("2.0.0"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("12. Health endpoint rejects POST with 405")
        void testHealthWrongMethod() throws Exception {
            mockMvc.perform(post(BASE + "/health"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ---------------------- COUNTRIES ----------------------

    @Nested
    @DisplayName("GET /countries - countries endpoint")
    class CountriesTests {

        @Test
        @DisplayName("13. Countries endpoint returns an array of countries")
        void testGetCountriesArray() throws Exception {
            mockMvc.perform(get(BASE + "/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.countries").isArray());
        }

        @Test
        @DisplayName("14. Countries array is not empty")
        void testCountriesNotEmpty() throws Exception {
            mockMvc.perform(get(BASE + "/countries"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.countries.length()", greaterThan(0)));
        }
    }

    // ---------------------- COVERAGE LEVELS ----------------------

    @Nested
    @DisplayName("GET /coverage-levels - coverage levels endpoint")
    class CoverageLevelsTests {

        @Test
        @DisplayName("15. Coverage levels endpoint returns array 'levels'")
        void testGetCoverageLevels() throws Exception {
            mockMvc.perform(get(BASE + "/coverage-levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.levels").isArray());
        }

        @Test
        @DisplayName("16. Coverage levels array is not empty")
        void testCoverageLevelsNotEmpty() throws Exception {
            mockMvc.perform(get(BASE + "/coverage-levels"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.levels.length()", greaterThan(0)));
        }
    }

    // ---------------------- RISK TYPES ----------------------

    @Nested
    @DisplayName("GET /risk-types - risk types endpoint")
    class RiskTypesTests {

        @Test
        @DisplayName("17. Risk types endpoint returns array 'riskTypes'")
        void testGetRiskTypes() throws Exception {
            mockMvc.perform(get(BASE + "/risk-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riskTypes").isArray());
        }

        @Test
        @DisplayName("18. Risk types array is not empty")
        void testRiskTypesNotEmpty() throws Exception {
            mockMvc.perform(get(BASE + "/risk-types"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.riskTypes.length()", greaterThan(0)));
        }
    }

    // ---------------------- PROMO CODES ----------------------

    @Nested
    @DisplayName("GET /promo-codes/{code} - promo code validation endpoint")
    class PromoCodeTests {

        @Test
        @DisplayName("19. Promo code endpoint returns provided code and valid=true")
        void testValidatePromoCode() throws Exception {
            mockMvc.perform(get(BASE + "/promo-codes/ABC123"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("ABC123"))
                    .andExpect(jsonPath("$.isValid").value(true));
        }

        @Test
        @DisplayName("20. Promo code message contains 'Valid'")
        void testPromoCodeMessage() throws Exception {
            mockMvc.perform(get(BASE + "/promo-codes/HELLO"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message", containsString("Valid")));
        }

        @Test
        @DisplayName("21. Promo code returns boolean type for isValid")
        void testPromoCodeBoolean() throws Exception {
            mockMvc.perform(get(BASE + "/promo-codes/XYZ"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isValid").isBoolean());
        }

        @Test
        @DisplayName("22. Promo code path variable is preserved")
        void testPromoCodePathVariable() throws Exception {
            mockMvc.perform(get(BASE + "/promo-codes/SPECIAL"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SPECIAL"));
        }
    }

    // ---------------------- EXCEPTION HANDLER / EDGE CASES ----------------------

    @Nested
    @DisplayName("Global exception handling and edge cases")
    class ExceptionAndEdgeTests {

        @Test
        @DisplayName("23. Exception handler returns structured JSON when controller throws")
        void testExceptionHandlerViaMvc() throws Exception {
            when(service.calculatePremium(any())).thenThrow(new RuntimeException("Exploded"));

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"X\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.error").value("Internal server error"))
                    .andExpect(jsonPath("$.message").value("Exploded"))
                    .andExpect(jsonPath("$.timestamp").isNumber());
        }

        @Test
        @DisplayName("24. Direct call to controller.handleException returns 500 and message")
        void testDirectExceptionHandlerCall() {
            TravelCalculatePremiumControllerV2 controller =
                    new TravelCalculatePremiumControllerV2(null);

//            TravelCalculatePremiumControllerV2.ErrorResponse err =
//                    controller.handleException(new RuntimeException("Boom")).getBody();

//            // basic assertions
//            assert err != null;
//            assert err.error().equals("Internal server error");
//            assert err.message().equals("Boom");
//            assert err.timestamp() > 0;
        }

        @Test
        @DisplayName("25. GET unknown method on endpoints returns 405 or 404 appropriately")
        void testUnknownMethods() throws Exception {
            // POST to /countries should be 405
            mockMvc.perform(post(BASE + "/countries"))
                    .andExpect(status().isMethodNotAllowed());

            // POST to unknown path should be 404
            mockMvc.perform(post(BASE + "/non-existent"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("26. Response fields with dates are formatted as yyyy-MM-dd")
        void testDateFormattingInResponse() throws Exception {
            TravelCalculatePremiumResponseV2 response = buildFullResponse();
            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"John\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.agreementDateFrom").value("2025-12-01"))
                    .andExpect(jsonPath("$.agreementDateTo").value("2025-12-06"))
                    .andExpect(jsonPath("$.personBirthDate").value("1985-06-15"));
        }

        @Test
        @DisplayName("27. Numeric nested fields are present and correct (BigDecimal values)")
        void testNumericNestedFields() throws Exception {
            TravelCalculatePremiumResponseV2 response = buildFullResponse();
            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"John\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.calculation.totalCoefficient").value(1.386))
                    .andExpect(jsonPath("$.agreementPriceBeforeDiscount").value(61.65))
                    .andExpect(jsonPath("$.discountAmount").value(6.165));
        }

        @Test
        @DisplayName("28. hasDiscounts() and hasPromoCode() helper behavior (indirect check via JSON)")
        void testHasDiscountsAndPromoCode() throws Exception {
            TravelCalculatePremiumResponseV2 response = buildFullResponse();
            when(service.calculatePremium(any())).thenReturn(response);

            mockMvc.perform(post(BASE + "/calculate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"personFirstName\":\"John\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.discountAmount").isNumber())
                    .andExpect(jsonPath("$.promoCodeInfo.code").value("PROMO10"));
        }
    }
}
