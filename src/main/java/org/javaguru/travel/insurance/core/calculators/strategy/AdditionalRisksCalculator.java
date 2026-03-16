package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Компонент расчёта суммарного коэффициента дополнительных рисков
 * с учётом возрастных модификаторов.
 *
 * ОТВЕТСТВЕННОСТЬ (SRP):
 *   Для каждого выбранного необязательного риска получает базовый коэффициент,
 *   применяет возрастной модификатор и возвращает суммарный коэффициент
 *   и детали по каждому риску.
 *
 * ИСПРАВЛЕНИЕ DIP (п. 3.1):
 *   Зависит от ReferenceDataPort (domain port), а не от RiskTypeRepository.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdditionalRisksCalculator {

    // ✅ Domain port — правильная зависимость для core слоя
    private final ReferenceDataPort referenceDataPort;
    private final AgeRiskPricingService ageRiskPricingService;

    /**
     * Рассчитывает суммарный коэффициент дополнительных рисков.
     *
     * Обязательные риски (isMandatory=true) исключаются из расчёта:
     * TRAVEL_MEDICAL уже включён в базовую премию.
     *
     * @param selectedRiskCodes список кодов выбранных рисков (может быть null/empty)
     * @param age               возраст застрахованного (для возрастных модификаторов)
     * @param agreementDate     дата начала поездки (для temporal validity)
     * @return результат с суммарным коэффициентом и деталями по каждому риску
     */
    public AdditionalRisksResult calculate(
            List<String> selectedRiskCodes,
            int age,
            LocalDate agreementDate) {

        if (selectedRiskCodes == null || selectedRiskCodes.isEmpty()) {
            return new AdditionalRisksResult(BigDecimal.ZERO, new ArrayList<>());
        }

        List<RiskDetail> riskDetails = new ArrayList<>();
        BigDecimal totalCoefficient = BigDecimal.ZERO;

        for (String riskCode : selectedRiskCodes) {
            Optional<Risk> riskOpt = referenceDataPort.findRisk(
                    new RiskCode(riskCode), agreementDate);

            if (riskOpt.isEmpty() || riskOpt.get().isMandatory()) {
                // Риск не найден или обязательный — пропускаем
                continue;
            }

            Risk risk = riskOpt.get();
            BigDecimal baseCoefficient = risk.getBaseCoefficient().value();

            BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                    riskCode, age, agreementDate);

            BigDecimal modifiedCoefficient = baseCoefficient.multiply(ageModifier);

            riskDetails.add(new RiskDetail(
                    riskCode, baseCoefficient, ageModifier, modifiedCoefficient));

            totalCoefficient = totalCoefficient.add(modifiedCoefficient);

            log.debug("Additional risk '{}': base={}, ageModifier={}, modified={}",
                    riskCode, baseCoefficient, ageModifier, modifiedCoefficient);
        }

        log.debug("Total additional risks coefficient: {} ({} risks)",
                totalCoefficient, riskDetails.size());

        return new AdditionalRisksResult(totalCoefficient, riskDetails);
    }

    // ========================================
    // ВЛОЖЕННЫЕ ТИПЫ
    // ========================================

    /**
     * Суммарный результат расчёта дополнительных рисков.
     *
     * @param totalCoefficient суммарный коэффициент всех применённых рисков
     * @param riskDetails      детали по каждому риску с возрастным модификатором
     */
    public record AdditionalRisksResult(
            BigDecimal totalCoefficient,
            List<RiskDetail> riskDetails
    ) {}

    /**
     * Детали по одному риску после применения возрастного модификатора.
     */
    public record RiskDetail(
            String riskCode,
            BigDecimal baseCoefficient,
            BigDecimal ageModifier,
            BigDecimal modifiedCoefficient
    ) {}
}