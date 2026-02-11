package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис применения скидок
 *
 * ЦЕЛЬ: Централизованное управление всеми типами скидок
 *
 * ОБЯЗАННОСТИ:
 * 1. Применение промо-кодов
 * 2. Применение групповых/корпоративных скидок
 * 3. Выбор наилучшей скидки
 * 4. Расчет итоговой суммы скидки
 *
 * МЕТРИКИ:
 * - Complexity: 4
 * - LOC: ~120
 * - Single Responsibility: только скидки
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DiscountApplicationService {

    private final PromoCodeService promoCodeService;
    private final DiscountService discountService;

    /**
     * Применяет все доступные скидки и возвращает результат
     */
    public DiscountApplicationResult applyDiscounts(
            TravelCalculatePremiumRequest request,
            BigDecimal basePremium) {

        log.debug("Applying discounts to base premium: {}", basePremium);

        List<AppliedDiscount> appliedDiscounts = new ArrayList<>();
        BigDecimal totalDiscount = BigDecimal.ZERO;

        // 1. Промо-код
        if (hasPromoCode(request)) {
            var promoResult = applyPromoCode(request, basePremium);
            if (promoResult != null) {
                appliedDiscounts.add(promoResult);
                totalDiscount = totalDiscount.add(promoResult.amount());
                log.info("Promo code applied: {} - {} EUR",
                        promoResult.code(), promoResult.amount());
            }
        }

        // 2. Другие скидки (групповые, корпоративные)
        var otherDiscount = applyOtherDiscounts(request, basePremium);
        if (otherDiscount != null) {
            appliedDiscounts.add(otherDiscount);
            totalDiscount = totalDiscount.add(otherDiscount.amount());
            log.info("Discount applied: {} - {} EUR",
                    otherDiscount.code(), otherDiscount.amount());
        }

        BigDecimal finalPremium = basePremium.subtract(totalDiscount);

        log.info("Total discount: {} EUR, Final premium: {} EUR",
                totalDiscount, finalPremium);

        return new DiscountApplicationResult(
                basePremium,
                totalDiscount,
                finalPremium,
                appliedDiscounts
        );
    }

    /**
     * Проверяет наличие промо-кода
     */
    private boolean hasPromoCode(TravelCalculatePremiumRequest request) {
        return request.getPromoCode() != null &&
                !request.getPromoCode().trim().isEmpty();
    }

    /**
     * Применяет промо-код
     */
    private AppliedDiscount applyPromoCode(
            TravelCalculatePremiumRequest request,
            BigDecimal basePremium) {

        var promoResult = promoCodeService.applyPromoCode(
                request.getPromoCode(),
                request.getAgreementDateFrom(),
                basePremium
        );

        if (!promoResult.isValid()) {
            log.warn("Promo code validation failed: {}", promoResult.errorMessage());
            return null;
        }

        return new AppliedDiscount(
                "PROMO_CODE",
                promoResult.code(),
                promoResult.description(),
                promoResult.actualDiscountAmount(),
                promoResult.discountValue()
        );
    }

    /**
     * Применяет другие скидки (выбирает лучшую)
     */
    private AppliedDiscount applyOtherDiscounts(
            TravelCalculatePremiumRequest request,
            BigDecimal basePremium) {

        int personsCount = request.getPersonsCount() != null && request.getPersonsCount() > 0
                ? request.getPersonsCount() : 1;
        boolean isCorporate = Boolean.TRUE.equals(request.getIsCorporate());

        var bestDiscountOpt = discountService.calculateBestDiscount(
                basePremium,
                personsCount,
                isCorporate,
                request.getAgreementDateFrom()
        );

        if (bestDiscountOpt.isEmpty()) {
            return null;
        }

        var discount = bestDiscountOpt.get();
        return new AppliedDiscount(
                discount.discountType().name(),
                discount.code(),
                discount.name(),
                discount.amount(),
                discount.percentage()
        );
    }

    /**
     * Результат применения скидок
     */
    public record DiscountApplicationResult(
            BigDecimal basePremium,
            BigDecimal totalDiscount,
            BigDecimal finalPremium,
            List<AppliedDiscount> appliedDiscounts
    ) {}

    /**
     * Примененная скидка
     */
    public record AppliedDiscount(
            String type,
            String code,
            String description,
            BigDecimal amount,
            BigDecimal percentage
    ) {}
}