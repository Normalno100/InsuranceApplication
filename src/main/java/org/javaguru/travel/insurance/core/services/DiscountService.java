package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.DiscountEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.DiscountRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Сервис для работы со скидками (групповые, корпоративные, сезонные, лояльность).
 *
 * РЕФАКТОРИНГ (п. 3.3 плана):
 *   ДО:  скидки были захардкожены в Java-коде (initDiscounts()),
 *        изменение условий скидок требовало редеплоя приложения.
 *
 *   ПОСЛЕ: скидки читаются из таблицы discounts в БД.
 *     - Результат кешируется через @Cacheable ("discounts") — скидки меняются редко.
 *     - Изменение скидок в БД применяется после очистки кеша или перезапуска.
 *     - Логика применимости (isDiscountApplicable) и расчёта суммы — без изменений.
 *
 * КЕШИРОВАНИЕ:
 *   Кеш ключ включает дату, чтобы корректно обрабатывать temporal validity.
 *   При активации новых скидок (новая valid_from) или истечении старых
 *   кеш обновится на следующий день автоматически.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final DiscountRepository discountRepository;

    /**
     * Рассчитывает все применимые скидки.
     *
     * @param premiumAmount сумма премии до скидок
     * @param personsCount  количество застрахованных
     * @param isCorporate   признак корпоративного клиента
     * @param agreementDate дата договора
     * @return список применимых скидок с рассчитанными суммами
     */
    public List<DiscountResult> calculateApplicableDiscounts(
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate,
            LocalDate agreementDate) {

        List<DiscountEntity> allDiscounts = loadDiscounts(agreementDate);
        List<DiscountResult> results = new ArrayList<>();

        for (DiscountEntity discount : allDiscounts) {
            if (isDiscountApplicable(discount, premiumAmount, personsCount, isCorporate)) {
                BigDecimal discountAmount = calculateDiscountAmount(discount, premiumAmount);
                results.add(new DiscountResult(
                        discount.getCode(),
                        discount.getName(),
                        DiscountType.valueOf(discount.getDiscountType()),
                        discount.getDiscountPercentage(),
                        discountAmount
                ));
            }
        }

        log.debug("Found {} applicable discounts for premiumAmount={}, persons={}, corporate={}",
                results.size(), premiumAmount, personsCount, isCorporate);

        return results;
    }

    /**
     * Рассчитывает итоговую скидку — выбирает максимальную из применимых.
     *
     * @param premiumAmount сумма премии
     * @param personsCount  количество лиц
     * @param isCorporate   корпоративный клиент
     * @param agreementDate дата договора
     * @return максимальная применимая скидка
     */
    public Optional<DiscountResult> calculateBestDiscount(
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate,
            LocalDate agreementDate) {

        return calculateApplicableDiscounts(premiumAmount, personsCount, isCorporate, agreementDate)
                .stream()
                .max(Comparator.comparing(DiscountResult::amount));
    }

    /**
     * Получает скидку по коду.
     */
    public Optional<DiscountEntity> getDiscount(String code) {
        return discountRepository.findByCode(code.toUpperCase());
    }

    // =====================================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // =====================================================

    /**
     * Загружает активные скидки из БД с кешированием.
     *
     * Кеш ключ = дата договора, чтобы temporal validity работала корректно.
     * Скидки меняются редко, поэтому кеширование по дате безопасно.
     *
     * @param date дата, на которую нужны скидки
     * @return список активных скидок из БД
     */
    @Cacheable(value = "discounts", key = "#date")
    public List<DiscountEntity> loadDiscounts(LocalDate date) {
        List<DiscountEntity> discounts = discountRepository.findAllActiveOnDate(date);
        log.debug("Loaded {} active discounts from DB for date {}", discounts.size(), date);
        return discounts;
    }

    /**
     * Проверяет применимость скидки.
     */
    private boolean isDiscountApplicable(
            DiscountEntity discount,
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate) {

        // Проверка минимальной суммы премии
        if (discount.getMinPremiumAmount() != null
                && premiumAmount.compareTo(discount.getMinPremiumAmount()) < 0) {
            return false;
        }

        // Проверка типа скидки
        DiscountType type;
        try {
            type = DiscountType.valueOf(discount.getDiscountType());
        } catch (IllegalArgumentException e) {
            log.warn("Unknown discount type '{}' for discount '{}', skipping",
                    discount.getDiscountType(), discount.getCode());
            return false;
        }

        return switch (type) {
            case GROUP ->
                // Групповая скидка: нужно минимальное количество человек
                    discount.getMinPersonsCount() != null
                            && personsCount >= discount.getMinPersonsCount();

            case CORPORATE ->
                // Корпоративная скидка: только для корпоративных клиентов
                    isCorporate;

            case SEASONAL ->
                // Сезонная скидка применима всегда (период уже учтён в temporal validity)
                    true;

            case LOYALTY ->
                // Программа лояльности всегда применима
                // В будущем здесь может быть проверка истории клиента
                    true;
        };
    }

    /**
     * Рассчитывает сумму скидки.
     */
    private BigDecimal calculateDiscountAmount(DiscountEntity discount, BigDecimal premiumAmount) {
        BigDecimal discountAmount = premiumAmount
                .multiply(discount.getDiscountPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Скидка не может быть больше суммы премии
        if (discountAmount.compareTo(premiumAmount) > 0) {
            discountAmount = premiumAmount;
        }

        return discountAmount;
    }

    // =====================================================
    // ВЛОЖЕННЫЕ КЛАССЫ
    // =====================================================

    /**
     * Тип скидки.
     */
    public enum DiscountType {
        GROUP,      // Групповая скидка
        CORPORATE,  // Корпоративная скидка
        SEASONAL,   // Сезонная скидка
        LOYALTY     // Программа лояльности
    }

    /**
     * Результат применения скидки.
     */
    public record DiscountResult(
            String code,
            String name,
            DiscountType discountType,
            BigDecimal percentage,
            BigDecimal amount
    ) {}
}