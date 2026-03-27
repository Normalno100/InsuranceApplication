package org.javaguru.travel.insurance.application.service;

import org.javaguru.travel.insurance.application.dto.v3.PersonPremium;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;

import java.math.BigDecimal;
import java.util.List;

/**
 * Результат расчёта премии для группы застрахованных.
 *
 * task_134: Создан для MultiPersonPremiumCalculationService.
 *
 * totalPremium       — суммарная базовая премия по всем персонам (до скидок полиса)
 * personPremiums     — список индивидуальных премий по каждой персоне
 * groupUnderwriting  — наиболее строгое решение андеррайтинга из всех персон
 * firstPersonDetails — детали расчёта первой персоны (для метаданных ответа)
 */
public record GroupPremiumResult(
        BigDecimal totalPremium,
        List<PersonPremium> personPremiums,
        UnderwritingResult groupUnderwriting,
        MedicalRiskPremiumCalculator.PremiumCalculationResult firstPersonDetails
) {
    /**
     * Возвращает true если групповой андеррайтинг одобрен.
     */
    public boolean isApproved() {
        return groupUnderwriting != null && groupUnderwriting.isApproved();
    }

    /**
     * Возвращает true если групповой андеррайтинг отклонён.
     */
    public boolean isDeclined() {
        return groupUnderwriting != null && groupUnderwriting.isDeclined();
    }

    /**
     * Возвращает true если требуется ручная проверка.
     */
    public boolean requiresReview() {
        return groupUnderwriting != null && groupUnderwriting.requiresManualReview();
    }
}