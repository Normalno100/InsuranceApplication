package org.javaguru.travel.insurance.core.services;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.PromoCodeEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.PromoCodeRepository;
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
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Тесты race condition в PromoCodeService.
 *
 * ЭТАП 6 (рефакторинг): Добавление отсутствующих тестов.
 *
 * ПОКРЫВАЕМ:
 *   1. Unit-тесты: лимит использований (max_usage_count)
 *   2. Логика findActiveByCodeForUpdate — блокирующий запрос
 *   3. Счётчик инкрементируется при успешном применении
 *   4. Многопоточный тест с CountDownLatch — симуляция race condition
 *
 * ВАЖНО: В unit-тестах мы не можем тестировать реальные DB-блокировки.
 * Тест с CountDownLatch проверяет, что сервис вызывает findActiveByCodeForUpdate
 * (блокирующий запрос), а не findActiveByCode (без блокировки).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PromoCodeService — race condition (task: ИСПРАВЛЕНИЕ 2.1)")
class PromoCodeRaceConditionTest {

    @Mock
    private PromoCodeRepository promoCodeRepository;

    @InjectMocks
    private PromoCodeService service;

    private static final String PROMO_CODE = "TEST_PROMO_10PCT";
    private static final LocalDate AGREEMENT_DATE = LocalDate.of(2026, 4, 17);
    private static final BigDecimal PREMIUM = new BigDecimal("100.00");

    // ── Использование блокирующего запроса ────────────────────────────────────

    @Nested
    @DisplayName("Использование SELECT FOR UPDATE")
    class SelectForUpdateTests {

        @Test
        @DisplayName("должен использовать findActiveByCodeForUpdate, а не findActiveByCode")
        void shouldUseFindActiveByCodeForUpdate() {
            when(promoCodeRepository.findActiveByCodeForUpdate(
                    eq(PROMO_CODE.toUpperCase()), eq(AGREEMENT_DATE)))
                    .thenReturn(Optional.of(activePromoCode(10, null, 0)));

            service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            verify(promoCodeRepository).findActiveByCodeForUpdate(
                    eq(PROMO_CODE.toUpperCase()), eq(AGREEMENT_DATE));
            verify(promoCodeRepository, never()).findActiveByCode(any(), any());
        }

        @Test
        @DisplayName("должен вызвать findActiveByCodeForUpdate с верхним регистром кода")
        void shouldUppercasePromoCodeWhenSearching() {
            String lowercaseCode = "test_promo_10pct";
            when(promoCodeRepository.findActiveByCodeForUpdate(eq("TEST_PROMO_10PCT"), any()))
                    .thenReturn(Optional.empty());

            service.applyPromoCode(lowercaseCode, AGREEMENT_DATE, PREMIUM);

            verify(promoCodeRepository).findActiveByCodeForUpdate(
                    eq("TEST_PROMO_10PCT"), any());
        }
    }

    // ── Лимит использований ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Проверка лимита использований")
    class UsageLimitTests {

        @Test
        @DisplayName("должен применить код когда счётчик < max_usage_count")
        void shouldApplyCodeWhenCountBelowLimit() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, 5, 3)));  // 3 < 5

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("должен отклонить код когда счётчик достиг max_usage_count")
        void shouldRejectCodeWhenLimitReached() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, 5, 5)));  // 5 == 5

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).contains("limit reached");
        }

        @Test
        @DisplayName("должен отклонить код когда счётчик превысил max_usage_count")
        void shouldRejectCodeWhenCountExceedsLimit() {
            // Должно быть невозможно в нормальных условиях, но защищаемся
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, 5, 6)));  // 6 > 5

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("должен применять код без ограничений когда max_usage_count = null")
        void shouldApplyCodeWithoutLimitWhenMaxUsageCountIsNull() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, null, 9999)));  // null = безлимит

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("должен отклонить последнее применение когда счётчик равен max")
        void shouldRejectAtExactLimit() {
            PromoCodeEntity entity = activePromoCode(10, 100, 100); // счётчик == max

            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(entity));

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).containsIgnoringCase("limit");
        }
    }

    // ── Инкремент счётчика ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Инкремент счётчика использований")
    class UsageCountIncrementTests {

        @Test
        @DisplayName("должен сохранить код после успешного применения (incrementUsageCount)")
        void shouldSavePromoCodeAfterSuccessfulApplication() {
            PromoCodeEntity entity = activePromoCode(10, 100, 0);
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(entity));

            service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            verify(promoCodeRepository).save(entity);
        }

        @Test
        @DisplayName("НЕ должен сохранять код если лимит достигнут")
        void shouldNotSavePromoCodeWhenLimitReached() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, 5, 5)));

            service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            verify(promoCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("НЕ должен сохранять код если он не найден")
        void shouldNotSavePromoCodeWhenNotFound() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.empty());

            service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            verify(promoCodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("счётчик должен инкрементироваться в entity перед сохранением")
        void counterShouldBeIncrementedBeforeSave() {
            PromoCodeEntity entity = activePromoCode(10, 100, 5);
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(entity));

            service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            // После применения счётчик должен стать 6
            assertThat(entity.getCurrentUsageCount()).isEqualTo(6);
        }
    }

    // ── Многопоточный тест ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Многопоточный сценарий — симуляция race condition")
    class ConcurrentTests {

        /**
         * Симулирует конкурентное применение промо-кода с max_usage_count=5.
         *
         * Так как в unit-тестах нет реальной БД и блокировок,
         * мы проверяем что счётчик корректно считает.
         *
         * Реальный race condition тест требует интеграционного контекста с БД
         * (SELECT FOR UPDATE) — это проверяется в интеграционных тестах.
         */
        @Test
        @DisplayName("однопоточно: 5 последовательных применений при max=5 → последующие отклоняются")
        void shouldRejectApplicationsAfterLimitReachedSequentially() {
            int maxUsageCount = 5;
            AtomicInteger counter = new AtomicInteger(0);

            // Каждый вызов возвращает entity с текущим счётчиком
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenAnswer(inv -> {
                        int current = counter.get();
                        if (current < maxUsageCount) {
                            PromoCodeEntity entity = activePromoCode(10, maxUsageCount, current);
                            return Optional.of(entity);
                        }
                        return Optional.of(activePromoCode(10, maxUsageCount, maxUsageCount));
                    });

            doAnswer(inv -> {
                PromoCodeEntity savedEntity = inv.getArgument(0);
                counter.set(savedEntity.getCurrentUsageCount());
                return savedEntity;
            }).when(promoCodeRepository).save(any());

            // Первые 5 применений должны быть успешными
            int successCount = 0;
            int failCount = 0;
            for (int i = 0; i < 8; i++) {
                PromoCodeService.PromoCodeResult result =
                        service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);
                if (result.isValid()) {
                    successCount++;
                } else {
                    failCount++;
                }
            }

            assertThat(successCount).isEqualTo(5);
            assertThat(failCount).isEqualTo(3);
        }

        @Test
        @DisplayName("многопоточно: вызов findActiveByCodeForUpdate происходит в каждом потоке")
        void shouldCallForUpdateInEachThread() throws InterruptedException {
            int threadCount = 10;
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);

            // Все потоки получат "лимит достигнут" — тест проверяет только что вызов идёт
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, 5, 5)));

            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            startLatch.countDown(); // Все потоки стартуют одновременно
            doneLatch.await();      // Ждём завершения всех потоков
            executor.shutdown();

            // Каждый из 10 потоков должен был вызвать findActiveByCodeForUpdate
            verify(promoCodeRepository, times(threadCount))
                    .findActiveByCodeForUpdate(any(), any());
        }
    }

    // ── Прочие сценарии ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Прочие сценарии валидации")
    class OtherValidationTests {

        @Test
        @DisplayName("должен вернуть invalid если промо-код не найден")
        void shouldReturnInvalidWhenCodeNotFound() {
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.empty());

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode("NONEXISTENT", AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
            assertThat(result.errorMessage()).containsIgnoringCase("not found");
        }

        @Test
        @DisplayName("должен вернуть invalid для null кода")
        void shouldReturnInvalidForNullCode() {
            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(null, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("должен вернуть invalid для пустого кода")
        void shouldReturnInvalidForEmptyCode() {
            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode("", AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("должен применить процентную скидку корректно")
        void shouldApplyPercentageDiscountCorrectly() {
            // 10% от 100 = 10
            when(promoCodeRepository.findActiveByCodeForUpdate(any(), any()))
                    .thenReturn(Optional.of(activePromoCode(10, null, 0)));

            PromoCodeService.PromoCodeResult result =
                    service.applyPromoCode(PROMO_CODE, AGREEMENT_DATE, PREMIUM);

            assertThat(result.isValid()).isTrue();
            assertThat(result.actualDiscountAmount()).isEqualByComparingTo(new BigDecimal("10.00"));
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private PromoCodeEntity activePromoCode(int discountPercent, Integer maxUsageCount, int currentUsageCount) {
        PromoCodeEntity entity = new PromoCodeEntity();
        entity.setId(1L);
        entity.setCode(PROMO_CODE);
        entity.setDescription("Test 10% off");
        entity.setDiscountType("PERCENTAGE");
        entity.setDiscountValue(new BigDecimal(discountPercent));
        entity.setValidFrom(LocalDate.of(2020, 1, 1));
        entity.setValidTo(LocalDate.of(2099, 12, 31));
        entity.setMaxUsageCount(maxUsageCount);
        entity.setCurrentUsageCount(currentUsageCount);
        entity.setIsActive(true);
        return entity;
    }
}