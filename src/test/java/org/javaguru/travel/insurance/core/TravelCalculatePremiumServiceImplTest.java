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

    // ========== ГРУППА 1: УСПЕШНЫЕ СЦЕНАРИИ - КОПИРОВАНИЕ ПОЛЕЙ ==========

    @Nested
    @DisplayName("Successful Calculation - Field Copying")
    class FieldCopying {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);
        }

        @Test
        @DisplayName("Should copy personFirstName from request to response")
        void shouldCopyPersonFirstName() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getPersonFirstName(), response.getPersonFirstName());
        }

        @Test
        @DisplayName("Should copy personLastName from request to response")
        void shouldCopyPersonLastName() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getPersonLastName(), response.getPersonLastName());
        }

        @Test
        @DisplayName("Should copy agreementDateFrom from request to response")
        void shouldCopyAgreementDateFrom() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getAgreementDateFrom(), response.getAgreementDateFrom());
        }

        @Test
        @DisplayName("Should copy agreementDateTo from request to response")
        void shouldCopyAgreementDateTo() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(request.getAgreementDateTo(), response.getAgreementDateTo());
        }

        @Test
        @DisplayName("Should copy all fields correctly in one response")
        void shouldCopyAllFields() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertAll(
                    () -> assertEquals(request.getPersonFirstName(), response.getPersonFirstName()),
                    () -> assertEquals(request.getPersonLastName(), response.getPersonLastName()),
                    () -> assertEquals(request.getAgreementDateFrom(), response.getAgreementDateFrom()),
                    () -> assertEquals(request.getAgreementDateTo(), response.getAgreementDateTo())
            );
        }

        @ParameterizedTest
        @ValueSource(strings = {"John", "Jean-Pierre", "Mary Ann", "Иван", "José"})
        @DisplayName("Should correctly copy various first names")
        void shouldCopyVariousFirstNames(String firstName) {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName(firstName);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(firstName, response.getPersonFirstName());
        }

        @ParameterizedTest
        @ValueSource(strings = {"Smith", "O'Connor", "van der Berg", "Петров", "García"})
        @DisplayName("Should correctly copy various last names")
        void shouldCopyVariousLastNames(String lastName) {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonLastName(lastName);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(lastName, response.getPersonLastName());
        }
    }

    // ========== ГРУППА 2: РАСЧЕТ ЦЕНЫ ДЛЯ РАЗНЫХ ПЕРИОДОВ ==========

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
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(days);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal(expectedPrice), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for zero days (same dates)")
        void shouldCalculatePriceForZeroDays() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.now());
            request.setAgreementDateTo(LocalDate.now());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(0L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for one day")
        void shouldCalculatePriceForOneDay() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(1L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("1"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for one week (7 days)")
        void shouldCalculatePriceForOneWeek() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(7L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("7"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for one month (30 days)")
        void shouldCalculatePriceForOneMonth() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("30"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for one year (365 days)")
        void shouldCalculatePriceForOneYear() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(365L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("365"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for leap year (366 days)")
        void shouldCalculatePriceForLeapYear() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(366L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("366"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should calculate price for very long trip (500+ days)")
        void shouldCalculatePriceForLongTrip() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(500L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("500"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should not have errors in successful price calculation")
        void shouldNotHaveErrorsInSuccessfulCalculation() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertFalse(response.hasErrors());
            assertNull(response.getErrors());
        }
    }

    // ========== ГРУППА 3: ОБРАБОТКА ОШИБОК ВАЛИДАЦИИ ==========

    @Nested
    @DisplayName("Validation Error Handling")
    class ValidationErrorHandling {

        @Test
        @DisplayName("Should return errors when validation fails")
        void shouldReturnErrorsWhenValidationFails() {
            TravelCalculatePremiumRequest request = createValidRequest();
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertTrue(response.hasErrors());
            assertEquals(1, response.getErrors().size());
            assertEquals("personFirstName", response.getErrors().get(0).getField());
            assertEquals("Must not be empty!", response.getErrors().get(0).getMessage());
        }

        @Test
        @DisplayName("Should return multiple validation errors")
        void shouldReturnMultipleErrors() {
            TravelCalculatePremiumRequest request = createValidRequest();
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
        @DisplayName("Should return all 4 errors when all fields are invalid")
        void shouldReturnAllFourErrors() {
            TravelCalculatePremiumRequest request = createValidRequest();
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!"),
                    new ValidationError("personLastName", "Must not be empty!"),
                    new ValidationError("agreementDateFrom", "Must not be empty!"),
                    new ValidationError("agreementDateTo", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertTrue(response.hasErrors());
            assertEquals(4, response.getErrors().size());
        }

        @Test
        @DisplayName("Should NOT calculate price when validation fails")
        void shouldNotCalculatePriceWhenValidationFails() {
            TravelCalculatePremiumRequest request = createValidRequest();
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNull(response.getAgreementPrice());
            verify(dateTimeService, never()).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should NOT populate fields when validation fails")
        void shouldNotPopulateFieldsWhenValidationFails() {
            TravelCalculatePremiumRequest request = createValidRequest();
            List<ValidationError> errors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNull(response.getPersonFirstName());
            assertNull(response.getPersonLastName());
            assertNull(response.getAgreementDateFrom());
            assertNull(response.getAgreementDateTo());
            assertNull(response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should preserve error messages from validator")
        void shouldPreserveErrorMessages() {
            TravelCalculatePremiumRequest request = createValidRequest();
            List<ValidationError> errors = List.of(
                    new ValidationError("agreementDateTo", "Must be after agreementDateFrom!")
            );
            when(requestValidator.validate(request)).thenReturn(errors);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("Must be after agreementDateFrom!", response.getErrors().get(0).getMessage());
        }
    }

    // ========== ГРУППА 4: ВЗАИМОДЕЙСТВИЕ С ЗАВИСИМОСТЯМИ ==========

    @Nested
    @DisplayName("Dependencies Interaction")
    class DependenciesInteraction {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);
        }

        @Test
        @DisplayName("Should call validator exactly once")
        void shouldCallValidatorOnce() {
            TravelCalculatePremiumRequest request = createValidRequest();

            service.calculatePremium(request);

            verify(requestValidator, times(1)).validate(request);
        }

        @Test
        @DisplayName("Should call dateTimeService with correct dates")
        void shouldCallDateTimeServiceWithCorrectDates() {
            TravelCalculatePremiumRequest request = createValidRequest();

            service.calculatePremium(request);

            verify(dateTimeService, times(1)).getDaysBetween(
                    request.getAgreementDateFrom(),
                    request.getAgreementDateTo()
            );
        }

        @Test
        @DisplayName("Should call dateTimeService exactly once on success")
        void shouldCallDateTimeServiceOnce() {
            TravelCalculatePremiumRequest request = createValidRequest();

            service.calculatePremium(request);

            verify(dateTimeService, times(1)).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should NOT call dateTimeService when validation fails")
        void shouldNotCallDateTimeServiceWhenValidationFails() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(requestValidator.validate(request)).thenReturn(
                    List.of(new ValidationError("personFirstName", "Must not be empty!"))
            );

            service.calculatePremium(request);

            verify(dateTimeService, never()).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should call validator before dateTimeService")
        void shouldCallValidatorBeforeDateTimeService() {
            TravelCalculatePremiumRequest request = createValidRequest();

            service.calculatePremium(request);

            var inOrder = inOrder(requestValidator, dateTimeService);
            inOrder.verify(requestValidator).validate(request);
            inOrder.verify(dateTimeService).getDaysBetween(any(), any());
        }

        @Test
        @DisplayName("Should pass correct request to validator")
        void shouldPassCorrectRequestToValidator() {
            TravelCalculatePremiumRequest request = createValidRequest();
            ArgumentCaptor<TravelCalculatePremiumRequest> captor =
                    ArgumentCaptor.forClass(TravelCalculatePremiumRequest.class);

            service.calculatePremium(request);

            verify(requestValidator).validate(captor.capture());
            assertSame(request, captor.getValue());
        }

        @Test
        @DisplayName("Should pass correct dates to dateTimeService")
        void shouldPassCorrectDatesToDateTimeService() {
            TravelCalculatePremiumRequest request = createValidRequest();
            ArgumentCaptor<LocalDate> dateFromCaptor = ArgumentCaptor.forClass(LocalDate.class);
            ArgumentCaptor<LocalDate> dateToCaptor = ArgumentCaptor.forClass(LocalDate.class);

            service.calculatePremium(request);

            verify(dateTimeService).getDaysBetween(dateFromCaptor.capture(), dateToCaptor.capture());
            assertEquals(request.getAgreementDateFrom(), dateFromCaptor.getValue());
            assertEquals(request.getAgreementDateTo(), dateToCaptor.getValue());
        }
    }

    // ========== ГРУППА 5: ГРАНИЧНЫЕ СЛУЧАИ ==========

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
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName("Jean-Pierre");
            request.setPersonLastName("O'Connor");
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("Jean-Pierre", response.getPersonFirstName());
            assertEquals("O'Connor", response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle request with Cyrillic characters")
        void shouldHandleCyrillicCharacters() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName("Иван");
            request.setPersonLastName("Петров");
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
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(longName, response.getPersonFirstName());
            assertEquals(longName, response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle single character names")
        void shouldHandleSingleCharacterNames() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setPersonFirstName("A");
            request.setPersonLastName("B");
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals("A", response.getPersonFirstName());
            assertEquals("B", response.getPersonLastName());
        }

        @Test
        @DisplayName("Should handle dates far in the future")
        void shouldHandleFutureDates() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.now().plusYears(10));
            request.setAgreementDateTo(LocalDate.now().plusYears(10).plusDays(30));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(30L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementPrice());
            assertEquals(new BigDecimal("30"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should handle dates in the past")
        void shouldHandlePastDates() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.now().minusYears(1));
            request.setAgreementDateTo(LocalDate.now().minusYears(1).plusDays(10));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementPrice());
            assertEquals(new BigDecimal("10"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should handle leap year dates")
        void shouldHandleLeapYearDates() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.of(2024, 2, 28));
            request.setAgreementDateTo(LocalDate.of(2024, 3, 1));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(2L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("2"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should handle year boundary crossing")
        void shouldHandleYearBoundaryCrossing() {
            TravelCalculatePremiumRequest request = createValidRequest();
            request.setAgreementDateFrom(LocalDate.of(2023, 12, 30));
            request.setAgreementDateTo(LocalDate.of(2024, 1, 5));
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(6L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertEquals(new BigDecimal("6"), response.getAgreementPrice());
        }
    }

    // ========== ГРУППА 6: ПРОВЕРКА ТИПОВ И ЗНАЧЕНИЙ ==========

    @Nested
    @DisplayName("Type and Value Verification")
    class TypeAndValueVerification {

        @BeforeEach
        void setUp() {
            when(requestValidator.validate(any())).thenReturn(Collections.emptyList());
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);
        }

        @Test
        @DisplayName("Should return non-null response")
        void shouldReturnNonNullResponse() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response);
        }

        @Test
        @DisplayName("Should return response with BigDecimal price")
        void shouldReturnBigDecimalPrice() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementPrice());
            assertTrue(response.getAgreementPrice() instanceof BigDecimal);
        }

        @Test
        @DisplayName("Should return response with correct LocalDate types")
        void shouldReturnCorrectDateTypes() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getAgreementDateFrom());
            assertNotNull(response.getAgreementDateTo());
            assertTrue(response.getAgreementDateFrom() instanceof LocalDate);
            assertTrue(response.getAgreementDateTo() instanceof LocalDate);
        }

        @Test
        @DisplayName("Should return response with correct String types for names")
        void shouldReturnCorrectNameTypes() {
            TravelCalculatePremiumRequest request = createValidRequest();

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            assertNotNull(response.getPersonFirstName());
            assertNotNull(response.getPersonLastName());
            assertTrue(response.getPersonFirstName() instanceof String);
            assertTrue(response.getPersonLastName() instanceof String);
        }

        @Test
        @DisplayName("Should return price with correct scale")
        void shouldReturnPriceWithCorrectScale() {
            TravelCalculatePremiumRequest request = createValidRequest();
            when(dateTimeService.getDaysBetween(any(), any())).thenReturn(10L);

            TravelCalculatePremiumResponse response = service.calculatePremium(request);

            // BigDecimal created from long should have scale 0
            assertEquals(0, response.getAgreementPrice().scale());
        }
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumRequest createValidRequest() {
        TravelCalculatePremiumRequest request = new TravelCalculatePremiumRequest();
        request.setPersonFirstName("John");
        request.setPersonLastName("Peterson");
        request.setAgreementDateFrom(LocalDate.now());
        request.setAgreementDateTo(LocalDate.now().plusDays(10));
        return request;
    }
}