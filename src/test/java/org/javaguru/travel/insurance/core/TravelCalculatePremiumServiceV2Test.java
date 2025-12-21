package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @InjectMocks
    private TravelCalculatePremiumServiceV2 service;

    // ========= HELPERS =========

    private TravelCalculatePremiumRequestV2 validRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .agreementDateFrom(LocalDate.now())
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult baseResult(BigDecimal premium) {
        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium,
                BigDecimal.valueOf(4.5),
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ONE,
                10,
                30,
                "Spain",
                BigDecimal.valueOf(50000),
                List.of(),
                List.of()
        );
    }

    // ========= TESTS =========

    @Test
    void shouldReturnErrorsWhenValidationFails() {
        var error = new ValidationError("field", "error");
        when(validator.validate(any())).thenReturn(List.of(error));

        var response = service.calculatePremium(validRequest());

        assertNotNull(response.getErrors());
        assertEquals(1, response.getErrors().size());
        verifyNoInteractions(calculator);
    }

    @Test
    void shouldCalculatePremiumWithoutDiscounts() {
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(baseResult(new BigDecimal("50.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var response = service.calculatePremium(validRequest());

        assertEquals(new BigDecimal("50.00"), response.getAgreementPrice());
        assertEquals("EUR", response.getCurrency());
        assertNull(response.getAppliedDiscounts());
    }

    @Test
    void shouldApplyMinimumPremium() {
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(baseResult(new BigDecimal("5.00")));
        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var response = service.calculatePremium(validRequest());

        assertEquals(new BigDecimal("10.00"), response.getAgreementPrice());
    }

    @Test
    void shouldApplyPromoCodeWhenValid() {
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(baseResult(new BigDecimal("100.00")));

        when(promoCodeService.applyPromoCode(any(), any(), any()))
                .thenReturn(PromoCodeService.PromoResult.valid(
                        "PROMO10",
                        "Promo",
                        PromoCodeService.DiscountType.PERCENTAGE,
                        new BigDecimal("10"),
                        new BigDecimal("10.00")
                ));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.empty());

        var request = validRequest();
        request.setPromoCode("PROMO10");

        var response = service.calculatePremium(request);

        assertEquals(new BigDecimal("90.00"), response.getAgreementPrice());
        assertNotNull(response.getPromoCodeInfo());
    }

    @Test
    void shouldApplyAdditionalDiscount() {
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenReturn(baseResult(new BigDecimal("100.00")));

        when(discountService.calculateBestDiscount(any(), anyInt(), anyBoolean(), any()))
                .thenReturn(Optional.of(
                        new DiscountService.Discount(
                                DiscountService.DiscountType.FAMILY,
                                "Family",
                                new BigDecimal("5"),
                                new BigDecimal("5.00")
                        )
                ));

        var response = service.calculatePremium(validRequest());

        assertEquals(new BigDecimal("95.00"), response.getAgreementPrice());
        assertNotNull(response.getAppliedDiscounts());
    }

    @Test
    void shouldReturnSystemErrorWhenExceptionOccurs() {
        when(validator.validate(any())).thenReturn(List.of());
        when(calculator.calculatePremiumWithDetails(any()))
                .thenThrow(new RuntimeException("Boom"));

        var response = service.calculatePremium(validRequest());

        assertNotNull(response.getErrors());
        assertEquals("system", response.getErrors().get(0).getField());
    }
}
