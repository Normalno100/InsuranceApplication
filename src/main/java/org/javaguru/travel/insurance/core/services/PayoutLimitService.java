package org.javaguru.travel.insurance.core.services;

import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.MedicalRiskLimitLevelEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Сервис для применения лимита страховых выплат по медицинскому риску.
 *
 * task_117: Если рассчитанная премия имплицирует выплаты > лимита,
 * применяется корректировка премии.
 *
 * ЛОГИКА КОРРЕКТИРОВКИ:
 *   1. Рассчитываем loss ratio (отношение выплат к премии).
 *      Стандартное значение: premium = coverageAmount * LOSS_RATIO_FACTOR / days_normalizer.
 *      Упрощение: если maxPayoutAmount < coverageAmount,
 *      корректируем премию пропорционально: premium * (maxPayout / coverage).
 *
 *   2. Итоговая формула коррекции:
 *      adjustedPremium = rawPremium * (maxPayoutAmount / coverageAmount)
 *      Применяется ТОЛЬКО если maxPayoutAmount < coverageAmount.
 */
@Slf4j
@Service
public class PayoutLimitService {

    /**
     * Применяет лимит выплат к рассчитанной премии.
     *
     * @param rawPremium     базовая премия до коррекции
     * @param coverageAmount сумма покрытия (из уровня)
     * @param maxPayoutAmount максимальный лимит выплат (может быть null)
     * @return результат с (возможно скорректированной) премией и флагом применения
     */
    public PayoutLimitResult applyPayoutLimit(
            BigDecimal rawPremium,
            BigDecimal coverageAmount,
            BigDecimal maxPayoutAmount) {

        // Если лимит не задан или равен/выше coverage — корректировка не нужна
        if (maxPayoutAmount == null || maxPayoutAmount.compareTo(coverageAmount) >= 0) {
            log.debug("Payout limit not applicable: maxPayout={}, coverage={}",
                    maxPayoutAmount, coverageAmount);
            return new PayoutLimitResult(rawPremium, maxPayoutAmount != null ? maxPayoutAmount : coverageAmount, false);
        }

        // Лимит ниже суммы покрытия — корректируем премию
        // adjustedPremium = rawPremium * (maxPayoutAmount / coverageAmount)
        BigDecimal ratio = maxPayoutAmount.divide(coverageAmount, 10, RoundingMode.HALF_UP);
        BigDecimal adjustedPremium = rawPremium.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

        log.info("Payout limit applied: coverage={}, maxPayout={}, ratio={}, " +
                        "rawPremium={} → adjustedPremium={}",
                coverageAmount, maxPayoutAmount, ratio, rawPremium, adjustedPremium);

        return new PayoutLimitResult(adjustedPremium, maxPayoutAmount, true);
    }

    /**
     * Результат применения лимита выплат.
     *
     * @param adjustedPremium   итоговая премия (скорректированная или исходная)
     * @param appliedPayoutLimit лимит выплат, который был применён (или coverageAmount если нет лимита)
     * @param payoutLimitApplied true если лимит был фактически применён (premium скорректирована)
     */
    public record PayoutLimitResult(
            BigDecimal adjustedPremium,
            BigDecimal appliedPayoutLimit,
            boolean payoutLimitApplied
    ) {}
}