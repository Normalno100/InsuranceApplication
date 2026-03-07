package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.services.RiskBundleService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Компонент расчёта скидки за пакет рисков (bundle discount).
 *
 * ОТВЕТСТВЕННОСТЬ (SRP):
 *   Поиск наилучшего применимого пакета рисков и расчёт суммы скидки
 *   на основе процента пакета.
 *
 * АРХИТЕКТУРА:
 *   core слой → зависит от RiskBundleService (тоже core).
 *   Нет зависимостей на infrastructure.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BundleDiscountCalculator {

    private final RiskBundleService riskBundleService;

    /**
     * Ищет лучший применимый пакет и рассчитывает сумму скидки.
     *
     * Если ни один пакет не применим (нет selectedRisks или нет подходящего
     * пакета) — возвращает нулевую скидку.
     *
     * @param selectedRisks список кодов выбранных рисков
     * @param premiumAmount базовая премия (до скидки)
     * @param agreementDate дата начала поездки (для temporal validity)
     * @return результат с найденным пакетом и суммой скидки
     */
    public BundleDiscountResult calculate(
            List<String> selectedRisks,
            BigDecimal premiumAmount,
            LocalDate agreementDate) {

        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        var bestBundleOpt = riskBundleService.getBestApplicableBundle(selectedRisks, agreementDate);

        if (bestBundleOpt.isEmpty()) {
            log.debug("No applicable bundle found for risks: {}", selectedRisks);
            return new BundleDiscountResult(null, BigDecimal.ZERO);
        }

        RiskBundleService.ApplicableBundleResult bundle = bestBundleOpt.get();
        BigDecimal discountAmount = riskBundleService.calculateBundleDiscount(premiumAmount, bundle);

        log.info("Bundle discount applied: '{}' ({}%) → {} EUR discount on {} EUR premium",
                bundle.code(), bundle.discountPercentage(), discountAmount, premiumAmount);

        return new BundleDiscountResult(bundle, discountAmount);
    }
}