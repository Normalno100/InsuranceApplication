package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.javaguru.travel.insurance.util.TestAssertions.*;
import static org.javaguru.travel.insurance.util.TestFixtures.*;
import static org.javaguru.travel.insurance.util.TestFixtures.ErrorMessages.*;
import static org.javaguru.travel.insurance.util.TestFixtures.StandardValues.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumServiceImpl Tests")
public class TravelCalculatePremiumServiceImplTest {

    @Mock
    private DateTimeService dateTimeService;

    @Mock
    private TravelCalculatePremiumRequestValidator requestValidator;

    @InjectMocks
    private TravelCalculatePremiumServiceImpl service;

    // ========== FIELD COPYING ==========

    @Nested
    @DisplayName("Successful Calculation - Field Copying")
    class FieldCopying {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);
        }

        @Test
        @DisplayName("Should copy personFirstName from request to response")
        void shouldCopyPersonFirstName() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertFirstNameCopied(request, response);
        }

        @Test
        @DisplayName("Should copy personLastName from request to response")
        void shouldCopyPersonLastName() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertLastNameCopied(request, response);
        }

        @Test
        @DisplayName("Should copy agreementDateFrom from request to response")
        void shouldCopyAgreementDateFrom() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertDateFromCopied(request, response);
        }

        @Test
        @DisplayName("Should copy agreementDateTo from request to response")
        void shouldCopyAgreementDateTo() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertDateToCopied(request, response);
        }

        @Test
        @DisplayName("Should copy all fields correctly in one response")
        void shouldCopyAllFields() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertFieldsCopied(request, response);
        }

        @ParameterizedTest
        @ValueSource(strings = {"John", "Jean-Pierre", "Mary Ann", "Иван", "José"})
        @DisplayName("Should correctly copy various first names")
        void shouldCopyVariousFirstNames(String firstName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(firstName);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(firstName, response.getPersonFirstName());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Smith", "O'Connor", "van der Berg", "Петров", "García"})
        @DisplayName("Should correctly copy various last names")
        void shouldCopyVariousLastNames(String lastName) {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonLastName(lastName);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(lastName, response.getPersonLastName());
        }
    }

    // ========== PRICE CALCULATION ==========

    @Nested
    @DisplayName("Price Calculation for Different Periods")
    class PriceCalculation {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
        }

        @ParameterizedTest(name = "{0} days should result in price {1}")
        @CsvSource({
                "0, 0",
                "1, 1",
                "5, 5",
                "10, 10",
                "30, 30",
                "90, 90",
                "180, 180",
                "365, 365",
                "500, 500",
                "1000, 1000"
        })
        @DisplayName("Should calculate correct price for various periods")
        void shouldCalculateCorrectPriceForVariousPeriods(long days, int expectedPrice) {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(days);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, expectedPrice);
        }

        @Test
        @DisplayName("Should calculate price for zero days (same dates)")
        void shouldCalculatePriceForZeroDays() {
            TravelCalculatePremiumRequest request = requestWithSameDates();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(0L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 0);
        }

        @Test
        @DisplayName("Should calculate price for one day")
        void shouldCalculatePriceForOneDay() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(1L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 1);
        }

        @Test
        @DisplayName("Should calculate price for one week (7 days)")
        void shouldCalculatePriceForOneWeek() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(7L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 7);
        }

        @Test
        @DisplayName("Should calculate price for one month (30 days)")
        void shouldCalculatePriceForOneMonth() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 30);
        }

        @Test
        @DisplayName("Should calculate price for one year (365 days)")
        void shouldCalculatePriceForOneYear() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(365L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 365);
        }

        @Test
        @DisplayName("Should calculate price for leap year (366 days)")
        void shouldCalculatePriceForLeapYear() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(366L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 366);
        }

        @Test
        @DisplayName("Should calculate price for very long trip (500+ days)")
        void shouldCalculatePriceForLongTrip() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(500L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 500);
        }

        @Test
        @DisplayName("Should not have errors in successful price calculation")
        void shouldNotHaveErrorsInSuccessfulCalculation() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertSuccessfulResponse(response);
        }
    }

    // ========== VALIDATION ERROR HANDLING ==========

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationErrorHandling {

        @Test
        @DisplayName("Should return errors when validation fails")
        void shouldReturnErrorsWhenValidationFails() {
            TravelCalculatePremiumRequest request = validRequest();
            List<ValidationError> errors = List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertHasErrors(response);
            assertErrorCount(response, 1);
            assertHasError(response, "personFirstName", MUST_NOT_BE_EMPTY);
        }

        @Test
        @DisplayName("Should return multiple validation errors")
        void shouldReturnMultipleErrors() {
            TravelCalculatePremiumRequest request = validRequest();
            List<ValidationError> errors = List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY),
                    error("personLastName", MUST_NOT_BE_EMPTY),
                    error("agreementDateFrom", MUST_NOT_BE_EMPTY)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertErrorCount(response, 3);
        }

        @Test
        @DisplayName("Should return all 4 errors when all fields are invalid")
        void shouldReturnAllFourErrors() {
            TravelCalculatePremiumRequest request = invalidRequest();
            List<ValidationError> errors = List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY),
                    error("personLastName", MUST_NOT_BE_EMPTY),
                    error("agreementDateFrom", MUST_NOT_BE_EMPTY),
                    error("agreementDateTo", MUST_NOT_BE_EMPTY)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertCompleteErrorResponse(response, 4);
        }

        @Test
        @DisplayName("Should NOT calculate price when validation fails")
        void shouldNotCalculatePriceWhenValidationFails() {
            TravelCalculatePremiumRequest request = validRequest();
            List<ValidationError> errors = List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPriceIsNull(response);
            verify(dateTimeService, never()).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should NOT populate fields when validation fails")
        void shouldNotPopulateFieldsWhenValidationFails() {
            TravelCalculatePremiumRequest request = validRequest();
            List<ValidationError> errors = List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertFieldsAreNull(response);
        }

        @Test
        @DisplayName("Should preserve error messages from validator")
        void shouldPreserveErrorMessages() {
            TravelCalculatePremiumRequest request = validRequest();
            List<ValidationError> errors = List.of(
                    error("agreementDateTo", MUST_BE_AFTER)
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertHasError(response, "agreementDateTo", MUST_BE_AFTER);
        }
    }

    // ========== DEPENDENCIES INTERACTION ==========

    @Nested
    @DisplayName("Dependencies Interaction")
    class DependenciesInteraction {

        @Test
        @DisplayName("Should call validator exactly once")
        void shouldCallValidatorOnce() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            service.calculatePremium(request);

            verify(requestValidator, times(1)).validate(request);
        }

        @Test
        @DisplayName("Should call dateTimeService with correct dates")
        void shouldCallDateTimeServiceWithCorrectDates() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            service.calculatePremium(request);

            verify(dateTimeService, times(1)).getDaysBetween(
                    request.getAgreementDateFrom(),
                    request.getAgreementDateTo()
            );
        }

        @Test
        @DisplayName("Should call dateTimeService exactly once on success")
        void shouldCallDateTimeServiceOnce() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            service.calculatePremium(request);

            verify(dateTimeService, times(1)).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should NOT call dateTimeService when validation fails")
        void shouldNotCallDateTimeServiceWhenValidationFails() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(request)).thenReturn(
                    List.of(error("personFirstName", MUST_NOT_BE_EMPTY))
            );

            service.calculatePremium(request);

            verify(dateTimeService, never()).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should call validator before dateTimeService")
        void shouldCallValidatorBeforeDateTimeService() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            service.calculatePremium(request);

            var inOrder = inOrder(requestValidator, dateTimeService);
            inOrder.verify(requestValidator).validate(request);
            inOrder.verify(dateTimeService).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should pass correct request to validator")
        void shouldPassCorrectRequestToValidator() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);
            ArgumentCaptor<TravelCalculatePremiumRequest> captor =
                    ArgumentCaptor.forClass(TravelCalculatePremiumRequest.class);

            service.calculatePremium(request);

            verify(requestValidator).validate(captor.capture());
            assertSame(request, captor.getValue());
        }

        @Test
        @DisplayName("Should pass correct dates to dateTimeService")
        void shouldPassCorrectDatesToDateTimeService() {
            TravelCalculatePremiumRequest request = validRequest();
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);
            ArgumentCaptor<LocalDate> dateFromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> dateToCaptor = ArgumentCaptor.forClass(LocalDate.class);

            service.calculatePremium(request);

            verify(dateTimeService).getDaysBetween(dateFromCaptor.capture(), dateToCaptor.capture());
            assertEquals(request.getAgreementDateFrom(), dateFromCaptor.getValue());
            assertEquals(request.getAgreementDateTo(), dateToCaptor.getValue());
        }
    }

    // ========== EDGE CASES ==========

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Should handle request with special characters in names")
        void shouldHandleSpecialCharactersInNames() {
            TravelCalculatePremiumRequest request = requestWithSpecialCharacters();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(SPECIAL_FIRST_NAME, response.getPersonFirstName());
            assertEquals(SPECIAL_LAST_NAME, response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle request with Cyrillic characters")
        void shouldHandleCyrillicCharacters() {
            TravelCalculatePremiumRequest request = requestWithCyrillicNames();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(CYRILLIC_FIRST_NAME, response.getPersonFirstName());
            assertEquals(CYRILLIC_LAST_NAME, response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle very long names")
        void shouldHandleVeryLongNames() {
            String longName = "A".repeat(100);
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName(longName);
            request.setPersonLastName(longName);
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(longName, response.getPersonFirstName());
            assertEquals(longName, response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle single character names")
        void shouldHandleSingleCharacterNames() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setPersonFirstName("A");
            request.setPersonLastName("B");
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("A", response.getPersonFirstName());
            assertEquals("B", response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle dates far in the future")
        void shouldHandleFutureDates() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().plusYears(10));
            request.setAgreementDateTo(LocalDate.now().plusYears(10).plusDays(30));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 30);
        }

        @Test
        @DisplayName("Should handle dates in the past")
        void shouldHandlePastDates() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(LocalDate.now().minusYears(1));
            request.setAgreementDateTo(LocalDate.now().minusYears(1).plusDays(10));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 10);
        }

        @Test
        @DisplayName("Should handle leap year dates")
        void shouldHandleLeapYearDates() {
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(SpecialDates.leapYearDate().minusDays(1));
            request.setAgreementDateTo(SpecialDates.leapYearDate().plusDays(1));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(2L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 2);
        }

        @Test
        @DisplayName("Should handle year boundary crossing")
        void shouldHandleYearBoundaryCrossing() {
            LocalDate[] dates = SpecialDates.yearBoundaryDates();
            TravelCalculatePremiumRequest request = validRequest();
            request.setAgreementDateFrom(dates[0]);
            request.setAgreementDateTo(dates[1]);
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(6L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPrice(response, 6);
        }
    }

    // ========== TYPE AND VALUE VERIFICATION ==========

    @Nested
    @DisplayName("Type and Value Verification")
    class TypeAndValueVerification {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);
        }

        @Test
        @DisplayName("Should return non-null response")
        void shouldReturnNonNullResponse() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertResponseNotNull(response);
        }

        @Test
        @DisplayName("Should return response with BigDecimal price")
        void shouldReturnBigDecimalPrice() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPriceType(response);
        }

        @Test
        @DisplayName("Should return response with correct LocalDate types")
        void shouldReturnCorrectDateTypes() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertDateTypes(response);
        }

        @Test
        @DisplayName("Should return response with correct String types for names")
        void shouldReturnCorrectNameTypes() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getPersonFirstName());
            assertNotNull(response.getPersonLastName());
            assertTrue(response.getPersonFirstName() instanceof String);
            assertTrue(response.getPersonLastName() instanceof String);
        }

        @Test
        @DisplayName("Should return price with correct scale")
        void shouldReturnPriceWithCorrectScale() {
            TravelCalculatePremiumRequest request = validRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertPriceScale(response);
        }
    }

    // ========== COMPLETE SCENARIOS ==========

    @Nested
    @DisplayName("Complete Scenarios - Integration")
    class CompleteScenarios {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
        }

        @Test
        @DisplayName("Complete successful scenario: valid request -> successful response")
        void completeSuccessfulScenario() {
            TravelCalculatePremiumRequest request = validRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertCompleteSuccessfulResponse(request, response, STANDARD_DAYS);
        }

        @Test
        @DisplayName("Complete error scenario: invalid request -> error response")
        void completeErrorScenario() {
            TravelCalculatePremiumRequest request = invalidRequest();
            when(requestValidator.validate(request)).thenReturn(List.of(
                    error("personFirstName", MUST_NOT_BE_EMPTY),
                    error("personLastName", MUST_NOT_BE_EMPTY),
                    error("agreementDateFrom", MUST_NOT_BE_EMPTY),
                    error("agreementDateTo", MUST_NOT_BE_EMPTY)
            ));

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertCompleteErrorResponse(response, 4);
        }

        @Test
        @DisplayName("Complete scenario: special characters -> successful response")
        void completeScenarioWithSpecialCharacters() {
            TravelCalculatePremiumRequest request = requestWithSpecialCharacters();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(STANDARD_DAYS);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertAll(
                    () -> assertSuccessfulResponse(response),
                    () -> assertEquals(SPECIAL_FIRST_NAME, response.getPersonFirstName()),
                    () -> assertEquals(SPECIAL_LAST_NAME, response.getPersonLastName()),
                    () -> assertPrice(response, STANDARD_DAYS)
            );
        }

        @Test
        @DisplayName("Complete scenario: zero days -> zero price")
        void completeScenarioZeroDays() {
            TravelCalculatePremiumRequest request = requestWithSameDates();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(0L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertCompleteSuccessfulResponse(request, response, 0);
        }

        @Test
        @DisplayName("Complete scenario: long trip -> correct price")
        void completeScenarioLongTrip() {
            TravelCalculatePremiumRequest request = requestWithDays(365);
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(365L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertCompleteSuccessfulResponse(request, response, 365);
        }
    }
}