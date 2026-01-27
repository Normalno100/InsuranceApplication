package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.core.services.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.validation.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse.ResponseStatus;
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
 * Тесты для TravelCalculatePremiumService
 * Обновлено для API v2.0 с новым форматом ответа
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumService v2.0")
class TravelCalculatePremiumServiceTest {

    @Mock
    private TravelCalculatePremiumRequestValidator validator;

    @Mock
    private MedicalRiskPremiumCalculator calculator;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private DiscountService discountService;

    @Mock
    private UnderwritingService underwritingService;

    @InjectMocks
    private TravelCalculatePremiumService service;

    // ========================================
    // HAPPY PATH TESTS
    // ========================================

    @Test
    @DisplayName("calculates premium without discounts")
    void shouldCalculatePremiumWithoutDiscounts() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertNotNull(response);
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertTrue(response.isSuccessful());
        assertTrue(response.getErrors().isEmpty());

        // Проверяем pricing summary
        assertNotNull(response.getPricing());
        assertEquals(new BigDecimal("50.00"), response.getPricing().getTotalPremium());
        assertEquals("EUR", response.getPricing().getCurrency());
        assertEquals(BigDecimal.ZERO, response.getPricing().getTotalDiscount());

        // Проверяем person summary
        assertNotNull(response.getPerson());
        assertEquals("John", response.getPerson().getFirstName());
        assertEquals("Doe", response.getPerson().getLastName());
        assertEquals(35, response.getPerson().getAge());

        // Проверяем trip summary
        assertNotNull(response.getTrip());
        assertEquals(14, response.getTrip().getDays());
        assertEquals("ES", response.getTrip().getCountryCode());

        // Проверяем underwriting
        assertNotNull(response.getUnderwriting());
        assertEquals("APPROVED", response.getUnderwriting().getDecision());

        // Проверяем metadata
        assertNotNull(response.getRequestId());
        assertNotNull(response.getTimestamp());
        assertEquals("2.0", response.getApiVersion());
    }

    @Test
    @DisplayName("applies minimum premium of 10 EUR")
    void shouldApplyMinimumPremium() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("5.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("10.00"), response.getPricing().getTotalPremium());
    }

    @Test
    @DisplayName("applies promo code discount")
    void shouldApplyPromoCodeDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
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
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("90.00"), response.getPricing().getTotalPremium());
        assertEquals(new BigDecimal("10.00"), response.getPricing().getTotalDiscount());

        // Проверяем примененные скидки
        assertNotNull(response.getAppliedDiscounts());
        assertEquals(1, response.getAppliedDiscounts().size());

        var promoDiscount = response.getAppliedDiscounts().get(0);
        assertEquals("PROMO_CODE", promoDiscount.getType());
        assertEquals("PROMO10", promoDiscount.getCode());
        assertEquals(new BigDecimal("10.00"), promoDiscount.getAmount());
    }

    @Test
    @DisplayName("applies additional discount")
    void shouldApplyAdditionalDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
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
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("95.00"), response.getPricing().getTotalPremium());
        assertEquals(new BigDecimal("5.00"), response.getPricing().getTotalDiscount());

        assertNotNull(response.getAppliedDiscounts());
        assertEquals(1, response.getAppliedDiscounts().size());
        assertEquals("GROUP", response.getAppliedDiscounts().get(0).getType());
    }

    @Test
    @DisplayName("combines promo code and discount")
    void shouldCombinePromoCodeAndDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
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
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertEquals(new BigDecimal("85.00"), response.getPricing().getTotalPremium());
        assertEquals(new BigDecimal("15.00"), response.getPricing().getTotalDiscount());

        // Проверяем что применены обе скидки
        assertNotNull(response.getAppliedDiscounts());
        assertEquals(2, response.getAppliedDiscounts().size());
    }

    @Test
    @DisplayName("includes pricing details when requested")
    void shouldIncludePricingDetailsWhenRequested() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request, true);  // includeDetails = true

        // Then
        assertNotNull(response.getPricingDetails());
        assertEquals(new BigDecimal("4.5"), response.getPricingDetails().getBaseRate());
        assertEquals(BigDecimal.ONE, response.getPricingDetails().getAgeCoefficient());
    }

    @Test
    @DisplayName("excludes pricing details when not requested")
    void shouldExcludePricingDetailsWhenNotRequested() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();

        // When
        var response = service.calculatePremium(request, false);  // includeDetails = false

        // Then
        assertNull(response.getPricingDetails());
    }

    // ========================================
    // VALIDATION ERROR TESTS
    // ========================================

    @Test
    @DisplayName("returns validation errors without calculation")
    void shouldReturnValidationErrors() {
        // Given
        var validationErrors = List.of(
                org.javaguru.travel.insurance.core.validation.ValidationError.error(
                        "personFirstName", "Must not be empty!"),
                org.javaguru.travel.insurance.core.validation.ValidationError.error(
                        "personLastName", "Must not be empty!")
        );
        when(validator.validate(any())).thenReturn(validationErrors);

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(ResponseStatus.VALIDATION_ERROR, response.getStatus());
        assertFalse(response.isSuccessful());

        assertNotNull(response.getErrors());
        assertEquals(2, response.getErrors().size());

        assertTrue(response.getErrors().stream()
                .anyMatch(e -> "personFirstName".equals(e.getField())));
        assertTrue(response.getErrors().stream()
                .anyMatch(e -> "personLastName".equals(e.getField())));

        // Pricing должен быть null при ошибках валидации
        assertNull(response.getPricing());
        assertNull(response.getPerson());
        assertNull(response.getTrip());

        verify(calculator, never()).calculatePremiumWithDetails(any());
    }

    @Test
    @DisplayName("handles calculation exception gracefully")
    void shouldHandleCalculationException() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenThrow(new RuntimeException("Calculation failed"));

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(ResponseStatus.VALIDATION_ERROR, response.getStatus());
        assertFalse(response.isSuccessful());

        assertNotNull(response.getErrors());
        assertFalse(response.getErrors().isEmpty());

        var systemError = response.getErrors().get(0);
        assertEquals("system", systemError.getField());
        assertTrue(systemError.getMessage().contains("Calculation error"));
    }

    // ========================================
    // UNDERWRITING TESTS
    // ========================================

    @Test
    @DisplayName("handles declined underwriting decision")
    void shouldHandleDeclinedUnderwriting() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.declined(
                        Collections.emptyList(),
                        "Age exceeds maximum allowed"
                ));

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(ResponseStatus.DECLINED, response.getStatus());
        assertFalse(response.isSuccessful());

        assertNotNull(response.getUnderwriting());
        assertEquals("DECLINED", response.getUnderwriting().getDecision());
        assertEquals("Age exceeds maximum allowed", response.getUnderwriting().getReason());

        assertNull(response.getPricing());

        // Должна быть ошибка в errors
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).getMessage().contains("declined"));
    }

    @Test
    @DisplayName("handles manual review required decision")
    void shouldHandleManualReviewRequired() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.requiresReview(
                        Collections.emptyList(),
                        "High coverage amount for age"
                ));

        var request = validRequest();

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals(ResponseStatus.REQUIRES_REVIEW, response.getStatus());
        assertFalse(response.isSuccessful());

        assertNotNull(response.getUnderwriting());
        assertEquals("REQUIRES_MANUAL_REVIEW", response.getUnderwriting().getDecision());
        assertEquals("High coverage amount for age", response.getUnderwriting().getReason());

        assertNull(response.getPricing());

        // Должна быть информация в errors
        assertFalse(response.getErrors().isEmpty());
        assertTrue(response.getErrors().get(0).getMessage().contains("Manual review required"));
    }

    // ========================================
    // EDGE CASE TESTS
    // ========================================

    @Test
    @DisplayName("handles zero premium after discount")
    void shouldHandleZeroPremiumAfterDiscount() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
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
        assertEquals(ResponseStatus.SUCCESS, response.getStatus());
        assertEquals(BigDecimal.ZERO, response.getPricing().getTotalPremium());
    }

    @Test
    @DisplayName("uses EUR as default currency")
    void shouldUseEurAsDefaultCurrency() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setCurrency(null);

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals("EUR", response.getPricing().getCurrency());
    }

    @Test
    @DisplayName("uses specified currency")
    void shouldUseSpecifiedCurrency() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(createCalculationResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setCurrency("USD");

        // When
        var response = service.calculatePremium(request);

        // Then
        assertEquals("USD", response.getPricing().getCurrency());
    }

    @Test
    @DisplayName("defaults to 1 person when count not specified")
    void shouldDefaultToOnePerson() {
        // Given
        when(validator.validate(any())).thenReturn(Collections.emptyList());
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(Collections.emptyList()));
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

    // ========================================
    // HELPER METHODS
    // ========================================

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
                premium,                    // premium
                new BigDecimal("4.5"),       // baseRate
                35,                          // age
                BigDecimal.ONE,              // ageCoefficient
                "Adults",                    // ageGroupDescription
                BigDecimal.ONE,              // countryCoefficient
                "Spain",                     // countryName
                BigDecimal.ONE,              // durationCoefficient
                BigDecimal.ZERO,             // additionalRisksCoefficient
                BigDecimal.ONE,              // totalCoefficient
                14,                          // days
                new BigDecimal("50000"),     // coverageAmount
                Collections.emptyList(),     // riskDetails
                null,                        // bundleDiscount
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