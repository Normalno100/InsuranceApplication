package org.javaguru.travel.insurance.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for Value Objects interactions
 */
@DisplayName("Value Objects Integration Tests")
class ValueObjectsIntegrationTest {

    @Test
    @DisplayName("Should calculate premium with age coefficient")
    void shouldCalculatePremiumWithAgeCoefficient() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Age age = new Age(65); // Elderly
        Coefficient ageCoefficient = Coefficient.of(1.8); // Simulated age coefficient

        // When
        Premium adjustedPremium = basePremium.multiply(ageCoefficient);

        // Then
        assertThat(adjustedPremium.amount()).isEqualByComparingTo(new BigDecimal("180.00"));
        assertThat(adjustedPremium.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("Should apply multiple coefficients sequentially")
    void shouldApplyMultipleCoefficientsSequentially() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Coefficient ageCoefficient = Coefficient.of(1.5);      // Age surcharge
        Coefficient riskCoefficient = Coefficient.of(1.2);     // Risk surcharge
        Coefficient discountCoefficient = Coefficient.of(0.9); // 10% discount

        // When
        Premium afterAge = basePremium.multiply(ageCoefficient);
        Premium afterRisk = afterAge.multiply(riskCoefficient);
        Premium finalPremium = afterRisk.multiply(discountCoefficient);

        // Then: 100 * 1.5 * 1.2 * 0.9 = 162.00
        assertThat(finalPremium.amount()).isEqualByComparingTo(new BigDecimal("162.00"));
    }

    @Test
    @DisplayName("Should combine multiple coefficients before applying to premium")
    void shouldCombineMultipleCoefficientsBeforeApplying() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Coefficient ageCoefficient = Coefficient.of(1.5);
        Coefficient riskCoefficient = Coefficient.of(1.2);
        Coefficient discountCoefficient = Coefficient.of(0.9);

        // When
        Coefficient combined = ageCoefficient
                .multiply(riskCoefficient)
                .multiply(discountCoefficient);
        Premium finalPremium = basePremium.multiply(combined);

        // Then
        assertThat(combined.value()).isEqualByComparingTo(new BigDecimal("1.6200"));
        assertThat(finalPremium.amount()).isEqualByComparingTo(new BigDecimal("162.00"));
    }

    @Test
    @DisplayName("Should calculate total premium for multiple persons")
    void shouldCalculateTotalPremiumForMultiplePersons() {
        // Given
        Premium person1Premium = Premium.of(new BigDecimal("150.00"));
        Premium person2Premium = Premium.of(new BigDecimal("200.00"));
        Premium person3Premium = Premium.of(new BigDecimal("180.00"));

        // When
        Premium totalPremium = person1Premium
                .add(person2Premium)
                .add(person3Premium);

        // Then
        assertThat(totalPremium.amount()).isEqualByComparingTo(new BigDecimal("530.00"));
    }

    @Test
    @DisplayName("Should apply family discount to total premium")
    void shouldApplyFamilyDiscountToTotalPremium() {
        // Given
        Premium totalPremium = Premium.of(new BigDecimal("500.00"));
        Coefficient familyDiscount = Coefficient.of(0.85); // 15% discount

        // When
        Premium discountedPremium = totalPremium.multiply(familyDiscount);

        // Then
        assertThat(discountedPremium.amount()).isEqualByComparingTo(new BigDecimal("425.00"));
    }

    @Test
    @DisplayName("Should calculate age-based premium with Age value object")
    void shouldCalculateAgeBasedPremiumWithAgeValueObject() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Age youngAge = new Age(25);
        Age elderlyAge = new Age(70);

        // Simulated coefficients based on age
        Coefficient youngCoefficient = Coefficient.of(0.9);  // Discount for young
        Coefficient elderlyCoefficient = Coefficient.of(2.0); // Surcharge for elderly

        // When
        Premium youngPremium = basePremium.multiply(youngCoefficient);
        Premium elderlyPremium = basePremium.multiply(elderlyCoefficient);

        // Then
        assertThat(youngPremium.amount()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(elderlyPremium.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
        assertThat(elderlyPremium.isGreaterThan(youngPremium)).isTrue();
    }

    @Test
    @DisplayName("Should verify age determines extreme sport availability")
    void shouldVerifyAgeDeterminesExtremeSportAvailability() {
        // Given
        Age youngAge = new Age(25);
        Age borderlineAge = new Age(70);
        Age elderlyAge = new Age(75);

        // Then
        assertThat(youngAge.isExtremeSportAvailable()).isTrue();
        assertThat(borderlineAge.isExtremeSportAvailable()).isTrue();
        assertThat(elderlyAge.isExtremeSportAvailable()).isFalse();
    }

    @Test
    @DisplayName("Should verify age requires manual review")
    void shouldVerifyAgeRequiresManualReview() {
        // Given
        Age normalAge = new Age(60);
        Age reviewAge = new Age(75);
        Age maxAge = new Age(80);

        // Then
        assertThat(normalAge.requiresManualReview()).isFalse();
        assertThat(reviewAge.requiresManualReview()).isTrue();
        assertThat(maxAge.requiresManualReview()).isTrue();
    }

    @Test
    @DisplayName("Should calculate premium with coefficient types")
    void shouldCalculatePremiumWithCoefficientTypes() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Coefficient increasing = Coefficient.of(1.5);  // Increases premium
        Coefficient decreasing = Coefficient.of(0.9);  // Decreases premium
        Coefficient neutral = Coefficient.ONE;          // No change

        // When & Then
        assertThat(increasing.isIncreasing()).isTrue();
        assertThat(increasing.isDecreasing()).isFalse();

        assertThat(decreasing.isIncreasing()).isFalse();
        assertThat(decreasing.isDecreasing()).isTrue();

        assertThat(neutral.isOne()).isTrue();
        assertThat(neutral.isIncreasing()).isFalse();
        assertThat(neutral.isDecreasing()).isFalse();
    }

    @Test
    @DisplayName("Should calculate percentage change for coefficients")
    void shouldCalculatePercentageChangeForCoefficients() {
        // Given
        Coefficient surcharge25 = Coefficient.of(1.25); // +25%
        Coefficient discount15 = Coefficient.of(0.85);  // -15%
        Coefficient neutral = Coefficient.ONE;           // 0%

        // When & Then
        assertThat(surcharge25.getPercentageChange())
                .isEqualByComparingTo(new BigDecimal("25.00"));
        assertThat(discount15.getPercentageChange())
                .isEqualByComparingTo(new BigDecimal("-15.00"));
        assertThat(neutral.getPercentageChange())
                .isEqualByComparingTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("Should handle complex family scenario")
    void shouldHandleComplexFamilyScenario() {
        // Given: Family trip with 2 adults and 1 child
        Premium dailyRate = Premium.of(new BigDecimal("10.00"));
        BigDecimal days = new BigDecimal("14");

        Coefficient adultCoefficient = Coefficient.of(1.0);
        Coefficient childCoefficient = Coefficient.of(0.5);
        Coefficient familyDiscount = Coefficient.of(0.85);
        Coefficient durationDiscount = Coefficient.of(0.95);

        // When
        Premium adult1 = dailyRate.multiply(days).multiply(adultCoefficient);
        Premium adult2 = dailyRate.multiply(days).multiply(adultCoefficient);
        Premium child = dailyRate.multiply(days).multiply(childCoefficient);

        Premium subtotal = adult1.add(adult2).add(child);
        Premium afterFamilyDiscount = subtotal.multiply(familyDiscount);
        Premium finalPremium = afterFamilyDiscount.multiply(durationDiscount);

        // Then: 140 + 140 + 70 = 350 * 0.85 * 0.95 = 282.625 → 282.63
        assertThat(finalPremium.amount()).isEqualByComparingTo(new BigDecimal("282.63"));
    }

    @Test
    @DisplayName("Should handle zero premium edge case")
    void shouldHandleZeroPremiumEdgeCase() {
        // Given
        Premium zeroPremium = Premium.zero(Currency.EUR);
        Coefficient coefficient = Coefficient.of(1.5);

        // When
        Premium result = zeroPremium.multiply(coefficient);

        // Then
        assertThat(result.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should handle neutral coefficient edge case")
    void shouldHandleNeutralCoefficientEdgeCase() {
        // Given
        Premium premium = Premium.of(new BigDecimal("100.00"));
        Coefficient neutral = Coefficient.ONE;

        // When
        Premium result = premium.multiply(neutral);

        // Then
        assertThat(result).isEqualTo(premium);
    }

    @Test
    @DisplayName("Should calculate surcharge amount")
    void shouldCalculateSurchargeAmount() {
        // Given
        Premium basePremium = Premium.of(new BigDecimal("100.00"));
        Coefficient surcharge = Coefficient.of(1.25); // 25% surcharge

        // When
        Premium totalPremium = basePremium.multiply(surcharge);
        Premium surchargeAmount = totalPremium.subtract(basePremium);

        // Then
        assertThat(totalPremium.amount()).isEqualByComparingTo(new BigDecimal("125.00"));
        assertThat(surchargeAmount.amount()).isEqualByComparingTo(new BigDecimal("25.00"));
    }

    @Test
    @DisplayName("Should apply minimum premium")
    void shouldApplyMinimumPremium() {
        // Given: Premium below minimum
        Premium lowPremium = Premium.of(new BigDecimal("5.00"));
        Premium normalPremium = Premium.of(new BigDecimal("50.00"));

        // When
        Premium adjustedLow = lowPremium.applyMinimum();
        Premium adjustedNormal = normalPremium.applyMinimum();

        // Then
        assertThat(adjustedLow.amount()).isEqualByComparingTo(new BigDecimal("10.00"));
        assertThat(adjustedNormal.amount()).isEqualByComparingTo(new BigDecimal("50.00"));
    }

    @Test
    @DisplayName("Should compare premiums")
    void shouldComparePremiums() {
        // Given
        Premium lower = Premium.of(new BigDecimal("100.00"));
        Premium higher = Premium.of(new BigDecimal("150.00"));

        // Then
        assertThat(lower.isLessThan(higher)).isTrue();
        assertThat(higher.isGreaterThan(lower)).isTrue();
        assertThat(lower.isLessThan(lower)).isFalse();
    }

    @Test
    @DisplayName("Should maintain immutability of value objects")
    void shouldMaintainImmutabilityOfValueObjects() {
        // Given
        Premium original = Premium.of(new BigDecimal("100.00"));
        Coefficient coefficient = Coefficient.of(1.5);

        // When
        Premium modified = original.multiply(coefficient);

        // Then: Original unchanged
        assertThat(original.amount()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(modified.amount()).isEqualByComparingTo(new BigDecimal("150.00"));
        assertThat(modified).isNotSameAs(original);
    }

    @Test
    @DisplayName("Should work with Age from birth date")
    void shouldWorkWithAgeFromBirthDate() {
        // Given
        LocalDate birthDate = LocalDate.of(1990, 1, 1);
        LocalDate referenceDate = LocalDate.of(2025, 1, 30);

        // When
        Age age = Age.fromBirthDate(birthDate, referenceDate);

        // Then
        assertThat(age.years()).isEqualTo(35);
        assertThat(age.getAgeGroupDescription()).isEqualTo("Adults");
    }

    @Test
    @DisplayName("Should categorize age groups correctly")
    void shouldCategorizeAgeGroupsCorrectly() {
        // Given & When & Then
        assertThat(new Age(3).getAgeGroupDescription()).isEqualTo("Infants and toddlers");
        assertThat(new Age(12).getAgeGroupDescription()).isEqualTo("Children and teenagers");
        assertThat(new Age(25).getAgeGroupDescription()).isEqualTo("Young adults");
        assertThat(new Age(35).getAgeGroupDescription()).isEqualTo("Adults");
        assertThat(new Age(45).getAgeGroupDescription()).isEqualTo("Middle-aged");
        assertThat(new Age(55).getAgeGroupDescription()).isEqualTo("Senior");
        assertThat(new Age(65).getAgeGroupDescription()).isEqualTo("Elderly");
        assertThat(new Age(75).getAgeGroupDescription()).isEqualTo("Very elderly");
    }

    @Test
    @DisplayName("Should add coefficients")
    void shouldAddCoefficients() {
        // Given
        Coefficient coef1 = Coefficient.of(0.5);
        Coefficient coef2 = Coefficient.of(0.3);

        // When
        Coefficient sum = coef1.add(coef2);

        // Then
        assertThat(sum.value()).isEqualByComparingTo(new BigDecimal("0.8000"));
    }

    @Test
    @DisplayName("Should handle premium subtraction")
    void shouldHandlePremiumSubtraction() {
        // Given
        Premium total = Premium.of(new BigDecimal("100.00"));
        Premium discount = Premium.of(new BigDecimal("30.00"));

        // When
        Premium result = total.subtract(discount);

        // Then
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("70.00"));
    }

    @Test
    @DisplayName("Should prevent negative premium on subtraction")
    void shouldPreventNegativePremiumOnSubtraction() {
        // Given
        Premium smaller = Premium.of(new BigDecimal("50.00"));
        Premium larger = Premium.of(new BigDecimal("100.00"));

        // When: Subtracting larger from smaller
        Premium result = smaller.subtract(larger);

        // Then: Result is zero, not negative
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(result.isZero()).isTrue();
    }

    @Test
    @DisplayName("Should reject operations with different currencies")
    void shouldRejectOperationsWithDifferentCurrencies() {
        // Given
        Premium euroPremium = new Premium(new BigDecimal("100.00"), Currency.EUR);
        Premium usdPremium = new Premium(new BigDecimal("100.00"), Currency.USD);

        // When & Then
        assertThatThrownBy(() -> euroPremium.add(usdPremium))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");

        assertThatThrownBy(() -> euroPremium.subtract(usdPremium))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");

        assertThatThrownBy(() -> euroPremium.isGreaterThan(usdPremium))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("Should support different currencies")
    void shouldSupportDifferentCurrencies() {
        // Given & When
        Premium eur = new Premium(new BigDecimal("100.00"), Currency.EUR);
        Premium usd = new Premium(new BigDecimal("100.00"), Currency.USD);
        Premium gbp = new Premium(new BigDecimal("100.00"), Currency.GBP);

        // Then
        assertThat(eur.currency()).isEqualTo(Currency.EUR);
        assertThat(usd.currency()).isEqualTo(Currency.USD);
        assertThat(gbp.currency()).isEqualTo(Currency.GBP);
    }
}