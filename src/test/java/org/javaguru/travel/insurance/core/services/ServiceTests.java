package org.javaguru.travel.insurance.core.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Упрощённые тесты сервисов скидок и промо-кодов
 * Фокус: бизнес-правила, а не граничные случаи
 */
class ServiceTests {

    // ========== PROMO CODE SERVICE ==========

    @Nested
    @DisplayName("PromoCodeService")
    class PromoCodeServiceTest {

        private PromoCodeService service;
        private static final LocalDate VALID_DATE = LocalDate.of(2025, 7, 1);

        @BeforeEach
        void setUp() {
            service = new PromoCodeService();
        }

        @Test
        @DisplayName("applies percentage discount correctly")
        void shouldApplyPercentageDiscount() {
            var result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 6, 15),
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("20.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("applies fixed amount discount")
        void shouldApplyFixedDiscount() {
            var result = service.applyPromoCode(
                    "WELCOME50",
                    VALID_DATE,
                    new BigDecimal("400")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("50.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("caps discount at max amount")
        void shouldCapDiscountAtMaxAmount() {
            var result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 6, 15),
                    new BigDecimal("10000")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("100.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("rejects invalid codes")
        void shouldRejectInvalidCode() {
            var result = service.applyPromoCode(
                    "INVALID",
                    VALID_DATE,
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertNotNull(result.errorMessage());
        }

        @Test
        @DisplayName("rejects expired codes")
        void shouldRejectExpiredCode() {
            var result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 9, 1),
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
        }

        @Test
        @DisplayName("rejects when premium below minimum")
        void shouldRejectWhenPremiumBelowMinimum() {
            var result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 6, 15),
                    new BigDecimal("10")
            );

            assertFalse(result.isValid());
        }
    }

    // ========== DISCOUNT SERVICE ==========

    @Nested
    @DisplayName("DiscountService")
    class DiscountServiceTest {

        private DiscountService service;
        private static final LocalDate BASE_DATE = LocalDate.of(2025, 3, 1);

        @BeforeEach
        void setUp() {
            service = new DiscountService();
        }

        @Test
        @DisplayName("applies group discount at threshold")
        void shouldApplyGroupDiscount() {
            var discounts = service.calculateApplicableDiscounts(
                    new BigDecimal("1000"),
                    5,
                    false,
                    BASE_DATE
            );

            assertTrue(discounts.stream().anyMatch(d -> d.code().equals("GROUP_5")));

            var groupDiscount = discounts.stream()
                    .filter(d -> d.code().equals("GROUP_5"))
                    .findFirst()
                    .orElseThrow();

            assertEquals(new BigDecimal("100.00"), groupDiscount.amount());
        }

        @Test
        @DisplayName("applies corporate discount")
        void shouldApplyCorporateDiscount() {
            var discounts = service.calculateApplicableDiscounts(
                    new BigDecimal("1000"),
                    2,
                    true,
                    BASE_DATE
            );

            assertTrue(discounts.stream().anyMatch(d -> d.code().equals("CORPORATE")));
        }

        @Test
        @DisplayName("ignores corporate discount for non-corporate")
        void shouldIgnoreCorporateForNonCorporate() {
            var discounts = service.calculateApplicableDiscounts(
                    new BigDecimal("1000"),
                    2,
                    false,
                    BASE_DATE
            );

            assertFalse(discounts.stream().anyMatch(d -> d.code().equals("CORPORATE")));
        }

        @Test
        @DisplayName("returns best discount when multiple applicable")
        void shouldReturnBestDiscount() {
            var best = service.calculateBestDiscount(
                    new BigDecimal("3000"),
                    15,
                    true,
                    LocalDate.of(2025, 7, 1)
            );

            assertTrue(best.isPresent());
            // Corporate 20% of 3000 = 600 is best
            assertEquals("CORPORATE", best.get().code());
        }

        @Test
        @DisplayName("returns empty when no discounts applicable")
        void shouldReturnEmptyWhenNoDiscounts() {
            var best = service.calculateBestDiscount(
                    new BigDecimal("1000"),
                    1,
                    false,
                    LocalDate.of(2024, 12, 31)
            );

            assertTrue(best.isEmpty());
        }
    }
}