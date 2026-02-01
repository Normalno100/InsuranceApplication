package org.javaguru.travel.insurance.domain.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Premium Value Object")
class PremiumTest {

    // ---------- creation ----------

    @Test
    @DisplayName("Should create premium with amount and currency")
    void shouldCreatePremium() {
        Premium premium = new Premium(new BigDecimal("25.50"), Currency.EUR);

        assertThat(premium.amount()).isEqualByComparingTo("25.50");
        assertThat(premium.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("Should create premium with default currency EUR")
    void shouldCreateWithDefaultCurrency() {
        Premium premium = Premium.of(new BigDecimal("15.00"));

        assertThat(premium.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    @DisplayName("Should create premium from string amount")
    void shouldCreateFromString() {
        Premium premium = Premium.of("19.99", Currency.USD);

        assertThat(premium.amount()).isEqualByComparingTo("19.99");
        assertThat(premium.currency()).isEqualTo(Currency.USD);
    }

    @Test
    @DisplayName("Should create zero premium")
    void shouldCreateZeroPremium() {
        Premium premium = Premium.zero(Currency.EUR);

        assertThat(premium.isZero()).isTrue();
        assertThat(premium.amount()).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("Should reject null amount")
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> new Premium(null, Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount cannot be null");
    }

    @Test
    @DisplayName("Should reject null currency")
    void shouldRejectNullCurrency() {
        assertThatThrownBy(() -> new Premium(BigDecimal.TEN, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Currency cannot be null");
    }

    @Test
    @DisplayName("Should reject negative amount")
    void shouldRejectNegativeAmount() {
        assertThatThrownBy(() -> new Premium(new BigDecimal("-1.00"), Currency.EUR))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be negative");
    }

    // ---------- arithmetic ----------

    @Test
    @DisplayName("Should add premiums with same currency")
    void shouldAddPremiums() {
        Premium p1 = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Premium p2 = new Premium(new BigDecimal("5.50"), Currency.EUR);

        Premium result = p1.add(p2);

        assertThat(result.amount()).isEqualByComparingTo("15.50");
    }

    @Test
    @DisplayName("Should reject adding premiums with different currencies")
    void shouldRejectAddWithDifferentCurrencies() {
        Premium eur = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Premium usd = new Premium(new BigDecimal("5.00"), Currency.USD);

        assertThatThrownBy(() -> eur.add(usd))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different currencies");
    }

    @Test
    @DisplayName("Should subtract premiums")
    void shouldSubtractPremiums() {
        Premium p1 = new Premium(new BigDecimal("20.00"), Currency.EUR);
        Premium p2 = new Premium(new BigDecimal("7.50"), Currency.EUR);

        Premium result = p1.subtract(p2);

        assertThat(result.amount()).isEqualByComparingTo("12.50");
    }

    @Test
    @DisplayName("Should not allow negative result when subtracting")
    void shouldReturnZeroWhenSubtractResultIsNegative() {
        Premium p1 = new Premium(new BigDecimal("5.00"), Currency.EUR);
        Premium p2 = new Premium(new BigDecimal("10.00"), Currency.EUR);

        Premium result = p1.subtract(p2);

        assertThat(result.isZero()).isTrue();
    }

    // ---------- multiplication ----------

    @Test
    @DisplayName("Should multiply premium by coefficient")
    void shouldMultiplyByCoefficient() {
        Premium premium = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Coefficient coefficient = Coefficient.of(1.5);

        Premium result = premium.multiply(coefficient);

        assertThat(result.amount()).isEqualByComparingTo("15.00");
    }

    @Test
    @DisplayName("Should multiply premium by BigDecimal")
    void shouldMultiplyByBigDecimal() {
        Premium premium = new Premium(new BigDecimal("10.00"), Currency.EUR);

        Premium result = premium.multiply(new BigDecimal("2.5"));

        assertThat(result.amount()).isEqualByComparingTo("25.00");
    }

    // ---------- minimum premium ----------

    @Test
    @DisplayName("Should apply minimum premium if amount is less than minimum")
    void shouldApplyMinimumPremium() {
        Premium premium = new Premium(new BigDecimal("5.00"), Currency.EUR);

        Premium result = premium.applyMinimum();

        assertThat(result.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("Should not change premium if amount is above minimum")
    void shouldNotChangePremiumAboveMinimum() {
        Premium premium = new Premium(new BigDecimal("20.00"), Currency.EUR);

        Premium result = premium.applyMinimum();

        assertThat(result).isSameAs(premium);
    }

    // ---------- comparison ----------

    @Test
    @DisplayName("Should compare premiums correctly")
    void shouldComparePremiums() {
        Premium smaller = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Premium larger = new Premium(new BigDecimal("20.00"), Currency.EUR);

        assertThat(larger.isGreaterThan(smaller)).isTrue();
        assertThat(smaller.isLessThan(larger)).isTrue();
    }

    @Test
    @DisplayName("Should reject comparison with different currencies")
    void shouldRejectComparisonWithDifferentCurrencies() {
        Premium eur = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Premium usd = new Premium(new BigDecimal("10.00"), Currency.USD);

        assertThatThrownBy(() -> eur.isGreaterThan(usd))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------- equality ----------

    @Test
    @DisplayName("Should be equal for same amount and currency")
    void shouldBeEqual() {
        Premium p1 = new Premium(new BigDecimal("10.0"), Currency.EUR);
        Premium p2 = new Premium(new BigDecimal("10.00"), Currency.EUR);

        assertThat(p1).isEqualTo(p2);
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    @DisplayName("Should not be equal for different currencies")
    void shouldNotBeEqualForDifferentCurrencies() {
        Premium eur = new Premium(new BigDecimal("10.00"), Currency.EUR);
        Premium usd = new Premium(new BigDecimal("10.00"), Currency.USD);

        assertThat(eur).isNotEqualTo(usd);
    }

    // ---------- toString ----------

    @Test
    @DisplayName("Should return formatted string representation")
    void shouldReturnFormattedString() {
        Premium premium = new Premium(new BigDecimal("12.5"), Currency.EUR);

        assertThat(premium.toString()).isEqualTo("12.50 EUR");
    }
}
