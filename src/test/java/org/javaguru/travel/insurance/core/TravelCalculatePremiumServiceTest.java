package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.core.services.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Упрощённые тесты главного сервиса V2
 * Фокус: интеграция компонентов, а не детали реализации
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumService")
class TravelCalculatePremiumServiceTest {

    @Mock
    private TravelCalculatePremiumRequestValidator validator;

    @Mock
    private MedicalRiskPremiumCalculator calculator;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private DiscountService discountService;

    @InjectMocks
    private TravelCalculatePremiumService service;

    // ========== HAPPY PATH ==========

    @Test
    @DisplayName("calculates premium without discounts")
    void shouldCalculatePremiumWithoutDiscounts() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertNotNull(response);
        assertEquals(new BigDecimal("50.00"), response.getAgreementPrice());
        assertEquals("EUR", response.getCurrency());
        assertNull(response.getAppliedDiscounts());
        assertTrue(response.getErrors() == null || response.getErrors().isEmpty());
    }

    @Test
    @DisplayName("applies minimum premium of 10 EUR")
    void shouldApplyMinimumPremium() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("5.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(new BigDecimal("10.00"), response.getAgreementPrice());
    }

    @Test
    @DisplayName("applies promo code discount")
    void shouldApplyPromoCodeDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("100.00")));

        when(promoCodeService.applyPromoCode(any(), any(), any()))
                .thenReturn(createPromoCodeResult(
                        "PROMO10",
                        "10% discount",
                        PromoCodeService.DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        new BigDecimal("10.00")
                ));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setPromoCode("PROMO10");

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(new BigDecimal("90.00"), response.getAgreementPrice());
        assertEquals(new BigDecimal("10.00"), response.getDiscountAmount());
        assertNotNull(response.getPromoCodeInfo());
        assertEquals("PROMO10", response.getPromoCodeInfo().getCode());
    }

    @Test
    @DisplayName("applies additional discount")
    void shouldApplyAdditionalDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("100.00")));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.of(
                        new DiscountService.DiscountResult(
                                "FAMILY",
                                "Family discount",
                                DiscountService.DiscountType.GROUP,
                                new BigDecimal("5"),
                                new BigDecimal("5.00")
                        )
                ));

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(new BigDecimal("95.00"), response.getAgreementPrice());
        assertEquals(new BigDecimal("5.00"), response.getDiscountAmount());
        assertNotNull(response.getAppliedDiscounts());
        assertEquals(1, response.getAppliedDiscounts().size());
    }

    @Test
    @DisplayName("combines promo code and discount")
    void shouldCombinePromoCodeAndDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("100.00")));

        when(promoCodeService.applyPromoCode(any(), any(), any()))
                .thenReturn(createPromoCodeResult(
                        "PROMO10",
                        "10% discount",
                        PromoCodeService.DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        new BigDecimal("10.00")
                ));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.of(
                        new DiscountService.DiscountResult(
                                "GROUP",
                                "Group discount",
                                DiscountService.DiscountType.GROUP,
                                new BigDecimal("5"),
                                new BigDecimal("5.00")
                        )
                ));

        var request = validRequest();
        request.setPromoCode("PROMO10");

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(new BigDecimal("85.00"), response.getAgreementPrice());
        assertEquals(new BigDecimal("15.00"), response.getDiscountAmount());
    }

    // ========== VALIDATION ERRORS ==========

    @Test
    @DisplayName("returns validation errors without calculation")
    void shouldReturnValidationErrors() {
        // Given
        var validationErrors = List.of(
                new ValidationError("personFirstName", "Must not be empty!"),
                new ValidationError("personLastName", "Must not be empty!")
        );
        when(validator.validate(any())).thenReturn(validationErrors);

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertNotNull(response.getErrors());
        assertEquals(2, response.getErrors().size());

        // Check that errors are present
        assertTrue(response.getErrors().stream()
                .anyMatch(e -> "personFirstName".equals(e.getField())));
        assertTrue(response.getErrors().stream()
                .anyMatch(e -> "personLastName".equals(e.getField())));

        // Check that calculation was not performed
        assertNull(response.getAgreementPrice());
        verify(calculator, never()).calculatePremiumWithDetails(any());
    }

    @Test
    @DisplayName("handles calculation exception gracefully")
    void shouldHandleCalculationException() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenThrow(new RuntimeException("Calculation failed"));

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());

        var systemError = response.getErrors().get(0);
        assertEquals("system", systemError.getField());
        assertTrue(systemError.getMessage().contains("Calculation error"));
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("handles zero premium after discount")
    void shouldHandleZeroPremiumAfterDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("10.00")));

        when(promoCodeService.applyPromoCode(any(), any(), any()))
                .thenReturn(createPromoCodeResult(
                        "FREE",
                        "100% discount",
                        PromoCodeService.DiscountType.PERCENTAGE,
                        new BigDecimal("100"),
                        new BigDecimal("10.00")
                ));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setPromoCode("FREE");

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(BigDecimal.ZERO, response.getAgreementPrice());
    }

    @Test
    @DisplayName("uses EUR as default currency")
    void shouldUseEurAsDefaultCurrency() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setCurrency(null); // No currency specified

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals("EUR", response.getCurrency());
    }

    @Test
    @DisplayName("uses specified currency")
    void shouldUseSpecifiedCurrency() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setCurrency("USD");

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals("USD", response.getCurrency());
    }

    @Test
    @DisplayName("defaults to 1 person when count not specified")
    void shouldDefaultToOnePerson() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setPersonsCount(null);

        // When
        service.calculatePremium(request);

        // Then
        verify(discountService).calculateBestDiscount(any(), eq(1), anyBoolean(), any());
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.of(2025, 6, 1))
                .agreementDateTo(LocalDate.of(2025, 6, 15))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult createCalculationResult(
            BigDecimal premium) {

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("4.5"),      // baseRate
                35,                          // age
                new BigDecimal("1.0"),      // ageCoefficient
                "Adults",                    // ageGroupDescription
                new BigDecimal("1.0"),      // countryCoefficient
                "Spain",                     // countryName
                BigDecimal.ZERO,            // additionalRisksCoefficient
                new BigDecimal("1.0"),      // totalCoefficient
                14,                          // days
                new BigDecimal("50000"),    // coverageAmount
                Collections.emptyList(),     // riskDetails
                Collections.emptyList()      // calculationSteps
        );
    }

    private PromoCodeService.PromoCodeResult createPromoCodeResult(
            String code,
            String description,
            PromoCodeService.DiscountType type,
            BigDecimal value,
            BigDecimal actualAmount) {

        return new PromoCodeService.PromoCodeResult(
                true,
                null,
                code,
                description,
                type,
                value,
                actualAmount
        );
    }
}