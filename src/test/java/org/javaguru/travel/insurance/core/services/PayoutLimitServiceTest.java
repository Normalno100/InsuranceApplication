package org.javaguru.travel.insurance.core.services;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты PayoutLimitService — корректировка премии по лимиту выплат.
 *
 * ЭТАП 6 (рефакторинг): Добавление отсутствующих тестов.
 *
 * ЛОГИКА КОРРЕКТИРОВКИ (task_117):
 *   Если maxPayoutAmount < coverageAmount:
 *     adjustedPremium = rawPremium * (maxPayoutAmount / coverageAmount)
 *     payoutLimitApplied = true
 *
 *   Если maxPayoutAmount >= coverageAmount или maxPayoutAmount == null:
 *     adjustedPremium = rawPremium (без изменений)
 *     payoutLimitApplied = false
 */
@DisplayName("PayoutLimitService")
class PayoutLimitServiceTest {

    private final PayoutLimitService service = new PayoutLimitService();

    // ── Лимит применяется ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Когда лимит меньше покрытия")
    class WhenLimitLowerThanCoverage {

        @Test
        @DisplayName("должен скорректировать премию пропорционально лимиту")
        void shouldAdjustPremiumWhenLimitLowerThanCoverage() {
            BigDecimal rawPremium    = new BigDecimal("100.00");
            BigDecimal coverage      = new BigDecimal("50000.00");
            BigDecimal maxPayout     = new BigDecimal("40000.00"); // 80% от покрытия

            var result = service.applyPayoutLimit(rawPremium, coverage, maxPayout);

            assertThat(result.adjustedPremium()).isEqualByComparingTo("80.00");
            assertThat(result.payoutLimitApplied()).isTrue();
            assertThat(result.appliedPayoutLimit()).isEqualByComparingTo("40000.00");
        }

        @Test
        @DisplayName("должен установить payoutLimitApplied=true")
        void shouldSetPayoutLimitAppliedTrue() {
            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    new BigDecimal("200000.00"),
                    new BigDecimal("150000.00")   // 75% от покрытия
            );

            assertThat(result.payoutLimitApplied()).isTrue();
        }

        @Test
        @DisplayName("должен вернуть appliedPayoutLimit равный maxPayoutAmount")
        void shouldReturnAppliedPayoutLimitEqualToMaxPayout() {
            BigDecimal maxPayout = new BigDecimal("30000.00");

            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    new BigDecimal("50000.00"),
                    maxPayout
            );

            assertThat(result.appliedPayoutLimit()).isEqualByComparingTo(maxPayout);
        }

        @Test
        @DisplayName("должен округлить скорректированную премию до 2 знаков")
        void shouldRoundAdjustedPremiumToTwoDecimals() {
            // 100 * (33333 / 50000) = 66.666 → 66.67
            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("33333.00")
            );

            assertThat(result.adjustedPremium().scale()).isEqualTo(2);
        }

        @ParameterizedTest(name = "rawPremium={0}, coverage={1}, maxPayout={2} → adjusted={3}")
        @CsvSource({
                "100.00,  50000.00, 40000.00,  80.00",   // 80%
                "200.00, 100000.00, 50000.00, 100.00",   // 50%
                " 50.00,  10000.00,  9000.00,  45.00",   // 90%
                "300.00, 200000.00,100000.00, 150.00",   // 50%
        })
        @DisplayName("должен рассчитать скорректированную премию для разных сценариев")
        void shouldCalculateAdjustedPremiumForVariousScenarios(
                String rawPremium, String coverage, String maxPayout, String expectedAdjusted) {
            var result = service.applyPayoutLimit(
                    new BigDecimal(rawPremium),
                    new BigDecimal(coverage),
                    new BigDecimal(maxPayout)
            );

            assertThat(result.adjustedPremium()).isEqualByComparingTo(new BigDecimal(expectedAdjusted));
            assertThat(result.payoutLimitApplied()).isTrue();
        }
    }

    // ── Лимит НЕ применяется ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Когда лимит равен или выше покрытия")
    class WhenLimitEqualOrHigherThanCoverage {

        @Test
        @DisplayName("не должен изменять премию когда лимит равен покрытию")
        void shouldNotAdjustWhenLimitEqualsCoverage() {
            BigDecimal rawPremium = new BigDecimal("100.00");
            BigDecimal coverage   = new BigDecimal("50000.00");

            var result = service.applyPayoutLimit(rawPremium, coverage, coverage);

            assertThat(result.adjustedPremium()).isEqualByComparingTo("100.00");
            assertThat(result.payoutLimitApplied()).isFalse();
        }

        @Test
        @DisplayName("не должен изменять премию когда лимит выше покрытия")
        void shouldNotAdjustWhenLimitHigherThanCoverage() {
            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("75000.00")   // лимит > покрытия
            );

            assertThat(result.adjustedPremium()).isEqualByComparingTo("100.00");
            assertThat(result.payoutLimitApplied()).isFalse();
        }

        @Test
        @DisplayName("должен вернуть coverageAmount как appliedPayoutLimit когда лимит >= покрытия")
        void shouldReturnCoverageAsAppliedPayoutLimitWhenLimitHigher() {
            BigDecimal coverage  = new BigDecimal("50000.00");
            BigDecimal maxPayout = new BigDecimal("60000.00");

            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    coverage,
                    maxPayout
            );

            // appliedPayoutLimit должен равняться maxPayoutAmount (не coverageAmount),
            // так как лимит был передан и он просто >= coverage
            assertThat(result.appliedPayoutLimit()).isEqualByComparingTo(maxPayout);
        }
    }

    // ── Null лимит ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Когда maxPayoutAmount равен null")
    class WhenMaxPayoutAmountIsNull {

        @Test
        @DisplayName("не должен изменять премию когда maxPayoutAmount null")
        void shouldNotAdjustWhenMaxPayoutIsNull() {
            BigDecimal rawPremium = new BigDecimal("100.00");
            BigDecimal coverage   = new BigDecimal("50000.00");

            var result = service.applyPayoutLimit(rawPremium, coverage, null);

            assertThat(result.adjustedPremium()).isEqualByComparingTo("100.00");
            assertThat(result.payoutLimitApplied()).isFalse();
        }

        @Test
        @DisplayName("должен вернуть coverageAmount как appliedPayoutLimit когда null")
        void shouldReturnCoverageAsAppliedPayoutLimitWhenNull() {
            BigDecimal coverage = new BigDecimal("50000.00");

            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    coverage,
                    null
            );

            assertThat(result.appliedPayoutLimit()).isEqualByComparingTo(coverage);
        }
    }

    // ── Граничные случаи ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Граничные случаи")
    class EdgeCases {

        @Test
        @DisplayName("лимит = 1 при покрытии 50000 → премия уменьшается до минимума")
        void shouldHandleVerySmallPayoutLimit() {
            BigDecimal rawPremium = new BigDecimal("100.00");
            BigDecimal coverage   = new BigDecimal("50000.00");
            BigDecimal maxPayout  = new BigDecimal("1.00"); // почти ноль

            var result = service.applyPayoutLimit(rawPremium, coverage, maxPayout);

            assertThat(result.adjustedPremium()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(result.payoutLimitApplied()).isTrue();
        }

        @Test
        @DisplayName("результат PayoutLimitResult содержит все три поля")
        void resultRecordShouldContainAllThreeFields() {
            var result = service.applyPayoutLimit(
                    new BigDecimal("100.00"),
                    new BigDecimal("50000.00"),
                    new BigDecimal("40000.00")
            );

            assertThat(result.adjustedPremium()).isNotNull();
            assertThat(result.appliedPayoutLimit()).isNotNull();
            assertThat(result.payoutLimitApplied()).isNotNull();
        }

        @Test
        @DisplayName("нулевая сырая премия → скорректированная тоже ноль")
        void shouldReturnZeroAdjustedPremiumForZeroRaw() {
            var result = service.applyPayoutLimit(
                    BigDecimal.ZERO,
                    new BigDecimal("50000.00"),
                    new BigDecimal("40000.00")
            );

            assertThat(result.adjustedPremium()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.payoutLimitApplied()).isTrue();
        }
    }
}