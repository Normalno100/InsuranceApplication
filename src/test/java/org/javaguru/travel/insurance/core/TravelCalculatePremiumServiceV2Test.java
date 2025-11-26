package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2.CurrencyExchangeService;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2.TravelCalculatePremiumRequestValidatorV2;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("TravelCalculatePremiumServiceV2 tests")
class TravelCalculatePremiumServiceV2Test {

    private TravelCalculatePremiumRequestValidatorV2 validator;
    private MedicalRiskPremiumCalculator calculator;
    private PromoCodeService promoCodeService;
    private DiscountService discountService;
    private CurrencyExchangeService currencyExchangeService;
    private TravelCalculatePremiumServiceV2 service;

    @BeforeEach
    void setUp() {
        validator = mock(TravelCalculatePremiumRequestValidatorV2.class);
        calculator = mock(MedicalRiskPremiumCalculator.class);
        promoCodeService = mock(PromoCodeService.class);
        discountService = mock(DiscountService.class);
        currencyExchangeService = mock(CurrencyExchangeService.class);

        service = new TravelCalculatePremiumServiceV2(
                validator,
                calculator,
                promoCodeService,
                discountService,
                currencyExchangeService
        );

        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(baseCalculationResult(new BigDecimal("500.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());
        lenient().when(currencyExchangeService.convert(any(), anyString(), anyString()))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0))
                        .setScale(2, RoundingMode.HALF_UP));
    }

    @Nested
    @DisplayName("Happy path scenarios")
    class HappyPathScenarios {

        @Test
        @DisplayName("Calculates premium successfully for valid request")
        void calculatesPremiumSuccessfully() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertFalse(response.hasErrors());
            assertEquals(new BigDecimal("500.00"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Response contains risk details from calculator")
        void responseContainsRiskDetails() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(1, response.getRiskPremiums().size());
            assertEquals("TRAVEL_MEDICAL", response.getRiskPremiums().get(0).getRiskType());
        }

        @Test
        @DisplayName("Response contains calculation steps")
        void responseContainsCalculationSteps() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(1, response.getCalculation().getSteps().size());
            assertEquals("step", response.getCalculation().getSteps().get(0).getDescription());
        }

        @Test
        @DisplayName("Response uses request person names")
        void responseUsesPersonNames() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("John", response.getPersonFirstName());
            assertEquals("Smith", response.getPersonLastName());
        }

        @Test
        @DisplayName("Response includes country name from calculation")
        void responseIncludesCountryName() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("Spain", response.getCountryName());
        }

        @Test
        @DisplayName("Response sets agreement days from calculation result")
        void responseSetsAgreementDays() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(5, response.getAgreementDays());
        }

        @Test
        @DisplayName("Response contains coverage amount")
        void responseContainsCoverageAmount() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(new BigDecimal("100000"), response.getCoverageAmount());
        }

        @Test
        @DisplayName("Response includes selected risks from request")
        void responseIncludesSelectedRisks() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "TRIP_CANCELLATION"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(2, response.getSelectedRisks().size());
        }

        @Test
        @DisplayName("Response keeps currency as EUR when not requested otherwise")
        void responseKeepsCurrencyAsEur() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("EUR", response.getCurrency());
        }

        @Test
        @DisplayName("Response handles zero discounts gracefully")
        void responseHandlesZeroDiscounts() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
            assertFalse(response.hasDiscounts());
        }

        @Test
        @DisplayName("Applies minimum premium when value falls below threshold")
        void appliesMinimumPremium() {
            when(calculator.calculatePremiumWithDetails(any()))
                    .thenReturn(baseCalculationResult(new BigDecimal("8.00")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(new BigDecimal("10.00"), response.getAgreementPrice());
        }

        @Test
        @DisplayName("Does not let premium drop below zero when discounts exceed premium")
        void preventsNegativePremium() {
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(new DiscountService.DiscountResult(
                            "GROUP_5", "Group", DiscountService.DiscountType.GROUP,
                            new BigDecimal("50"), new BigDecimal("600.00")
                    )));

            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("SAVE10");
            when(promoCodeService.applyPromoCode(anyString(), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "SAVE10", "Save 10", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            BigDecimal.TEN, new BigDecimal("1000.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
        }

        @Test
        @DisplayName("Includes calculation formula in response")
        void includesCalculationFormula() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertNotNull(response.getCalculation().getFormula());
            assertTrue(response.getCalculation().getFormula().contains("Premium"));
        }

        @Test
        @DisplayName("Promo info remains null when promo not applied")
        void promoInfoNullWhenNotApplied() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertFalse(response.hasPromoCode());
        }

        @Test
        @DisplayName("Applied discounts list stays null when no discounts")
        void appliedDiscountsNullWhenEmpty() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertNull(response.getAppliedDiscounts());
        }
    }

    @Nested
    @DisplayName("Validation scenarios")
    class ValidationScenarios {

        @Test
        @DisplayName("Returns response with validation errors")
        void returnsValidationErrors() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personFirstName", "Required")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertTrue(response.hasErrors());
            assertEquals(1, response.getErrors().size());
        }

        @Test
        @DisplayName("Does not invoke calculator when validation fails")
        void skipsCalculatorOnValidationFailure() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personLastName", "Required")));

            service.calculatePremium(baseRequest());
            verify(calculator, never()).calculatePremiumWithDetails(any());
        }

        @Test
        @DisplayName("Handles multiple validation errors")
        void handlesMultipleValidationErrors() {
            List<ValidationError> errors = List.of(
                    new ValidationError("first", "err1"),
                    new ValidationError("second", "err2")
            );
            when(validator.validate(any())).thenReturn(errors);

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(2, response.getErrors().size());
        }

        @Test
        @DisplayName("Validation response preserves field names")
        void preservesFieldNames() {
            ValidationError error = new ValidationError("countryIsoCode", "Unknown");
            when(validator.validate(any())).thenReturn(List.of(error));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("countryIsoCode", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation response contains messages")
        void preservesMessages() {
            ValidationError error = new ValidationError("medicalRiskLimitLevel", "Required");
            when(validator.validate(any())).thenReturn(List.of(error));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("Required", response.getErrors().get(0).getMessage());
        }

        @Test
        @DisplayName("Validation errors result in empty response body fields")
        void validationLeavesBodyEmpty() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personBirthDate", "Required")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertNull(response.getPersonFirstName());
            assertNull(response.getAgreementPrice());
        }

        @Test
        @DisplayName("Validation reflects missing first name")
        void missingFirstNameError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personFirstName", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("personFirstName", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing last name")
        void missingLastNameError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personLastName", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("personLastName", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing birth date")
        void missingBirthDateError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personBirthDate", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("personBirthDate", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing agreement start date")
        void missingAgreementFromError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("agreementDateFrom", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("agreementDateFrom", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing agreement end date")
        void missingAgreementToError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("agreementDateTo", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("agreementDateTo", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects invalid date order")
        void invalidDateOrderError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("agreementDateTo", "before from")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("agreementDateTo", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing country")
        void missingCountryError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("countryIsoCode", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("countryIsoCode", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation reflects missing medical risk limit")
        void missingMedicalRiskError() {
            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("medicalRiskLimitLevel", "missing")));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("medicalRiskLimitLevel", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Validation response wraps validator errors unchanged")
        void responseContainsOriginalErrorObjects() {
            ValidationError error = new ValidationError("field", "message");
            when(validator.validate(any())).thenReturn(List.of(error));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertSame(error, response.getErrors().get(0));
        }

        @Test
        @DisplayName("Validation clears previous calculation state")
        void validationClearsCalculationState() {
            TravelCalculatePremiumResponseV2 success = service.calculatePremium(baseRequest());
            assertFalse(success.hasErrors());

            when(validator.validate(any()))
                    .thenReturn(List.of(new ValidationError("personFirstName", "Required")));
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertTrue(response.hasErrors());
        }
    }

    @Nested
    @DisplayName("Promo code application")
    class PromoCodeScenarios {

        @Test
        @DisplayName("Applies valid promo code discount")
        void appliesValidPromoCode() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("SUMMER");
            when(promoCodeService.applyPromoCode(eq("SUMMER"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "SUMMER", "Summer promo",
                            PromoCodeService.DiscountType.PERCENTAGE,
                            BigDecimal.TEN, new BigDecimal("50.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(new BigDecimal("50.00"), response.getDiscountAmount());
            assertTrue(response.hasPromoCode());
        }

        @Test
        @DisplayName("Promo code info contains details when applied")
        void promoCodeInfoContainsDetails() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("WELCOME");
            when(promoCodeService.applyPromoCode(eq("WELCOME"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "WELCOME", "Welcome bonus",
                            PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("50"), new BigDecimal("50.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals("WELCOME", response.getPromoCodeInfo().getCode());
            assertEquals(new BigDecimal("50.00"), response.getPromoCodeInfo().getActualDiscountAmount());
        }

        @Test
        @DisplayName("Invalid promo code is ignored gracefully")
        void invalidPromoCodeIgnored() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("BAD");
            when(promoCodeService.applyPromoCode(eq("BAD"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.invalid("Not found"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertFalse(response.hasPromoCode());
            assertEquals(BigDecimal.ZERO, response.getDiscountAmount());
        }

        @Test
        @DisplayName("Promo code discount accumulates with existing discounts")
        void promoCodeAddsToDiscountTotal() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("SAVE20");
            when(promoCodeService.applyPromoCode(eq("SAVE20"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "SAVE20", "Save 20",
                            PromoCodeService.DiscountType.PERCENTAGE,
                            new BigDecimal("20"), new BigDecimal("100.00")
                    ));
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(new DiscountService.DiscountResult(
                            "GROUP_5", "Group", DiscountService.DiscountType.GROUP,
                            new BigDecimal("10"), new BigDecimal("50.00")
                    )));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(new BigDecimal("150.00"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Promo code request passes correct parameters to service")
        void promoCodeReceivesCorrectParameters() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("CHECK");
            when(promoCodeService.applyPromoCode(eq("CHECK"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.invalid("bad"));

            service.calculatePremium(request);
            verify(promoCodeService).applyPromoCode(eq("CHECK"), eq(request.getAgreementDateFrom()), any());
        }

        @Test
        @DisplayName("Promo code set to blank is ignored")
        void blankPromoCodeIgnored() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("   ");

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            verifyNoInteractions(promoCodeService);
            assertFalse(response.hasPromoCode());
        }

        @Test
        @DisplayName("Promo code discount uses EUR before conversion")
        void promoCodeUsesEurBeforeConversion() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("SAVE");
            request.setCurrency("USD");
            when(promoCodeService.applyPromoCode(eq("SAVE"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "SAVE", "save", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("30"), new BigDecimal("30.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertTrue(response.hasPromoCode());
        }

        @Test
        @DisplayName("Promo code discount set to zero still stored")
        void promoCodeZeroDiscountStored() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("ZERO");
            when(promoCodeService.applyPromoCode(eq("ZERO"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "ZERO", "zero", PromoCodeService.DiscountType.PERCENTAGE,
                            BigDecimal.ZERO, BigDecimal.ZERO
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertTrue(response.hasPromoCode());
            assertEquals(BigDecimal.ZERO, response.getPromoCodeInfo().getActualDiscountAmount());
        }

        @Test
        @DisplayName("Promo code info stores description from service")
        void promoCodeInfoStoresDescription() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("DESC");
            when(promoCodeService.applyPromoCode(eq("DESC"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "DESC", "Description text",
                            PromoCodeService.DiscountType.PERCENTAGE,
                            new BigDecimal("12"), new BigDecimal("60.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals("Description text", response.getPromoCodeInfo().getDescription());
        }

        @Test
        @DisplayName("Promo code discount greater than premium handled")
        void promoCodeGreaterThanPremiumHandled() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("MEGA");
            when(promoCodeService.applyPromoCode(eq("MEGA"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "MEGA", "mega", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("1000"), new BigDecimal("1000.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
        }

        @Test
        @DisplayName("Promo code info contains type and value")
        void promoCodeInfoContainsTypeAndValue() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("TYPE");
            when(promoCodeService.applyPromoCode(eq("TYPE"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "TYPE", "type", PromoCodeService.DiscountType.PERCENTAGE,
                            new BigDecimal("15"), new BigDecimal("75.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals("PERCENTAGE", response.getPromoCodeInfo().getDiscountType());
            assertEquals(new BigDecimal("15"), response.getPromoCodeInfo().getDiscountValue());
        }

        @Test
        @DisplayName("Promo code discounts accumulate across multiple calls")
        void promoCodeCallsIndependent() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("ONCE");
            when(promoCodeService.applyPromoCode(eq("ONCE"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "ONCE", "once", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("25"), new BigDecimal("25.00")
                    ));

            TravelCalculatePremiumResponseV2 response1 = service.calculatePremium(request);
            TravelCalculatePremiumResponseV2 response2 = service.calculatePremium(request);

            assertEquals(new BigDecimal("25.00"), response1.getPromoCodeInfo().getActualDiscountAmount());
            assertEquals(new BigDecimal("25.00"), response2.getPromoCodeInfo().getActualDiscountAmount());
        }

        @Test
        @DisplayName("Promo code request includes original premium amount")
        void promoCodeReceivesOriginalPremium() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("ORIGINAL");
            when(promoCodeService.applyPromoCode(eq("ORIGINAL"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.invalid("bad"));

            service.calculatePremium(request);
            verify(promoCodeService).applyPromoCode(eq("ORIGINAL"), any(), eq(new BigDecimal("500.00")));
        }

        @Test
        @DisplayName("Promo code info null when service throws exception")
        void promoCodeHandledWhenServiceThrows() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("FAIL");
            when(promoCodeService.applyPromoCode(eq("FAIL"), any(), any()))
                    .thenThrow(new RuntimeException("fail"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertNull(response.getPromoCodeInfo());
        }

        @Test
        @DisplayName("Promo code discount still counted when best discount absent")
        void promoCodeCountedWithoutOtherDiscounts() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("ALONE");
            when(promoCodeService.applyPromoCode(eq("ALONE"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "ALONE", "alone", PromoCodeService.DiscountType.PERCENTAGE,
                            new BigDecimal("5"), new BigDecimal("25.00")
                    ));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(new BigDecimal("25.00"), response.getDiscountAmount());
        }
    }

    @Nested
    @DisplayName("Discount application scenarios")
    class DiscountScenarios {

        @Test
        @DisplayName("Applies best discount when available")
        void appliesBestDiscount() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "GROUP_5", "Group discount", DiscountService.DiscountType.GROUP,
                    new BigDecimal("10"), new BigDecimal("50.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(new BigDecimal("50.00"), response.getDiscountAmount());
            assertNotNull(response.getAppliedDiscounts());
        }

        @Test
        @DisplayName("Discount info includes type and percentage")
        void discountInfoIncludesDetails() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "CORPORATE", "Corporate", DiscountService.DiscountType.CORPORATE,
                    new BigDecimal("20"), new BigDecimal("100.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("CORPORATE", response.getAppliedDiscounts().get(0).getDiscountType());
            assertEquals(new BigDecimal("20"), response.getAppliedDiscounts().get(0).getPercentage());
        }

        @Test
        @DisplayName("Discount amount added to total discount sum")
        void discountAmountAddsToTotal() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "GROUP_10", "Group 10", DiscountService.DiscountType.GROUP,
                    new BigDecimal("15"), new BigDecimal("75.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(new BigDecimal("75.00"), response.getDiscountAmount());
        }

        @Test
        @DisplayName("Discount list null when best discount empty")
        void discountListNullWhenEmpty() {
            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertNull(response.getAppliedDiscounts());
        }

        @Test
        @DisplayName("Discount service invoked with request data")
        void discountServiceReceivesRequestData() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPersonsCount(7);
            request.setIsCorporate(true);

            service.calculatePremium(request);
            verify(discountService).calculateBestDiscount(eq(new BigDecimal("500.00")),
                    eq(7), eq(true), eq(request.getAgreementDateFrom()));
        }

        @Test
        @DisplayName("Discount info supports seasonal discounts")
        void discountInfoSupportsSeasonal() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "SEASON", "Season", DiscountService.DiscountType.SEASONAL,
                    new BigDecimal("5"), new BigDecimal("25.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("SEASONAL", response.getAppliedDiscounts().get(0).getDiscountType());
        }

        @Test
        @DisplayName("Discount amount cannot exceed premium")
        void discountCannotExceedPremium() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "MAX", "Max", DiscountService.DiscountType.GROUP,
                    new BigDecimal("150"), new BigDecimal("750.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
        }

        @Test
        @DisplayName("Discount info stored even when promo also applied")
        void discountStoredWithPromo() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPromoCode("STACK");
            when(promoCodeService.applyPromoCode(eq("STACK"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "STACK", "stack", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("20"), new BigDecimal("20.00")
                    ));
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "GROUP_5", "Group", DiscountService.DiscountType.GROUP,
                    new BigDecimal("10"), new BigDecimal("50.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(1, response.getAppliedDiscounts().size());
        }

        @Test
        @DisplayName("Discount info contains name")
        void discountInfoContainsName() {
            DiscountService.DiscountResult discountResult = new DiscountService.DiscountResult(
                    "LOYALTY", "Loyalty 5%", DiscountService.DiscountType.LOYALTY,
                    new BigDecimal("5"), new BigDecimal("25.00")
            );
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(discountResult));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals("Loyalty 5%", response.getAppliedDiscounts().get(0).getName());
        }

        @Test
        @DisplayName("Discount service returning exception results in safe response")
        void discountExceptionHandled() {
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenThrow(new RuntimeException("boom"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertTrue(response.hasErrors());
            assertEquals("system", response.getErrors().get(0).getField());
        }
    }

    @Nested
    @DisplayName("Combination and currency scenarios")
    class CombinationAndCurrencyScenarios {

        @Test
        @DisplayName("Currency conversion applies when requested currency differs")
        void currencyConversionApplies() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setCurrency("USD");

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals("USD", response.getCurrency());
        }

        @Test
        @DisplayName("Currency conversion called for premium, discount, final price")
        void currencyConversionCalledForAllAmounts() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setCurrency("USD");

            service.calculatePremium(request);
            verify(currencyExchangeService, times(3))
                    .convert(any(), eq("EUR"), eq("USD"));
        }

        @Test
        @DisplayName("Handles promo code and discount with currency conversion")
        void promoAndDiscountWithCurrency() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setCurrency("USD");
            request.setPromoCode("CURR");
            when(promoCodeService.applyPromoCode(eq("CURR"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "CURR", "curr", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("30"), new BigDecimal("30.00")
                    ));
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(new DiscountService.DiscountResult(
                            "GROUP_5", "Group", DiscountService.DiscountType.GROUP,
                            new BigDecimal("10"), new BigDecimal("50.00")
                    )));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals("USD", response.getCurrency());
            assertTrue(response.hasPromoCode());
            assertNotNull(response.getAppliedDiscounts());
        }

        @Test
        @DisplayName("Handles calculator exceptions by returning error response")
        void handlesCalculatorExceptions() {
            when(calculator.calculatePremiumWithDetails(any()))
                    .thenThrow(new RuntimeException("calc fail"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertTrue(response.hasErrors());
            assertEquals("system", response.getErrors().get(0).getField());
        }

        @Test
        @DisplayName("Combines promo code, discount, and currency conversion")
        void combinesPromoDiscountCurrency() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setCurrency("USD");
            request.setPromoCode("COMBO");
            when(promoCodeService.applyPromoCode(eq("COMBO"), any(), any()))
                    .thenReturn(PromoCodeService.PromoCodeResult.success(
                            "COMBO", "combo", PromoCodeService.DiscountType.FIXED_AMOUNT,
                            new BigDecimal("40"), new BigDecimal("40.00")
                    ));
            when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                    .thenReturn(Optional.of(new DiscountService.DiscountResult(
                            "CORPORATE", "Corporate", DiscountService.DiscountType.CORPORATE,
                            new BigDecimal("20"), new BigDecimal("100.00")
                    )));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertTrue(response.hasPromoCode());
            assertNotNull(response.getAppliedDiscounts());
            assertEquals("USD", response.getCurrency());
        }

        @Test
        @DisplayName("Handles zero persons count by defaulting to one")
        void handlesZeroPersons() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPersonsCount(0);
            service.calculatePremium(request);
            verify(discountService).calculateBestDiscount(any(), eq(1), anyBoolean(), any());
        }

        @Test
        @DisplayName("Handles null persons count by defaulting to one")
        void handlesNullPersons() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setPersonsCount(null);
            service.calculatePremium(request);
            verify(discountService).calculateBestDiscount(any(), eq(1), anyBoolean(), any());
        }

        @Test
        @DisplayName("Handles corporate flag null as false")
        void handlesNullCorporateFlag() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setIsCorporate(null);
            service.calculatePremium(request);
            verify(discountService).calculateBestDiscount(any(), anyInt(), eq(false), any());
        }

        @Test
        @DisplayName("Handles custom calculation result with additional risks coefficient")
        void handlesAdditionalRisksCoefficient() {
            MedicalRiskPremiumCalculator.PremiumCalculationResult result =
                    new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                            new BigDecimal("600.00"),
                            new BigDecimal("40.00"),
                            32,
                            new BigDecimal("1.10"),
                            "Adults",
                            new BigDecimal("1.20"),
                            "France",
                            new BigDecimal("0.50"),
                            new BigDecimal("1.98"),
                            7,
                            new BigDecimal("200000"),
                            List.of(new MedicalRiskPremiumCalculator.RiskPremiumDetail(
                                    "TRAVEL_MEDICAL", "Medical", new BigDecimal("600.00"), BigDecimal.ZERO
                            )),
                            List.of(new MedicalRiskPremiumCalculator.CalculationStep(
                                    "step", "formula", new BigDecimal("600.00")
                            ))
                    );
            when(calculator.calculatePremiumWithDetails(any())).thenReturn(result);

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(baseRequest());
            assertEquals(new BigDecimal("0.50"), response.getCalculation().getAdditionalRisksCoefficient());
        }

        @Test
        @DisplayName("Handles requests with custom selected risks list size")
        void handlesCustomSelectedRisksCount() {
            TravelCalculatePremiumRequestV2 request = baseRequest();
            request.setSelectedRisks(List.of("SPORT_ACTIVITIES", "TRIP_CANCELLATION", "EXTREME_SPORT"));

            TravelCalculatePremiumResponseV2 response = service.calculatePremium(request);
            assertEquals(3, response.getSelectedRisks().size());
        }
    }

    private TravelCalculatePremiumRequestV2 baseRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Smith")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 1, 1))
                .agreementDateTo(LocalDate.of(2025, 1, 6))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("LEVEL_100000")
                .selectedRisks(new ArrayList<>())
                .currency("EUR")
                .personsCount(1)
                .isCorporate(false)
                .build();
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult baseCalculationResult(BigDecimal premium) {
        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("25.00"),
                30,
                new BigDecimal("1.20"),
                "Adults",
                new BigDecimal("1.10"),
                "Spain",
                BigDecimal.ZERO,
                new BigDecimal("1.32"),
                5,
                new BigDecimal("100000"),
                List.of(new MedicalRiskPremiumCalculator.RiskPremiumDetail(
                        "TRAVEL_MEDICAL", "Medical Coverage", premium, BigDecimal.ZERO
                )),
                List.of(new MedicalRiskPremiumCalculator.CalculationStep(
                        "step", "formula", premium
                ))
        );
    }
}


