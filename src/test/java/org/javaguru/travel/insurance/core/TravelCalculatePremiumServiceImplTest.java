package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumServiceImpl Tests")
class TravelCalculatePremiumServiceImplTest {

    @Mock
    private DateTimeService dateTimeService;

    @Mock
    private TravelCalculatePremiumRequestValidator requestValidator;

    @InjectMocks
    private TravelCalculatePremiumServiceImpl service;

    @Nested
    @DisplayName("Successful Premium Calculation")
    class SuccessfulCalculation {

        private TravelCalculatePremiumRequest request;

        @BeforeEach
        void setUp() {
            request = createValidRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Should copy personFirstName from request to response")
        void shouldPopulatePersonFirstName() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getPersonFirstName(), response.getPersonFirstName());
        }

        @Test
        @DisplayName("Should copy personLastName from request to response")
        void shouldPopulatePersonLastName() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getPersonLastName(), response.getPersonLastName());
        }

        @Test
        @DisplayName("Should copy agreementDateFrom from request to response")
        void shouldPopulateAgreementDateFrom() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getAgreementDateFrom(), response.getAgreementDateFrom());
        }

        @Test
        @DisplayName("Should copy agreementDateTo from request to response")
        void shouldPopulateAgreementDateTo() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getAgreementDateTo(), response.getAgreementDateTo());
        }

        @Test
        @DisplayName("Should calculate and populate agreementPrice")
        void shouldPopulateAgreementPrice() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementPrice());
            assertEquals(new BigDecimal("10"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should not have errors in successful response")
        void shouldNotHaveErrorsInSuccessfulResponse() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertFalse(response.hasErrors());
        }

        @Test
        @DisplayName("Should call dateTimeService with correct dates")
        void shouldCallDateTimeServiceWithCorrectDates() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            service.calculatePremium(request);

            verify(dateTimeService, times(1)).getDaysBetween(
                    request.getAgreementDateFrom(),
                    request.getAgreementDateTo()
            );
        }

        @Test
        @DisplayName("Should call validator exactly once")
        void shouldCallValidatorOnce() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            service.calculatePremium(request);

            verify(requestValidator, times(1)).validate(request);
        }
    }

    @Nested
    @DisplayName("Price Calculation for Different Periods")
    class PriceCalculation {

        private TravelCalculatePremiumRequest request;

        @BeforeEach
        void setUp() {
            request = createValidRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Should calculate price for zero days (same dates)")
        void shouldCalculatePriceForZeroDays() {
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(0L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for one day")
        void shouldCalculatePriceForOneDay() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(1L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("1"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for 10 days")
        void shouldCalculatePriceForTenDays() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("10"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for 30 days (one month)")
        void shouldCalculatePriceForOneMonth() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("30"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for 365 days (one year)")
        void shouldCalculatePriceForOneYear() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(365L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("365"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for long trip (500+ days)")
        void shouldCalculatePriceForLongTrip() {
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(500L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("500"), response.getAgreementPrice());
        }
    }

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationErrorHandling {

        private TravelCalculatePremiumRequest request;

        @BeforeEach
        void setUp() {
            request = createValidRequest();
        }

        @Test
        @DisplayName("Should return errors when validation fails")
        void shouldReturnErrorsWhenValidationFails() {
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertTrue(response.hasErrors());
            assertEquals(1, response.getErrors().size());
            assertEquals("personFirstName", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Should return multiple errors")
        void shouldReturnMultipleErrors() {
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!"),
                    new ValidationError("personLastName", "Must not be empty!"),
                    new ValidationError("agreementDateFrom", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertTrue(response.hasErrors());
            assertEquals(3, response.getErrors().size());
        }

        @Test
        @DisplayName("Should not calculate price when validation fails")
        void shouldNotCalculatePriceWhenValidationFails() {
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNull(response.getAgreementPrice());
            verify(dateTimeService, never()).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should not populate fields when validation fails")
        void shouldNotPopulateFieldsWhenValidationFails() {
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNull(response.getPersonFirstName());
            assertNull(response.getPersonLastName());
            assertNull(response.getAgreementDateFrom());
            assertNull(response.getAgreementDateTo());
        }

        @Test
        @DisplayName("Should call validator even when request is invalid")
        void shouldCallValidatorEvenWhenRequestIsInvalid() {
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            service.calculatePremium(request);

            verify(requestValidator, times(1)).validate(request);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle request with special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName("Jean-Pierre");
            request.setPersonLastName("O'Connor");

            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("Jean-Pierre", response.getPersonFirstName());
            assertEquals("O'Connor", response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle request with Cyrillic characters in names")
        void shouldHandleCyrillicCharactersInNames() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName("Иван");
            request.setPersonLastName("Петров");

            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("Иван", response.getPersonFirstName());
            assertEquals("Петров", response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle very long names")
        void shouldHandleVeryLongNames() {
            TravelCalculatePremiumRequest request = createValidRequest();
            String longName = "A".repeat(100);
            request.setPersonFirstName(longName);
            request.setPersonLastName(longName);

            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(longName, response.getPersonFirstName());
            assertEquals(longName, response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle dates far in the future")
        void shouldHandleFutureDates() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.now().plusYears(10));
            request.setAgreementDateTo(LocalDate.now().plusYears(10).plusDays(30));

            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementPrice());
            assertEquals(new BigDecimal("30"), response.getAgreementPrice());
        }
    }

    private TravelCalculatePremiumRequest createValidRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Peterson");
        request.setAgreementDateFrom(LocalDate.now());
        request.setAgreementDateTo(LocalDate.now().plusDays(10));
        return request;
    }
}