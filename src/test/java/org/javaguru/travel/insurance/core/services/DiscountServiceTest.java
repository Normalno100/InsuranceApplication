package org.javaguru.travel.insurance.core.services;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.DiscountEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.DiscountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
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
 * Unit-тесты для DiscountService после рефакторинга 3.3.
 *
 * Проверяем что сервис:
 * 1. Читает скидки из DiscountRepository (не из hardcode)
 * 2. Правильно фильтрует по типу скидки
 * 3. Выбирает максимальную скидку
 * 4. Рассчитывает суммы скидок
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DiscountService — after refactoring 3.3 (DB-based)")
class DiscountServiceTest {

    @Mock
    private DiscountRepository discountRepository;

    @InjectMocks
    private DiscountService discountService;

    private static final LocalDate AGREEMENT_DATE = LocalDate.of(2025, 7, 1);
    private static final BigDecimal PREMIUM = new BigDecimal("100.00");

    // =====================================================
    // ТЕСТЫ: ЧТЕНИЕ ИЗ РЕПОЗИТОРИЯ
    // =====================================================

    @Nested
    @DisplayName("loadDiscounts() — reads from repository")
    class LoadDiscountsTests {

        @Test
        @DisplayName("should call repository with agreement date")
        void shouldCallRepositoryWithAgreementDate() {
            when(discountRepository.findAllActiveOnDate(AGREEMENT_DATE))
                    .thenReturn(List.of());

            discountService.loadDiscounts(AGREEMENT_DATE);

            verify(discountRepository).findAllActiveOnDate(AGREEMENT_DATE);
        }

        @Test
        @DisplayName("should return discounts from repository")
        void shouldReturnDiscountsFromRepository() {
            DiscountEntity entity = groupDiscount("GROUP_5", 5, new BigDecimal("10"));
            when(discountRepository.findAllActiveOnDate(AGREEMENT_DATE))
                    .thenReturn(List.of(entity));

            List<DiscountEntity> result = discountService.loadDiscounts(AGREEMENT_DATE);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCode()).isEqualTo("GROUP_5");
        }
    }

    // =====================================================
    // ТЕСТЫ: ГРУППОВЫЕ СКИДКИ
    // =====================================================

    @Nested
    @DisplayName("GROUP discounts")
    class GroupDiscountTests {

        @Test
        @DisplayName("should apply GROUP discount when personsCount >= minPersonsCount")
        void shouldApplyGroupDiscountWhenEnoughPersons() {
            DiscountEntity group5 = groupDiscount("GROUP_5", 5, new BigDecimal("10"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(group5));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 5, false, AGREEMENT_DATE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).code()).isEqualTo("GROUP_5");
            assertThat(results.get(0).amount()).isEqualByComparingTo("10.00");
        }

        @Test
        @DisplayName("should NOT apply GROUP discount when personsCount < minPersonsCount")
        void shouldNotApplyGroupDiscountWhenNotEnoughPersons() {
            DiscountEntity group5 = groupDiscount("GROUP_5", 5, new BigDecimal("10"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(group5));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 3, false, AGREEMENT_DATE);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should select better GROUP discount when multiple available")
        void shouldSelectBetterGroupDiscount() {
            DiscountEntity group5  = groupDiscount("GROUP_5",  5,  new BigDecimal("10"));
            DiscountEntity group10 = groupDiscount("GROUP_10", 10, new BigDecimal("15"));
            DiscountEntity group20 = groupDiscount("GROUP_20", 20, new BigDecimal("20"));
            when(discountRepository.findAllActiveOnDate(any()))
                    .thenReturn(List.of(group5, group10, group20));

            Optional<DiscountService.DiscountResult> best =
                    discountService.calculateBestDiscount(PREMIUM, 10, false, AGREEMENT_DATE);

            assertThat(best).isPresent();
            assertThat(best.get().code()).isEqualTo("GROUP_10");
            assertThat(best.get().percentage()).isEqualByComparingTo("15");
        }
    }

    // =====================================================
    // ТЕСТЫ: КОРПОРАТИВНЫЕ СКИДКИ
    // =====================================================

    @Nested
    @DisplayName("CORPORATE discounts")
    class CorporateDiscountTests {

        @Test
        @DisplayName("should apply CORPORATE discount when isCorporate=true")
        void shouldApplyCorporateDiscountForCorporateClient() {
            DiscountEntity corporate = corporateDiscount("CORPORATE", new BigDecimal("20"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(corporate));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 1, true, AGREEMENT_DATE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).discountType())
                    .isEqualTo(DiscountService.DiscountType.CORPORATE);
            assertThat(results.get(0).amount()).isEqualByComparingTo("20.00");
        }

        @Test
        @DisplayName("should NOT apply CORPORATE discount when isCorporate=false")
        void shouldNotApplyCorporateDiscountForNonCorporateClient() {
            DiscountEntity corporate = corporateDiscount("CORPORATE", new BigDecimal("20"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(corporate));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 1, false, AGREEMENT_DATE);

            assertThat(results).isEmpty();
        }
    }

    // =====================================================
    // ТЕСТЫ: СЕЗОННЫЕ СКИДКИ
    // =====================================================

    @Nested
    @DisplayName("SEASONAL discounts")
    class SeasonalDiscountTests {

        @Test
        @DisplayName("should apply SEASONAL discount (temporal validity checked at DB level)")
        void shouldApplySeasonalDiscount() {
            DiscountEntity seasonal = seasonalDiscount("SUMMER_SEASON", new BigDecimal("5"));
            when(discountRepository.findAllActiveOnDate(AGREEMENT_DATE))
                    .thenReturn(List.of(seasonal));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 1, false, AGREEMENT_DATE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).discountType())
                    .isEqualTo(DiscountService.DiscountType.SEASONAL);
        }
    }

    // =====================================================
    // ТЕСТЫ: ПРОГРАММА ЛОЯЛЬНОСТИ
    // =====================================================

    @Nested
    @DisplayName("LOYALTY discounts")
    class LoyaltyDiscountTests {

        @Test
        @DisplayName("should always apply LOYALTY discount")
        void shouldAlwaysApplyLoyaltyDiscount() {
            DiscountEntity loyalty = loyaltyDiscount("LOYALTY_5", new BigDecimal("5"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(loyalty));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 1, false, AGREEMENT_DATE);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).discountType())
                    .isEqualTo(DiscountService.DiscountType.LOYALTY);
        }
    }

    // =====================================================
    // ТЕСТЫ: МИНИМАЛЬНАЯ СУММА ПРЕМИИ
    // =====================================================

    @Nested
    @DisplayName("Minimum premium amount filter")
    class MinPremiumAmountTests {

        @Test
        @DisplayName("should NOT apply discount when premium < minPremiumAmount")
        void shouldNotApplyDiscountWhenPremiumTooLow() {
            DiscountEntity corporate = corporateDiscountWithMinPremium(
                    "CORPORATE", new BigDecimal("20"), new BigDecimal("200"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(corporate));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(
                            new BigDecimal("150"), 1, true, AGREEMENT_DATE);

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("should apply discount when premium >= minPremiumAmount")
        void shouldApplyDiscountWhenPremiumSufficient() {
            DiscountEntity corporate = corporateDiscountWithMinPremium(
                    "CORPORATE", new BigDecimal("20"), new BigDecimal("200"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(corporate));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(
                            new BigDecimal("300"), 1, true, AGREEMENT_DATE);

            assertThat(results).hasSize(1);
        }
    }

    // =====================================================
    // ТЕСТЫ: calculateBestDiscount
    // =====================================================

    @Nested
    @DisplayName("calculateBestDiscount()")
    class CalculateBestDiscountTests {

        @Test
        @DisplayName("should return empty when no applicable discounts")
        void shouldReturnEmptyWhenNoApplicableDiscounts() {
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of());

            Optional<DiscountService.DiscountResult> best =
                    discountService.calculateBestDiscount(PREMIUM, 1, false, AGREEMENT_DATE);

            assertThat(best).isEmpty();
        }

        @Test
        @DisplayName("should return discount with maximum amount")
        void shouldReturnDiscountWithMaximumAmount() {
            DiscountEntity loyalty5  = loyaltyDiscount("LOYALTY_5",  new BigDecimal("5"));
            DiscountEntity loyalty10 = loyaltyDiscount("LOYALTY_10", new BigDecimal("10"));
            when(discountRepository.findAllActiveOnDate(any()))
                    .thenReturn(List.of(loyalty5, loyalty10));

            Optional<DiscountService.DiscountResult> best =
                    discountService.calculateBestDiscount(PREMIUM, 1, false, AGREEMENT_DATE);

            assertThat(best).isPresent();
            assertThat(best.get().code()).isEqualTo("LOYALTY_10");
            assertThat(best.get().amount()).isEqualByComparingTo("10.00");
        }
    }

    // =====================================================
    // ТЕСТЫ: РАСЧЁТ СУММЫ СКИДКИ
    // =====================================================

    @Nested
    @DisplayName("Discount amount calculation")
    class DiscountAmountCalculationTests {

        @Test
        @DisplayName("should calculate 10% of premium correctly")
        void shouldCalculateTenPercentCorrectly() {
            DiscountEntity group = groupDiscount("GROUP_5", 5, new BigDecimal("10"));
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(group));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(
                            new BigDecimal("69.30"), 5, false, AGREEMENT_DATE);

            assertThat(results.get(0).amount()).isEqualByComparingTo("6.93");
        }

        @Test
        @DisplayName("should cap discount at premium amount")
        void shouldCapDiscountAtPremiumAmount() {
            DiscountEntity huge = groupDiscount("HUGE", 1, new BigDecimal("200")); // 200% скидка
            when(discountRepository.findAllActiveOnDate(any())).thenReturn(List.of(huge));

            List<DiscountService.DiscountResult> results =
                    discountService.calculateApplicableDiscounts(PREMIUM, 1, false, AGREEMENT_DATE);

            // Скидка не может превышать сумму премии
            assertThat(results.get(0).amount()).isEqualByComparingTo(PREMIUM);
        }
    }

    // =====================================================
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ
    // =====================================================

    private DiscountEntity groupDiscount(String code, int minPersons, BigDecimal percentage) {
        DiscountEntity e = new DiscountEntity();
        e.setCode(code);
        e.setName(code + " name");
        e.setDiscountType("GROUP");
        e.setDiscountPercentage(percentage);
        e.setMinPersonsCount(minPersons);
        e.setValidFrom(LocalDate.of(2025, 1, 1));
        e.setIsActive(true);
        return e;
    }

    private DiscountEntity corporateDiscount(String code, BigDecimal percentage) {
        DiscountEntity e = new DiscountEntity();
        e.setCode(code);
        e.setName(code + " name");
        e.setDiscountType("CORPORATE");
        e.setDiscountPercentage(percentage);
        e.setMinPersonsCount(1);
        e.setValidFrom(LocalDate.of(2025, 1, 1));
        e.setIsActive(true);
        return e;
    }

    private DiscountEntity corporateDiscountWithMinPremium(
            String code, BigDecimal percentage, BigDecimal minPremium) {
        DiscountEntity e = corporateDiscount(code, percentage);
        e.setMinPremiumAmount(minPremium);
        return e;
    }

    private DiscountEntity seasonalDiscount(String code, BigDecimal percentage) {
        DiscountEntity e = new DiscountEntity();
        e.setCode(code);
        e.setName(code + " name");
        e.setDiscountType("SEASONAL");
        e.setDiscountPercentage(percentage);
        e.setMinPersonsCount(1);
        e.setValidFrom(LocalDate.of(2025, 1, 1));
        e.setIsActive(true);
        return e;
    }

    private DiscountEntity loyaltyDiscount(String code, BigDecimal percentage) {
        DiscountEntity e = new DiscountEntity();
        e.setCode(code);
        e.setName(code + " name");
        e.setDiscountType("LOYALTY");
        e.setDiscountPercentage(percentage);
        e.setMinPersonsCount(1);
        e.setValidFrom(LocalDate.of(2025, 1, 1));
        e.setIsActive(true);
        return e;
    }
}