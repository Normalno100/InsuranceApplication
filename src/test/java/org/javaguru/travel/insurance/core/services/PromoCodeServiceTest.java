package org.javaguru.travel.insurance.core.services;

import org.javaguru.travel.insurance.core.services.PromoCodeService.DiscountType;
import org.javaguru.travel.insurance.core.services.PromoCodeService.PromoCode;
import org.javaguru.travel.insurance.core.services.PromoCodeService.PromoCodeResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("PromoCodeService tests")
class PromoCodeServiceTest {

    private static final LocalDate SUMMER_DAY = LocalDate.of(2025, 6, 15);
    private static final LocalDate DEFAULT_DAY = LocalDate.of(2025, 7, 1);

    private PromoCodeService service;

    @BeforeEach
    void setUp() {
        service = new PromoCodeService();
    }

    @Nested
    @DisplayName("Successful applications")
    class SuccessfulApplications {

        @Test
        @DisplayName("Applies SUMMER2025 percentage promo successfully")
        void shouldApplySummerPromoCodeSuccessfully() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("20.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Applies WELCOME50 fixed promo successfully")
        void shouldApplyWelcomeFixedPromoCodeSuccessfully() {
            PromoCodeResult result = service.applyPromoCode(
                    "WELCOME50",
                    DEFAULT_DAY,
                    new BigDecimal("400")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("50.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Applies unlimited FAMILY20 promo successfully")
        void shouldApplyFamilyPromoCodeWithoutUsageLimit() {
            PromoCodeResult result = service.applyPromoCode(
                    "FAMILY20",
                    DEFAULT_DAY,
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("40.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Accepts promo code case-insensitively")
        void shouldApplyPromoCodeIgnoringCaseInput() {
            PromoCodeResult result = service.applyPromoCode(
                    "summer2025",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
            assertEquals("SUMMER2025", result.code());
        }

        @Test
        @DisplayName("Allows promo code when premium equals minimum")
        void shouldApplyPromoCodeWhenPremiumEqualsMinAmount() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("50")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("5.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Allows promo code when minimum amount is not configured")
        void shouldApplyPromoCodeWhenMinAmountIsNull() {
            PromoCode promo = promo(
                    "NO_MIN",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("5"),
                    null,
                    null,
                    DEFAULT_DAY.minusDays(5),
                    DEFAULT_DAY.plusDays(5),
                    10,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "NO_MIN",
                    DEFAULT_DAY,
                    new BigDecimal("10")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("0.50"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Allows promo code without max discount limit")
        void shouldAllowPromoCodeWithoutMaxDiscount() {
            PromoCode promo = promo(
                    "NO_CAP",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("50"),
                    new BigDecimal("10"),
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    5,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "NO_CAP",
                    DEFAULT_DAY,
                    new BigDecimal("1000")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("500.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Caps percentage promo by configured max discount")
        void shouldCapPercentageDiscountByMaxAmount() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("10000")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("100.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Caps fixed promo so discount cannot exceed premium")
        void shouldCapFixedDiscountByPremiumAmount() {
            PromoCode promo = promo(
                    "BIG_FIXED",
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("300"),
                    new BigDecimal("50"),
                    null,
                    DEFAULT_DAY.minusDays(10),
                    DEFAULT_DAY.plusDays(10),
                    2,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "BIG_FIXED",
                    DEFAULT_DAY,
                    new BigDecimal("150")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("150.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Caps FAMILY promo by max discount amount")
        void shouldLimitFamilyPromoCodeByMaxDiscount() {
            PromoCodeResult result = service.applyPromoCode(
                    "FAMILY20",
                    DEFAULT_DAY,
                    new BigDecimal("5000")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("300.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Handles percentage discount rounding")
        void shouldHandlePercentageDiscountWithRounding() {
            PromoCode promo = promo(
                    "ROUND_PERCENT",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("7"),
                    new BigDecimal("10"),
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    3,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "ROUND_PERCENT",
                    DEFAULT_DAY,
                    new BigDecimal("123")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("8.61"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Handles zero-value fixed discount")
        void shouldHandleFixedDiscountZeroValue() {
            PromoCode promo = promo(
                    "ZERO_FIXED",
                    DiscountType.FIXED_AMOUNT,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    1,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "ZERO_FIXED",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertTrue(result.isValid());
            assertEquals(BigDecimal.ZERO.setScale(2), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Handles zero-value percentage discount")
        void shouldHandlePercentageDiscountZeroValue() {
            PromoCode promo = promo(
                    "ZERO_PERCENT",
                    DiscountType.PERCENTAGE,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    1,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "ZERO_PERCENT",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertTrue(result.isValid());
            assertEquals(BigDecimal.ZERO.setScale(2), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Allows custom promo code active in the future")
        void shouldAllowCustomPromoCodeWithFutureDates() {
            PromoCode promo = promo(
                    "FUTURE",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("15"),
                    new BigDecimal("20"),
                    null,
                    LocalDate.of(2025, 9, 1),
                    LocalDate.of(2025, 12, 31),
                    null,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "FUTURE",
                    LocalDate.of(2025, 10, 1),
                    new BigDecimal("100")
            );

            assertTrue(result.isValid());
            assertEquals(new BigDecimal("15.00"), result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Returns full details for successful application result")
        void shouldReturnAllDetailsForSuccessResult() {
            PromoCodeResult result = service.applyPromoCode(
                    "WELCOME50",
                    DEFAULT_DAY,
                    new BigDecimal("500")
            );

            assertAll(
                    () -> assertTrue(result.isValid()),
                    () -> assertNull(result.errorMessage()),
                    () -> assertEquals("WELCOME50", result.code()),
                    () -> assertEquals("Welcome bonus 50 EUR", result.description()),
                    () -> assertEquals(DiscountType.FIXED_AMOUNT, result.discountType()),
                    () -> assertEquals(new BigDecimal("50"), result.discountValue()),
                    () -> assertEquals(new BigDecimal("50.00"), result.actualDiscountAmount())
            );
        }
    }

    @Nested
    @DisplayName("Validation failures")
    class ValidationFailures {

        @Test
        @DisplayName("Rejects null promo code input")
        void shouldRejectNullPromoCodeInput() {
            PromoCodeResult result = service.applyPromoCode(
                    null,
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code is empty", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects empty promo code input")
        void shouldRejectEmptyPromoCodeInput() {
            PromoCodeResult result = service.applyPromoCode(
                    "",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code is empty", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects whitespace promo code input")
        void shouldRejectWhitespacePromoCodeInput() {
            PromoCodeResult result = service.applyPromoCode(
                    "   ",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code is empty", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects unknown promo code")
        void shouldRejectUnknownPromoCode() {
            PromoCodeResult result = service.applyPromoCode(
                    "UNKNOWN",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code not found", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects inactive promo code")
        void shouldRejectInactivePromoCode() {
            PromoCode promo = promo(
                    "INACTIVE",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    1,
                    0,
                    false
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "INACTIVE",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code is not active", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects promo code before valid-from date")
        void shouldRejectPromoCodeBeforeValidFromDate() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 5, 1),
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code is not yet valid", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects promo code after valid-to date")
        void shouldRejectPromoCodeAfterValidToDate() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 9, 1),
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code has expired", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects promo code when premium below minimum")
        void shouldRejectWhenPremiumBelowMinimum() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("10")
            );

            assertFalse(result.isValid());
            assertTrue(result.errorMessage().contains("Minimum premium amount"));
        }

        @Test
        @DisplayName("Rejects promo code when usage limit reached")
        void shouldRejectWhenUsageLimitReached() {
            PromoCode promo = promo(
                    "LIMITED",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    1,
                    1,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "LIMITED",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code usage limit reached", result.errorMessage());
        }

        @Test
        @DisplayName("Rejects promo code when usage limit is zero")
        void shouldRejectWhenUsageLimitIsZero() {
            PromoCode promo = promo(
                    "ZERO_LIMIT",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    0,
                    0,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "ZERO_LIMIT",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertEquals("Promo code usage limit reached", result.errorMessage());
        }
    }

    @Nested
    @DisplayName("Boundary and limit checks")
    class BoundaryChecks {

        @Test
        @DisplayName("Allows promo code while usage count below limit")
        void shouldAllowPromoCodeWhenUsageCountBelowLimit() {
            PromoCode promo = promo(
                    "LIMIT_OK",
                    DiscountType.PERCENTAGE,
                    new BigDecimal("10"),
                    BigDecimal.ZERO,
                    null,
                    DEFAULT_DAY.minusDays(1),
                    DEFAULT_DAY.plusDays(1),
                    5,
                    4,
                    true
            );
            registerPromo(promo);

            PromoCodeResult result = service.applyPromoCode(
                    "LIMIT_OK",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Allows promo code exactly on valid-from date")
        void shouldAllowPromoCodeOnValidFromDate() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 6, 1),
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Allows promo code exactly on valid-to date")
        void shouldAllowPromoCodeOnValidToDate() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    LocalDate.of(2025, 8, 31),
                    new BigDecimal("200")
            );

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Allows promo code when premium slightly above minimum")
        void shouldAllowPromoCodeWhenPremiumSlightlyAboveMin() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("50.01")
            );

            assertTrue(result.isValid());
        }

        @Test
        @DisplayName("Allows applying unlimited promo codes multiple times")
        void shouldAllowApplyingUnlimitedPromoCodeMultipleTimes() {
            PromoCodeResult first = service.applyPromoCode(
                    "FAMILY20",
                    DEFAULT_DAY,
                    new BigDecimal("200")
            );
            PromoCodeResult second = service.applyPromoCode(
                    "FAMILY20",
                    DEFAULT_DAY,
                    new BigDecimal("250")
            );

            assertTrue(first.isValid());
            assertTrue(second.isValid());
        }

        @Test
        @DisplayName("Contains initial built-in promo codes")
        void shouldContainInitialPromoCodes() {
            assertEquals(4, promoCodes().size());
        }

        @Test
        @DisplayName("Allows overriding existing promo code definition")
        void shouldAllowReplacingExistingPromoCodeDefinition() {
            PromoCode override = promo(
                    "SUMMER2025",
                    DiscountType.FIXED_AMOUNT,
                    new BigDecimal("30"),
                    BigDecimal.ZERO,
                    null,
                    SUMMER_DAY.minusDays(1),
                    SUMMER_DAY.plusDays(1),
                    null,
                    0,
                    true
            );
            registerPromo(override);

            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("100")
            );

            assertEquals(new BigDecimal("30.00"), result.actualDiscountAmount());
        }
    }

    @Nested
    @DisplayName("Lookup helpers")
    class LookupHelpers {

        @Test
        @DisplayName("Exists returns true for known promo code")
        void shouldReturnTrueWhenPromoCodeExists() {
            assertTrue(service.exists("SUMMER2025"));
        }

        @Test
        @DisplayName("Exists returns false for missing promo code")
        void shouldReturnFalseWhenPromoCodeMissing() {
            assertFalse(service.exists("NOPE"));
        }

        @Test
        @DisplayName("Retrieves promo code by exact code")
        void shouldGetPromoCodeByCode() {
            assertTrue(service.getPromoCode("SUMMER2025").isPresent());
        }

        @Test
        @DisplayName("Retrieves promo code ignoring case")
        void shouldGetPromoCodeIgnoringCase() {
            assertTrue(service.getPromoCode("summer2025").isPresent());
        }

        @Test
        @DisplayName("Returns empty optional for unknown promo code")
        void shouldReturnEmptyOptionalWhenPromoCodeMissing() {
            assertTrue(service.getPromoCode("missing").isEmpty());
        }
    }

    @Nested
    @DisplayName("Result integrity checks")
    class ResultIntegrityChecks {

        @Test
        @DisplayName("Provides error message for invalid promo code result")
        void shouldReturnErrorMessageWhenPromoCodeInvalid() {
            PromoCodeResult result = service.applyPromoCode(
                    "UNKNOWN",
                    DEFAULT_DAY,
                    new BigDecimal("100")
            );

            assertFalse(result.isValid());
            assertNotNull(result.errorMessage());
            assertNull(result.actualDiscountAmount());
        }

        @Test
        @DisplayName("Success result has null error message")
        void shouldReturnNullErrorForSuccessResult() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("200")
            );

            assertNull(result.errorMessage());
        }

        @Test
        @DisplayName("Actual discount keeps two decimal places")
        void shouldKeepActualDiscountScaleWithTwoDecimals() {
            PromoCodeResult result = service.applyPromoCode(
                    "SUMMER2025",
                    SUMMER_DAY,
                    new BigDecimal("333")
            );

            assertEquals(2, result.actualDiscountAmount().scale());
            assertEquals(new BigDecimal("33.30"), result.actualDiscountAmount());
        }
    }

    private void registerPromo(PromoCode promo) {
        promoCodes().put(promo.code().toUpperCase(), promo);
    }

    @SuppressWarnings("unchecked")
    private Map<String, PromoCode> promoCodes() {
        try {
            Field field = PromoCodeService.class.getDeclaredField("promoCodes");
            field.setAccessible(true);
            return (Map<String, PromoCode>) field.get(service);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    private PromoCode promo(String code,
                            DiscountType discountType,
                            BigDecimal discountValue,
                            BigDecimal minPremium,
                            BigDecimal maxDiscount,
                            LocalDate validFrom,
                            LocalDate validTo,
                            Integer maxUsage,
                            int currentUsage,
                            boolean active) {
        return new PromoCode(
                code,
                "Generated promo",
                discountType,
                discountValue,
                minPremium,
                maxDiscount,
                validFrom,
                validTo,
                maxUsage,
                currentUsage,
                active
        );
    }
}

