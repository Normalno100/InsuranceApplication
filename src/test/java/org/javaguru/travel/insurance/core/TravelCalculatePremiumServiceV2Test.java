package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumServiceV2 Tests")
class TravelCalculatePremiumServiceV2Test {

    @Mock
    private TravelCalculatePremiumRequestValidatorV2Impl validator;

    @Mock
    private MedicalRiskPremiumCalculator medicalRiskCalculator;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private DiscountService discountService;

    @Mock
    private TravelCalculatePremiumServiceV2.CurrencyExchangeService currencyExchangeService;

    private TravelCalculatePremiumServiceV2 service;

    @BeforeEach
    void setUp() {
        service = new TravelCalculatePremiumServiceV2(
                validator,
                medicalRiskCalculator,
                promoCodeService,
                discountService,
                currencyExchangeService
        );
    }

    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should return validation errors when request is invalid")
        void shouldReturnValidationErrorsWhenRequestIsInvalid() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();
            List<ValidationError> validationErrors = List.of(
                    new ValidationError("personFirstName", "Must not be empty!")
            );

            when(validator.validate(request)).thenReturn(validationErrors);

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertTrue(response.hasErrors());
            assertEquals(1, response.getErrors().size());
            assertEquals("personFirstName", response.getErrors().get(0).getField());
            verify(medicalRiskCalculator, never()).calculatePremiumWithDetails(any());
        }

        @Test
        @DisplayName("Should proceed with calculation when validation passes")
        void shouldProceedWithCalculationWhenValidationPasses() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            verify(medicalRiskCalculator).calculatePremiumWithDetails(request);
        }
    }

    @Nested
    @DisplayName("Premium Calculation Tests")
    class PremiumCalculationTests {

        @Test
        @DisplayName("Should calculate premium without discounts")
        void shouldCalculatePremiumWithoutDiscounts() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertNotNull(response.getAgreementPrice());
            assertEquals(new BigDecimal("63.00"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Should apply minimum premium of 10 EUR")
        void shouldApplyMinimumPremium() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();

            when(validator.validate(request)).thenReturn(List.of());
            mockCalculationWithLowPremium();
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertEquals(new BigDecimal("10.00"), response.getAgreementPrice());
        }
    }

    @Nested
    @DisplayName("Promo Code Tests")
    class PromoCodeTests {

        @Test
        @DisplayName("Should apply valid promo code")
        void shouldApplyValidPromoCode() {
            // Given
            TravelCalculatePremiumRequestV2 request = createRequestWithPromoCode("SUMMER2025");

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            PromoCodeService.PromoCodeResult promoResult = new PromoCodeService.PromoCodeResult(
                    true,
                    null,
                    "SUMMER2025",
                    "Summer discount",
                    PromoCodeService.DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    new BigDecimal("6.30")
            );
            when(promoCodeService.applyPromoCode(eq("SUMMER2025"), any(), any()))
                    .thenReturn(promoResult);
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertTrue(response.hasPromoCode());
            assertEquals("SUMMER2025", response.getPromoCodeInfo().getCode());
            assertEquals(new BigDecimal("6.30"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Should not apply invalid promo code")
        void shouldNotApplyInvalidPromoCode() {
            // Given
            TravelCalculatePremiumRequestV2 request = createRequestWithPromoCode("INVALID");

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            PromoCodeService.PromoCodeResult promoResult = new PromoCodeService.PromoCodeResult(
                    false,
                    "Promo code not found",
                    null,
                    null,
                    null,
                    null,
                    null
            );
            when(promoCodeService.applyPromoCode(eq("INVALID"), any(), any()))
                    .thenReturn(promoResult);
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertFalse(response.hasPromoCode());
            assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
        }
    }

    @Nested
    @DisplayName("Discount Tests")
    class DiscountTests {

        @Test
        @DisplayName("Should apply group discount")
        void shouldApplyGroupDiscount() {
            // Given
            TravelCalculatePremiumRequestV2 request = createRequestWithPersonsCount(10);

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            DiscountService.DiscountResult discount = new DiscountService.DiscountResult(
                    "GROUP_10",
                    "Group discount 10 persons",
                    DiscountService.DiscountType.GROUP,
                    new BigDecimal("15"),
                    new BigDecimal("9.45")
            );
            when(discountService.calculateBestDiscount(any(), eq(10), anyBoolean(), any()))
                    .thenReturn(Optional.of(discount));

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertTrue(response.hasDiscounts());
            assertEquals(new BigDecimal("9.45"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Should apply corporate discount")
        void shouldApplyCorporateDiscount() {
            // Given
            TravelCalculatePremiumRequestV2 request = createCorporateRequest();

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            DiscountService.DiscountResult discount = new DiscountService.DiscountResult(
                    "CORPORATE",
                    "Corporate discount",
                    DiscountService.DiscountType.CORPORATE,
                    new BigDecimal("20"),
                    new BigDecimal("12.60")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), eq(true), any()))
                    .thenReturn(Optional.of(discount));

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertTrue(response.hasDiscounts());
            assertEquals(new BigDecimal("12.60"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Should combine promo code and discount")
        void shouldCombinePromoCodeAndDiscount() {
            // Given
            TravelCalculatePremiumRequestV2 request = createRequestWithPromoCode("SUMMER2025");
            request.setPersonsCount(10);

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();

            PromoCodeService.PromoCodeResult promoResult = new PromoCodeService.PromoCodeResult(
                    true,
                    null,
                    "SUMMER2025",
                    "Summer discount",
                    PromoCodeService.DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    new BigDecimal("6.30")
            );
            when(promoCodeService.applyPromoCode(eq("SUMMER2025"), any(), any()))
                    .thenReturn(promoResult);

            DiscountService.DiscountResult discount = new DiscountService.DiscountResult(
                    "GROUP_10",
                    "Group discount",
                    DiscountService.DiscountType.GROUP,
                    new BigDecimal("15"),
                    new BigDecimal("9.45")
            );
            when(discountService.calculateBestDiscount(any(), eq(10), anyBoolean(), any()))
                    .thenReturn(Optional.of(discount));

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertEquals(new BigDecimal("15.75"), response.getDiscountAmount()); // 6.30 + 9.45
        }
    }

    @Nested
    @DisplayName("Currency Conversion Tests")
    class CurrencyConversionTests {

        @Test
        @DisplayName("Should convert to USD")
        void shouldConvertToUSD() {
            // Given
            TravelCalculatePremiumRequestV2 request = createRequestWithCurrency("USD");

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());
            when(currencyExchangeService.convert(any(), eq("EUR"), eq("USD")))
                    .thenAnswer(inv -> inv.getArgument(0, BigDecimal.class).multiply(new BigDecimal("1.08")));

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertEquals("USD", response.getCurrency());
            verify(currencyExchangeService, times(3)).convert(any(), eq("EUR"), eq("USD"));
        }

        @Test
        @DisplayName("Should not convert when currency is EUR")
        void shouldNotConvertWhenCurrencyIsEUR() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();

            when(validator.validate(request)).thenReturn(List.of());
            mockSuccessfulCalculation();
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.empty());

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertFalse(response.hasErrors());
            assertEquals("EUR", response.getCurrency());
            verify(currencyExchangeService, never()).convert(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle calculation exception")
        void shouldHandleCalculationException() {
            // Given
            TravelCalculatePremiumRequestV2 request = createValidRequest();

            when(validator.validate(request)).thenReturn(List.of());
            when(medicalRiskCalculator.calculatePremiumWithDetails(request))
                    .thenThrow(new RuntimeException("Calculation error"));

            // When
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);

            // Then
            assertTrue(response.hasErrors());
            assertEquals(1, response.getErrors().size());
            assertEquals("system", response.getErrors().get(0).getField());
            assertTrue(response.getErrors().get(0).getMessage().contains("Calculation error"));
        }
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumRequestV2 createValidRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    private TravelCalculatePremiumRequestV2 createRequestWithPromoCode(String promoCode) {
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPromoCode(promoCode);
        return request;
    }

    private TravelCalculatePremiumRequestV2 createRequestWithPersonsCount(int count) {
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setPersonsCount(count);
        return request;
    }

    private TravelCalculatePremiumRequestV2 createCorporateRequest() {
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setIsCorporate(true);
        return request;
    }

    private TravelCalculatePremiumRequestV2 createRequestWithCurrency(String currency) {
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        request.setCurrency(currency);
        return request;
    }

    private void mockSuccessfulCalculation() {
        MedicalRiskPremiumCalculator.PremiumCalculationResult result =
                new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                        new BigDecimal("63.00"),
                        new BigDecimal("4.50"),
                        35,
                        new BigDecimal("1.0"),
                        "Young adults",
                        new BigDecimal("1.0"),
                        "Spain",
                        BigDecimal.ZERO,
                        new BigDecimal("1.0"),
                        14,
                        new BigDecimal("50000"),
                        List.of(),
                        List.of()
                );

        when(medicalRiskCalculator.calculatePremiumWithDetails(any())).thenReturn(result);
    }

    private void mockCalculationWithLowPremium() {
        MedicalRiskPremiumCalculator.PremiumCalculationResult result =
                new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                        new BigDecimal("5.00"), // Low premium
                        new BigDecimal("4.50"),
                        35,
                        new BigDecimal("1.0"),
                        "Young adults",
                        new BigDecimal("1.0"),
                        "Spain",
                        BigDecimal.ZERO,
                        new BigDecimal("1.0"),
                        1, // 1 day
                        new BigDecimal("50000"),
                        List.of(),
                        List.of()
                );

        when(medicalRiskCalculator.calculatePremiumWithDetails(any())).thenReturn(result);
    }
}