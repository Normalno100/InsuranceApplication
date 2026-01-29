package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.domain.entities.PromoCodeEntity;
import org.javaguru.travel.insurance.core.repositories.PromoCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Сервис для работы с промо-кодами и скидками
 **/
@Slf4j
@Service
@RequiredArgsConstructor
public class PromoCodeService {

    private final PromoCodeRepository promoCodeRepository;

    /**
     * Валидирует и применяет промо-код
     *
     * @param code код промо-кода
     * @param agreementDate дата договора
     * @param premiumAmount сумма премии до скидки
     * @return результат применения промо-кода
     */
    @Transactional
    public PromoCodeResult applyPromoCode(String code, LocalDate agreementDate, BigDecimal premiumAmount) {
        if (code == null || code.trim().isEmpty()) {
            return PromoCodeResult.invalid("Promo code is empty");
        }

        log.debug("Applying promo code: {} for date: {}, premium: {}", code, agreementDate, premiumAmount);

        // Ищем промо-код в БД
        Optional<PromoCodeEntity> promoCodeOpt = promoCodeRepository.findActiveByCode(
                code.toUpperCase(),
                agreementDate
        );

        if (promoCodeOpt.isEmpty()) {
            log.warn("Promo code not found or not active: {}", code);
            return PromoCodeResult.invalid("Promo code not found or expired");
        }

        PromoCodeEntity promoCode = promoCodeOpt.get();

        // Валидация
        ValidationResult validation = validatePromoCode(promoCode, agreementDate, premiumAmount);
        if (!validation.isValid()) {
            log.warn("Promo code validation failed: {} - {}", code, validation.errorMessage());
            return PromoCodeResult.invalid(validation.errorMessage());
        }

        // Расчет скидки
        BigDecimal discountAmount = calculateDiscount(promoCode, premiumAmount);

        // ✅ КРИТИЧЕСКИ ВАЖНО: Инкрементируем счётчик использования в БД
        promoCode.incrementUsageCount();
        promoCodeRepository.save(promoCode);

        log.info("Promo code '{}' applied successfully. Discount: {} EUR. Usage count: {}/{}",
                code, discountAmount, promoCode.getCurrentUsageCount(), promoCode.getMaxUsageCount());

        return PromoCodeResult.success(
                promoCode.getCode(),
                promoCode.getDescription(),
                DiscountType.valueOf(promoCode.getDiscountType()),
                promoCode.getDiscountValue(),
                discountAmount
        );
    }

    /**
     * Валидирует промо-код
     */
    private ValidationResult validatePromoCode(
            PromoCodeEntity promoCode,
            LocalDate agreementDate,
            BigDecimal premiumAmount) {

        // Проверка активности
        if (!promoCode.getIsActive()) {
            return ValidationResult.invalid("Promo code is not active");
        }

        // Проверка периода действия
        if (agreementDate.isBefore(promoCode.getValidFrom())) {
            return ValidationResult.invalid("Promo code is not yet valid");
        }
        if (agreementDate.isAfter(promoCode.getValidTo())) {
            return ValidationResult.invalid("Promo code has expired");
        }

        // Проверка минимальной суммы
        if (promoCode.getMinPremiumAmount() != null
                && premiumAmount.compareTo(promoCode.getMinPremiumAmount()) < 0) {
            return ValidationResult.invalid(
                    String.format("Minimum premium amount for this promo code is %.2f EUR",
                            promoCode.getMinPremiumAmount())
            );
        }

        // Проверка лимита использований
        if (promoCode.getMaxUsageCount() != null
                && promoCode.getCurrentUsageCount() >= promoCode.getMaxUsageCount()) {
            return ValidationResult.invalid("Promo code usage limit reached");
        }

        return ValidationResult.valid();
    }

    /**
     * Рассчитывает размер скидки
     */
    private BigDecimal calculateDiscount(PromoCodeEntity promoCode, BigDecimal premiumAmount) {
        BigDecimal discount;

        if ("PERCENTAGE".equals(promoCode.getDiscountType())) {
            // Процентная скидка
            discount = premiumAmount
                    .multiply(promoCode.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        } else {
            // Фиксированная сумма
            discount = promoCode.getDiscountValue();
        }

        // Применяем максимальную скидку если указана
        if (promoCode.getMaxDiscountAmount() != null
                && discount.compareTo(promoCode.getMaxDiscountAmount()) > 0) {
            discount = promoCode.getMaxDiscountAmount();
        }

        // Скидка не может быть больше суммы премии
        if (discount.compareTo(premiumAmount) > 0) {
            discount = premiumAmount;
        }

        return discount.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Получает промо-код по коду (для просмотра, без применения)
     */
    public Optional<PromoCodeEntity> getPromoCode(String code) {
        return promoCodeRepository.findActiveByCode(code.toUpperCase());
    }

    /**
     * Проверяет существование промо-кода
     */
    public boolean exists(String code) {
        return promoCodeRepository.existsByCode(code.toUpperCase());
    }

    // ========== ВЛОЖЕННЫЕ КЛАССЫ ==========

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