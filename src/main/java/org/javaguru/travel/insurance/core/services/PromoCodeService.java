package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис для работы с промо-кодами и скидками
 */
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final Map<String, PromoCode> promoCodes = initPromoCodes();

    /**
     * Валидирует и применяет промо-код
     *
     * @param code код промо-кода
     * @param agreementDate дата договора
     * @param premiumAmount сумма премии до скидки
     * @return результат применения промо-кода
     */
    public PromoCodeResult applyPromoCode(String code, LocalDate agreementDate, BigDecimal premiumAmount) {
        if (code == null || code.trim().isEmpty()) {
            return PromoCodeResult.invalid("Promo code is empty");
        }

        PromoCode promoCode = promoCodes.get(code.toUpperCase());
        if (promoCode == null) {
            return PromoCodeResult.invalid("Promo code not found");
        }

        // Валидация
        ValidationResult validation = validatePromoCode(promoCode, agreementDate, premiumAmount);
        if (!validation.isValid()) {
            return PromoCodeResult.invalid(validation.errorMessage());
        }

        // Расчет скидки
        BigDecimal discountAmount = calculateDiscount(promoCode, premiumAmount);

        return PromoCodeResult.success(
                promoCode.code(),
                promoCode.description(),
                promoCode.discountType(),
                promoCode.discountValue(),
                discountAmount
        );
    }

    /**
     * Валидирует промо-код
     */
    private ValidationResult validatePromoCode(PromoCode promoCode, LocalDate agreementDate, BigDecimal premiumAmount) {
        // Проверка активности
        if (!promoCode.isActive()) {
            return ValidationResult.invalid("Promo code is not active");
        }

        // Проверка периода действия
        if (agreementDate.isBefore(promoCode.validFrom())) {
            return ValidationResult.invalid("Promo code is not yet valid");
        }
        if (agreementDate.isAfter(promoCode.validTo())) {
            return ValidationResult.invalid("Promo code has expired");
        }

        // Проверка минимальной суммы
        if (promoCode.minPremiumAmount() != null
                && premiumAmount.compareTo(promoCode.minPremiumAmount()) < 0) {
            return ValidationResult.invalid(
                    String.format("Minimum premium amount for this promo code is %.2f",
                            promoCode.minPremiumAmount())
            );
        }

        // Проверка лимита использований
        if (promoCode.maxUsageCount() != null
                && promoCode.currentUsageCount() >= promoCode.maxUsageCount()) {
            return ValidationResult.invalid("Promo code usage limit reached");
        }

        return ValidationResult.valid();
    }

    /**
     * Рассчитывает размер скидки
     */
    private BigDecimal calculateDiscount(PromoCode promoCode, BigDecimal premiumAmount) {
        BigDecimal discount;

        if (promoCode.discountType() == DiscountType.PERCENTAGE) {
            // Процентная скидка
            discount = premiumAmount
                    .multiply(promoCode.discountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // Фиксированная сумма
            discount = promoCode.discountValue();
        }

        // Применяем максимальную скидку если указана
        if (promoCode.maxDiscountAmount() != null
                && discount.compareTo(promoCode.maxDiscountAmount()) > 0) {
            discount = promoCode.maxDiscountAmount();
        }

        // Скидка не может быть больше суммы премии
        if (discount.compareTo(premiumAmount) > 0) {
            discount = premiumAmount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Получает промо-код по коду
     */
    public Optional<PromoCode> getPromoCode(String code) {
        return Optional.ofNullable(promoCodes.get(code.toUpperCase()));
    }

    /**
     * Проверяет существование промо-кода
     */
    public boolean exists(String code) {
        return promoCodes.containsKey(code.toUpperCase());
    }

    /**
     * Инициализация тестовых промо-кодов
     * В реальном приложении данные будут из БД
     */
    private static Map<String, PromoCode> initPromoCodes() {
        Map<String, PromoCode> codes = new HashMap<>();

        codes.put("SUMMER2025", new PromoCode(
                "SUMMER2025",
                "Summer discount 10%",
                DiscountType.PERCENTAGE,
                new BigDecimal("10"),
                new BigDecimal("50"),  // минимальная сумма
                new BigDecimal("100"), // максимальная скидка
                LocalDate.of(2025, 6, 1),
                LocalDate.of(2025, 8, 31),
                1000,
                0,
                true
        ));

        codes.put("WINTER2025", new PromoCode(
                "WINTER2025",
                "Winter discount 15%",
                DiscountType.PERCENTAGE,
                new BigDecimal("15"),
                new BigDecimal("100"),
                new BigDecimal("200"),
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 2, 28),
                500,
                0,
                true
        ));

        codes.put("WELCOME50", new PromoCode(
                "WELCOME50",
                "Welcome bonus 50 EUR",
                DiscountType.FIXED_AMOUNT,
                new BigDecimal("50"),
                new BigDecimal("200"),
                null,
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                100,
                0,
                true
        ));

        codes.put("FAMILY20", new PromoCode(
                "FAMILY20",
                "Family discount 20%",
                DiscountType.PERCENTAGE,
                new BigDecimal("20"),
                new BigDecimal("150"),
                new BigDecimal("300"),
                LocalDate.of(2025, 1, 1),
                LocalDate.of(2025, 12, 31),
                null,
                0,
                true
        ));

        return codes;
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

    /**
     * Модель промо-кода
     */
    public record PromoCode(
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal minPremiumAmount,
            BigDecimal maxDiscountAmount,
            LocalDate validFrom,
            LocalDate validTo,
            Integer maxUsageCount,
            int currentUsageCount,
            boolean isActive
    ) {}

    /**
     * Тип скидки
     */
    public enum DiscountType {
        PERCENTAGE,     // Процентная скидка
        FIXED_AMOUNT    // Фиксированная сумма
    }

    /**
     * Результат применения промо-кода
     */
    public record PromoCodeResult(
            boolean isValid,
            String errorMessage,
            String code,
            String description,
            DiscountType discountType,
            BigDecimal discountValue,
            BigDecimal actualDiscountAmount
    ) {
        public static PromoCodeResult success(
                String code,
                String description,
                DiscountType discountType,
                BigDecimal discountValue,
                BigDecimal actualDiscountAmount) {
            return new PromoCodeResult(
                    true, null, code, description,
                    discountType, discountValue, actualDiscountAmount
            );
        }

        public static PromoCodeResult invalid(String errorMessage) {
            return new PromoCodeResult(
                    false, errorMessage, null, null,
                    null, null, null
            );
        }
    }

    /**
     * Результат валидации
     */
    private record ValidationResult(boolean isValid, String errorMessage) {
        public static ValidationResult valid() {
            return new ValidationResult(true, null);
        }

        public static ValidationResult invalid(String errorMessage) {
            return new ValidationResult(false, errorMessage);
        }
    }
}