package org.javaguru.travel.insurance.core.calculators.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.RiskPremiumDetail;
import org.javaguru.travel.insurance.core.services.AgeRiskPricingService;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Компонент построения детальной разбивки по рискам для ответа API.
 *
 * ОТВЕТСТВЕННОСТЬ (SRP):
 *   Строит список {@link RiskPremiumDetail} — по одной записи на каждый риск
 *   (обязательный TRAVEL_MEDICAL + каждый выбранный необязательный риск).
 *   Используется для заполнения поля riskBreakdown в PricingDetails ответа.
 *
 * ИСПРАВЛЕНИЕ DIP (п. 3.1 / 3.2):
 *   Ранее SharedCalculationComponents использовал RiskTypeRepository напрямую.
 *   Теперь этот компонент зависит от ReferenceDataPort (domain port).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskDetailsBuilder {

    // ✅ Domain port — правильная зависимость для core слоя
    private final ReferenceDataPort referenceDataPort;
    private final AgeRiskPricingService ageRiskPricingService;

    /**
     * Строит детализацию по всем рискам: обязательному TRAVEL_MEDICAL
     * и каждому выбранному необязательному риску.
     *
     * В режиме COUNTRY_DEFAULT передавайте countryCoefficient = BigDecimal.ONE,
     * так как коэффициент страны уже «запечён» в базовой дневной ставке.
     *
     * @param selectedRiskCodes  список кодов выбранных необязательных рисков (может быть null)
     * @param baseRate           базовая дневная ставка (из уровня покрытия или country default)
     * @param ageCoefficient     возрастной коэффициент
     * @param countryCoefficient коэффициент страны (BigDecimal.ONE в режиме COUNTRY_DEFAULT)
     * @param durationCoefficient коэффициент длительности
     * @param days               количество дней поездки
     * @param age                возраст застрахованного
     * @param agreementDate      дата начала поездки
     * @return список деталей по каждому риску
     */
    public List<RiskPremiumDetail> build(
            List<String> selectedRiskCodes,
            BigDecimal baseRate,
            BigDecimal ageCoefficient,
            BigDecimal countryCoefficient,
            BigDecimal durationCoefficient,
            int days,
            int age,
            LocalDate agreementDate) {

        List<RiskPremiumDetail> details = new ArrayList<>();

        // Базовая премия по обязательному риску TRAVEL_MEDICAL
        BigDecimal basePremium = baseRate
                .multiply(ageCoefficient)
                .multiply(countryCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        // Обязательный риск TRAVEL_MEDICAL
        Risk medicalRisk = referenceDataPort
                .findRisk(RiskCode.TRAVEL_MEDICAL, agreementDate)
                .orElseThrow(() -> new IllegalStateException(
                        "Mandatory risk TRAVEL_MEDICAL not found for date: " + agreementDate));

        details.add(new RiskPremiumDetail(
                medicalRisk.getCode().value(),
                medicalRisk.getNameEn(),
                basePremium,
                BigDecimal.ZERO,        // baseCoefficient для обязательного риска = 0 (включён в baseRate)
                BigDecimal.ONE));       // ageModifier = 1.0 (не применяется)

        // Необязательные риски
        if (selectedRiskCodes != null) {
            for (String riskCode : selectedRiskCodes) {
                var riskOpt = referenceDataPort.findRisk(new RiskCode(riskCode), agreementDate);

                if (riskOpt.isEmpty() || riskOpt.get().isMandatory()) {
                    continue;
                }

                Risk risk = riskOpt.get();
                BigDecimal ageModifier = ageRiskPricingService.getAgeRiskModifier(
                        riskCode, age, agreementDate);

                BigDecimal baseCoeff = risk.getBaseCoefficient().value();
                BigDecimal modifiedCoefficient = baseCoeff.multiply(ageModifier);

                BigDecimal riskPremium = basePremium
                        .multiply(modifiedCoefficient)
                        .setScale(2, RoundingMode.HALF_UP);

                details.add(new RiskPremiumDetail(
                        risk.getCode().value(),
                        risk.getNameEn(),
                        riskPremium,
                        baseCoeff,
                        ageModifier));

                log.debug("Risk detail '{}': premium={}, baseCoeff={}, ageModifier={}",
                        riskCode, riskPremium, baseCoeff, ageModifier);
            }
        }

        return details;
    }
}