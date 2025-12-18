package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Упрощённые тесты сервиса - проверяем только бизнес-логику
 */
@ExtendWith(MockitoExtension.class)
class TravelCalculatePremiumServiceV2Test {

    @Mock
    private TravelCalculatePremiumRequestValidatorV2Impl validator;
    @Mock
    private MedicalRiskPremiumCalculator calculator;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private DiscountService discountService;
    @Mock
    private TravelCalculatePremiumServiceV2.CurrencyExchangeService currencyService;

    @InjectMocks
    private TravelCalculatePremiumServiceV2 service;

    // ========== ОСНОВНЫЕ СЦЕНАРИИ ==========

    @Test
    void shouldReturnErrors_whenValidationFails() {
        var request = createRequest();
        when(validator.validate(any()))
                .thenReturn(List.of(new org.javaguru.travel.insurance.dto.ValidationError(
                        "personFirstName", "Must not be empty")));

        var response = service.calculatePremium(request);

        assertThat(response.hasErrors()).isTrue();
        assertThat(response.getErrors()).hasSize(1);
    }

    @Test
    void shouldCalculatePremium_whenValidRequest() {
        var request = createRequest();
        when(validator.validate(any())).thenReturn(List.of());
        mockCalculation(new BigDecimal("100.00"));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var response = service.calculatePremium(request);

        assertThat(response.hasErrors()).isFalse();
        assertThat(response.getAgreementPrice()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void shouldApplyMinimumPremium_whenCalculatedTooLow() {
        var request = createRequest();
        when(validator.validate(any())).thenReturn(List.of());
        mockCalculation(new BigDecimal("5.00"));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var response = service.calculatePremium(request);

        assertThat(response.getAgreementPrice()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldApplyPromoCode_whenValid() {
        var request = createRequest();
        request.setPromoCode("SUMMER2025");

        when(validator.validate(any())).thenReturn(List.of());
        mockCalculation(new BigDecimal("100.00"));

        var promoResult = new PromoCodeService.PromoCodeResult(
                true, null, "SUMMER2025", "Summer discount",
                PromoCodeService.DiscountType.PERCENTAGE,
                new BigDecimal("10"), new BigDecimal("10.00")
        );
        when(promoCodeService.applyPromoCode(any(), any(), any()))
                .thenReturn(promoResult);
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var response = service.calculatePremium(request);

        assertThat(response.hasPromoCode()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void shouldApplyDiscount_whenEligible() {
        var request = createRequest();
        request.setPersonsCount(10);

        when(validator.validate(any())).thenReturn(List.of());
        mockCalculation(new BigDecimal("100.00"));

        var discount = new DiscountService.DiscountResult(
                "GROUP_10", "Group discount",
                DiscountService.DiscountType.GROUP,
                new BigDecimal("15"), new BigDecimal("15.00")
        );
        when(discountService.calculateBestDiscount(any(), eq(10), anyBoolean(), any()))
                .thenReturn(Optional.of(discount));

        var response = service.calculatePremium(request);

        assertThat(response.hasDiscounts()).isTrue();
        assertThat(response.getDiscountAmount()).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    void shouldConvertCurrency_whenRequested() {
        var request = createRequest();
        request.setCurrency("USD");

        when(validator.validate(any())).thenReturn(List.of());
        mockCalculation(new BigDecimal("100.00"));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());
        when(currencyService.convert(any(), eq("EUR"), eq("USD")))
                .thenAnswer(inv -> inv.getArgument(0, BigDecimal.class)
                        .multiply(new BigDecimal("1.08")));

        var response = service.calculatePremium(request);

        assertThat(response.getCurrency()).isEqualTo("USD");
        verify(currencyService, atLeastOnce()).convert(any(), eq("EUR"), eq("USD"));
    }

    @Test
    void shouldHandleError_whenCalculationFails() {
        var request = createRequest();
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenThrow(new RuntimeException("Calculation error"));

        var response = service.calculatePremium(request);

        assertThat(response.hasErrors()).isTrue();
        assertThat(response.getErrors())
                .anyMatch(e -> e.getField().equals("system"));
    }

    // ========== HELPERS ==========

    private TravelCalculatePremiumRequestV2 createRequest() {
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

    private void mockCalculation(BigDecimal premium) {
        var result = new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                new BigDecimal("4.50"), 35,
                new BigDecimal("1.0"), "Young adults",
                new BigDecimal("1.0"), "Spain",
                BigDecimal.ZERO, new BigDecimal("1.0"),
                14, new BigDecimal("50000"),
                List.of(), List.of()
        );
        when(calculator.calculatePremiumWithDetails(any())).thenReturn(result);
    }
}