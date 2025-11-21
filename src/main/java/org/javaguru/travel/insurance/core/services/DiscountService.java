package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Сервис для работы со скидками (групповые, корпоративные, сезонные)
 */
@Service
@RequiredArgsConstructor
public class DiscountService {

    private final Map<String, Discount> discounts = initDiscounts();

    /**
     * Рассчитывает все применимые скидки
     *
     * @param premiumAmount сумма премии до скидок
     * @param personsCount количество застрахованных
     * @param isCorporate признак корпоративного клиента
     * @param agreementDate дата договора
     * @return список применимых скидок
     */
    public List<DiscountResult> calculateApplicableDiscounts(
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate,
            LocalDate agreementDate) {

        List<DiscountResult> results = new ArrayList<>();

        for (Discount discount : discounts.values()) {
            if (isDiscountApplicable(discount, premiumAmount, personsCount, isCorporate, agreementDate)) {
                BigDecimal discountAmount = calculateDiscountAmount(discount, premiumAmount);
                results.add(new DiscountResult(
                        discount.code(),
                        discount.name(),
                        discount.discountType(),
                        discount.discountPercentage(),
                        discountAmount
                ));
            }
        }

        return results;
    }

    /**
     * Рассчитывает итоговую скидку (берется максимальная из применимых)
     *
     * @param premiumAmount сумма премии
     * @param personsCount количество лиц
     * @param isCorporate корпоративный клиент
     * @param agreementDate дата договора
     * @return максимальная скидка
     */
    public Optional<DiscountResult> calculateBestDiscount(
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate,
            LocalDate agreementDate) {

        List<DiscountResult> applicableDiscounts = calculateApplicableDiscounts(
                premiumAmount, personsCount, isCorporate, agreementDate
        );

        return applicableDiscounts.stream()
                .max(Comparator.comparing(DiscountResult::amount));
    }

    /**
     * Проверяет применимость скидки
     */
    private boolean isDiscountApplicable(
            Discount discount,
            BigDecimal premiumAmount,
            int personsCount,
            boolean isCorporate,
            LocalDate agreementDate) {

        // Проверка активности
        if (!discount.isActive()) {
            return false;
        }

        // Проверка периода действия
        if (agreementDate.isBefore(discount.validFrom())) {
            return false;
        }
        if (discount.validTo() != null && agreementDate.isAfter(discount.validTo())) {
            return false;
        }

        // Проверка минимальной суммы
        if (discount.minPremiumAmount() != null
                && premiumAmount.compareTo(discount.minPremiumAmount()) < 0) {
            return false;
        }

        // Проверка типа скидки
        switch (discount.discountType()) {
            case GROUP:
                // Групповая скидка требует минимального количества человек
                return discount.minPersonsCount() != null
                        && personsCount >= discount.minPersonsCount();

            case CORPORATE:
                // Корпоративная скидка только для корпоративных клиентов
                return isCorporate;

            case SEASONAL:
                // Сезонная скидка применима всегда в указанный период
                return true;

            case LOYALTY:
                // Программа лояльности - для простоты считаем всегда применимой
                // В реальности нужна проверка истории клиента
                return true;

            default:
                return false;
        }
    }

    /**
     * Рассчитывает сумму скидки
     */
    private BigDecimal calculateDiscountAmount(Discount discount, BigDecimal premiumAmount) {
        BigDecimal discountAmount = premiumAmount
                .multiply(discount.discountPercentage())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        // Скидка не может быть больше суммы премии
        if (discountAmount.compareTo(premiumAmount) > 0) {
            discountAmount = premiumAmount;
        }

        return discountAmount;
    }

    /**
     * Получает скидку по коду
     */
    public Optional<Discount> getDiscount(String code) {
        return Optional.ofNullable(discounts.get(code.toUpperCase()));
    }

    /**
     * Инициализация скидок
     * В реальном приложении данные будут из БД
     */
    private static Map<String, Discount> initDiscounts() {
        Map<String, Discount> discountMap = new HashMap<>();

        // Групповые скидки
        discountMap.put("GROUP_5", new Discount(
                "GROUP_5",
                "Group discount 5+ persons",
                DiscountType.GROUP,
                new BigDecimal("10"),
                5,
                null,
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        discountMap.put("GROUP_10", new Discount(
                "GROUP_10",
                "Group discount 10+ persons",
                DiscountType.GROUP,
                new BigDecimal("15"),
                10,
                null,
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        discountMap.put("GROUP_20", new Discount(
                "GROUP_20",
                "Group discount 20+ persons",
                DiscountType.GROUP,
                new BigDecimal("20"),
                20,
                null,
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        // Корпоративная скидка
        discountMap.put("CORPORATE", new Discount(
                "CORPORATE",
                "Corporate discount",
                DiscountType.CORPORATE,
                new BigDecimal("20"),
                null,
                new BigDecimal("100"),
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        // Сезонные скидки
        discountMap.put("WINTER_SEASON", new Discount(
                "WINTER_SEASON",
                "Winter season discount",
                DiscountType.SEASONAL,
                new BigDecimal("8"),
                null,
                null,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 2, 28),
                true
        ));

        discountMap.put("SUMMER_SEASON", new Discount(
                "SUMMER_SEASON",
                "Summer season discount",
                DiscountType.SEASONAL,
                new BigDecimal("5"),
                null,
                null,
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 31),
                true
        ));

        // Программа лояльности
        discountMap.put("LOYALTY_5", new Discount(
                "LOYALTY_5",
                "Loyalty discount 5%",
                DiscountType.LOYALTY,
                new BigDecimal("5"),
                null,
                null,
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        discountMap.put("LOYALTY_10", new Discount(
                "LOYALTY_10",
                "Loyalty discount 10%",
                DiscountType.LOYALTY,
                new BigDecimal("10"),
                null,
                null,
                LocalDate.of(2025, 1, 1),
                null,
                true
        ));

        return discountMap;
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    /**
     * Модель скидки
     */
    public record Discount(
            String code,
            String name,
            DiscountType discountType,
            BigDecimal discountPercentage,
            Integer minPersonsCount,
            BigDecimal minPremiumAmount,
            LocalDate validFrom,
            LocalDate validTo,
            boolean isActive
    ) {}

    /**
     * Тип скидки
     */
    public enum DiscountType {
        GROUP,      // Групповая скидка
        CORPORATE,  // Корпоративная скидка
        SEASONAL,   // Сезонная скидка
        LOYALTY     // Программа лояльности
    }

    /**
     * Результат применения скидки
     */
    public record DiscountResult(
            String code,
            String name,
            DiscountType discountType,
            BigDecimal percentage,
            BigDecimal amount
    ) {}
}