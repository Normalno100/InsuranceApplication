package org.javaguru.travel.insurance.core.services;

import org.javaguru.travel.insurance.core.services.DiscountService.Discount;
import org.javaguru.travel.insurance.core.services.DiscountService.DiscountResult;
import org.javaguru.travel.insurance.core.services.DiscountService.DiscountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DiscountService tests")
class DiscountServiceTest {

    private static final LocalDate BASE_DATE = LocalDate.of(2025, 3, 1);
    private static final LocalDate SUMMER_DATE = LocalDate.of(2025, 7, 1);
    private static final LocalDate WINTER_DATE = LocalDate.of(2025, 12, 20);

    private DiscountService service;

    @BeforeEach
    void setUp() {
        service = new DiscountService();
    }

    @Nested
    @DisplayName("Group discounts")
    class GroupDiscounts {

        @Test
        @DisplayName("GROUP_5 applies when persons count equals threshold")
        void group5AppliesAtThreshold() {
            List<DiscountResult> results = calculate(new BigDecimal("1000"), 5, false, BASE_DATE);

            DiscountResult discount = require(results, "GROUP_5");
            assertEquals(new BigDecimal("100.00"), discount.amount());
        }

        @Test
        @DisplayName("GROUP_5 does not apply when persons below threshold")
        void group5DoesNotApplyBelowThreshold() {
            List<DiscountResult> results = calculate(new BigDecimal("1000"), 4, false, BASE_DATE);

            assertFalse(has(results, "GROUP_5"));
        }

        @Test
        @DisplayName("GROUP_10 applies when persons count is 10")
        void group10AppliesAtTenPersons() {
            List<DiscountResult> results = calculate(new BigDecimal("800"), 10, false, BASE_DATE);

            assertTrue(has(results, "GROUP_10"));
            DiscountResult discount = require(results, "GROUP_10");
            assertEquals(new BigDecimal("120.00"), discount.amount());
        }

        @Test
        @DisplayName("GROUP_20 applies when persons count is 20")
        void group20AppliesAtTwentyPersons() {
            List<DiscountResult> results = calculate(new BigDecimal("900"), 20, false, BASE_DATE);

            DiscountResult discount = require(results, "GROUP_20");
            assertEquals(new BigDecimal("180.00"), discount.amount());
        }

        @Test
        @DisplayName("All group discounts apply when persons count exceeds 20")
        void allGroupDiscountsApplyAboveTwenty() {
            List<DiscountResult> results = calculate(new BigDecimal("500"), 25, false, BASE_DATE);

            assertTrue(has(results, "GROUP_5"));
            assertTrue(has(results, "GROUP_10"));
            assertTrue(has(results, "GROUP_20"));
        }
    }

    @Nested
    @DisplayName("Corporate discounts")
    class CorporateDiscounts {

        @Test
        @DisplayName("Corporate discount applies for corporate client with sufficient premium")
        void corporateDiscountApplies() {
            List<DiscountResult> results = calculate(new BigDecimal("1000"), 2, true, BASE_DATE);

            DiscountResult discount = require(results, "CORPORATE");
            assertEquals(new BigDecimal("200.00"), discount.amount());
        }

        @Test
        @DisplayName("Corporate discount ignored for non-corporate client")
        void corporateDiscountIgnoredForNonCorporate() {
            List<DiscountResult> results = calculate(new BigDecimal("1000"), 2, false, BASE_DATE);

            assertFalse(has(results, "CORPORATE"));
        }

        @Test
        @DisplayName("Corporate discount ignored when premium below minimum")
        void corporateDiscountRequiresMinPremium() {
            List<DiscountResult> results = calculate(new BigDecimal("80"), 2, true, BASE_DATE);

            assertFalse(has(results, "CORPORATE"));
        }
    }

    @Nested
    @DisplayName("Seasonal discounts")
    class SeasonalDiscounts {

        @Test
        @DisplayName("Summer seasonal discount applies during summer window")
        void summerSeasonalApplies() {
            List<DiscountResult> results = calculate(new BigDecimal("600"), 1, false, SUMMER_DATE);

            DiscountResult discount = require(results, "SUMMER_SEASON");
            assertEquals(new BigDecimal("30.00"), discount.amount());
        }

        @Test
        @DisplayName("Summer discount does not apply outside summer window")
        void summerSeasonalNotAppliedOutsideWindow() {
            List<DiscountResult> results = calculate(new BigDecimal("600"), 1, false, BASE_DATE);

            assertFalse(has(results, "SUMMER_SEASON"));
        }

        @Test
        @DisplayName("Winter seasonal discount applies during winter window")
        void winterSeasonalApplies() {
            List<DiscountResult> results = calculate(new BigDecimal("700"), 1, false, WINTER_DATE);

            DiscountResult discount = require(results, "WINTER_SEASON");
            assertEquals(new BigDecimal("56.00"), discount.amount());
        }

        @Test
        @DisplayName("Winter seasonal discount does not apply before season starts")
        void winterSeasonalNotAppliedBeforeSeason() {
            List<DiscountResult> results = calculate(new BigDecimal("700"), 1, false, BASE_DATE);

            assertFalse(has(results, "WINTER_SEASON"));
        }
    }

    @Nested
    @DisplayName("Discount combinations")
    class DiscountCombinations {

        @Test
        @DisplayName("Group and corporate discounts can apply together")
        void groupAndCorporateCombine() {
            List<DiscountResult> results = calculate(new BigDecimal("1500"), 12, true, BASE_DATE);

            assertTrue(has(results, "GROUP_5"));
            assertTrue(has(results, "GROUP_10"));
            assertTrue(has(results, "CORPORATE"));
        }

        @Test
        @DisplayName("Seasonal discount combines with group discounts")
        void seasonalCombinesWithGroup() {
            List<DiscountResult> results = calculate(new BigDecimal("1200"), 8, false, SUMMER_DATE);

            assertTrue(has(results, "GROUP_5"));
            assertTrue(has(results, "SUMMER_SEASON"));
        }

        @Test
        @DisplayName("All discount families can apply simultaneously")
        void allFamiliesApply() {
            List<DiscountResult> results = calculate(new BigDecimal("2000"), 22, true, SUMMER_DATE);

            assertTrue(has(results, "GROUP_5"));
            assertTrue(has(results, "GROUP_10"));
            assertTrue(has(results, "GROUP_20"));
            assertTrue(has(results, "CORPORATE"));
            assertTrue(has(results, "SUMMER_SEASON"));
        }

        @Test
        @DisplayName("Best discount returns highest monetary benefit")
        void bestDiscountPicksHighestAmount() {
            Optional<DiscountResult> best = service.calculateBestDiscount(
                    new BigDecimal("3000"),
                    15,
                    true,
                    SUMMER_DATE
            );

            assertTrue(best.isPresent());
            assertEquals("CORPORATE", best.get().code());
        }

        @Test
        @DisplayName("Best discount empty when no discounts available")
        void bestDiscountEmptyWhenNoDiscounts() {
            Optional<DiscountResult> best = service.calculateBestDiscount(
                    new BigDecimal("1000"),
                    1,
                    false,
                    LocalDate.of(2024, 12, 31)
            );

            assertTrue(best.isEmpty());
        }
    }

    @Nested
    @DisplayName("Custom discount behaviours")
    class CustomDiscountBehaviors {

        @Test
        @DisplayName("Inactive discounts are ignored")
        void inactiveDiscountIgnored() {
            registerDiscount(new Discount(
                    "INACTIVE",
                    "Inactive discount",
                    DiscountType.SEASONAL,
                    new BigDecimal("50"),
                    null,
                    null,
                    BASE_DATE.minusDays(1),
                    BASE_DATE.plusDays(1),
                    false
            ));

            List<DiscountResult> results = calculate(new BigDecimal("500"), 1, false, BASE_DATE);

            assertFalse(has(results, "INACTIVE"));
        }

        @Test
        @DisplayName("Discounts outside validity period are ignored")
        void discountOutsideValidityIgnored() {
            registerDiscount(new Discount(
                    "SHORT_TERM",
                    "Short discount",
                    DiscountType.SEASONAL,
                    new BigDecimal("5"),
                    null,
                    null,
                    BASE_DATE,
                    BASE_DATE,
                    true
            ));

            List<DiscountResult> results = calculate(new BigDecimal("400"), 1, false, BASE_DATE.plusDays(1));

            assertFalse(has(results, "SHORT_TERM"));
        }

        @Test
        @DisplayName("Min premium constraint respected for custom discount")
        void minPremiumConstraintRespected() {
            registerDiscount(new Discount(
                    "MIN_PREMIUM",
                    "Min premium discount",
                    DiscountType.SEASONAL,
                    new BigDecimal("5"),
                    null,
                    new BigDecimal("1000"),
                    BASE_DATE.minusDays(1),
                    null,
                    true
            ));

            List<DiscountResult> results = calculate(new BigDecimal("500"), 1, false, BASE_DATE);

            assertFalse(has(results, "MIN_PREMIUM"));
        }

        @Test
        @DisplayName("Discount amount capped by premium amount")
        void discountAmountCappedByPremium() {
            registerDiscount(new Discount(
                    "HUGE_PERCENT",
                    "Huge percent discount",
                    DiscountType.SEASONAL,
                    new BigDecimal("150"),
                    null,
                    null,
                    BASE_DATE.minusDays(1),
                    null,
                    true
            ));

            List<DiscountResult> results = calculate(new BigDecimal("100"), 1, false, BASE_DATE);

            DiscountResult discount = require(results, "HUGE_PERCENT");
            assertEquals(new BigDecimal("100"), discount.amount());
        }
    }

    @Nested
    @DisplayName("Utility methods")
    class UtilityMethods {

        @Test
        @DisplayName("Get discount returns value for known code")
        void getDiscountReturnsValue() {
            Optional<Discount> discount = service.getDiscount("GROUP_5");

            assertTrue(discount.isPresent());
            assertEquals("Group discount 5+ persons", discount.get().name());
        }

        @Test
        @DisplayName("Get discount works case-insensitively")
        void getDiscountCaseInsensitive() {
            assertTrue(service.getDiscount("group_10").isPresent());
        }

        @Test
        @DisplayName("Get discount returns empty for unknown code")
        void getDiscountReturnsEmptyForUnknown() {
            assertTrue(service.getDiscount("UNKNOWN").isEmpty());
        }
    }

    private List<DiscountResult> calculate(BigDecimal premium,
                                           int persons,
                                           boolean corporate,
                                           LocalDate date) {
        return service.calculateApplicableDiscounts(premium, persons, corporate, date);
    }

    private boolean has(List<DiscountResult> results, String code) {
        return results.stream().anyMatch(r -> r.code().equals(code));
    }

    private DiscountResult require(List<DiscountResult> results, String code) {
        return results.stream()
                .filter(r -> r.code().equals(code))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Discount " + code + " not present"));
    }

    private void registerDiscount(Discount discount) {
        discountMap().put(discount.code(), discount);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Discount> discountMap() {
        try {
            Field field = DiscountService.class.getDeclaredField("discounts");
            field.setAccessible(true);
            return (Map<String, Discount>) field.get(service);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
}

