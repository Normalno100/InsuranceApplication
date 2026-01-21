package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.repositories.AgeRiskCoefficientRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Сервис для расчета возрастных модификаторов рисков
 * Возрастные коэффициенты рисков — разные риски по-разному
 * зависят от возраста
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgeRiskPricingService {

    private final AgeRiskCoefficientRepository ageRiskRepository;

    /**
     * Получает модификатор коэффициента для конкретного риска и возраста
     *
     * @param riskCode код риска
     * @param age возраст
     * @param date дата применения
     * @return модификатор (1.0 = без изменений, 1.5 = +50%, 0.8 = -20%)
     */
    public BigDecimal getAgeRiskModifier(String riskCode, int age, LocalDate date) {
        log.debug("Getting age-risk modifier for risk '{}' and age {}", riskCode, age);

        var modifierOpt = ageRiskRepository.findModifierForRiskAndAge(riskCode, age, date);

        if (modifierOpt.isEmpty()) {
            log.debug("No age-risk modifier found for risk '{}' and age {}, using 1.0",
                    riskCode, age);
            return BigDecimal.ONE;
        }

        var modifier = modifierOpt.get();
        log.debug("Found age-risk modifier for '{}' age {}: {} ({})",
                riskCode, age, modifier.getCoefficientModifier(), modifier.getDescription());

        return modifier.getCoefficientModifier();
    }

    /**
     * Получает модификатор на текущую дату
     */
    public BigDecimal getAgeRiskModifier(String riskCode, int age) {
        return getAgeRiskModifier(riskCode, age, LocalDate.now());
    }

    /**
     * Получает модификаторы для всех указанных рисков
     *
     * @param riskCodes коды рисков
     * @param age возраст
     * @param date дата применения
     * @return карта: код риска -> модификатор
     */
    public Map<String, BigDecimal> getAgeRiskModifiers(
            List<String> riskCodes,
            int age,
            LocalDate date) {

        Map<String, BigDecimal> modifiers = new HashMap<>();

        for (String riskCode : riskCodes) {
            BigDecimal modifier = getAgeRiskModifier(riskCode, age, date);
            modifiers.put(riskCode, modifier);
        }

        return modifiers;
    }

    /**
     * Рассчитывает модифицированный коэффициент риска
     *
     * @param baseRiskCoefficient базовый коэффициент риска (например, 0.30 для SPORT_ACTIVITIES)
     * @param age возраст
     * @param riskCode код риска
     * @param date дата
     * @return модифицированный коэффициент
     */
    public BigDecimal calculateModifiedRiskCoefficient(
            BigDecimal baseRiskCoefficient,
            int age,
            String riskCode,
            LocalDate date) {

        BigDecimal modifier = getAgeRiskModifier(riskCode, age, date);

        BigDecimal modifiedCoefficient = baseRiskCoefficient.multiply(modifier);

        log.debug("Modified risk coefficient for '{}': {} × {} = {}",
                riskCode, baseRiskCoefficient, modifier, modifiedCoefficient);

        return modifiedCoefficient;
    }

    /**
     * Результат расчета с деталями
     */
    public AgeRiskPricingResult getAgeRiskPricingDetails(
            String riskCode,
            BigDecimal baseCoefficient,
            int age,
            LocalDate date) {

        var modifierOpt = ageRiskRepository.findModifierForRiskAndAge(riskCode, age, date);

        if (modifierOpt.isEmpty()) {
            return new AgeRiskPricingResult(
                    riskCode,
                    age,
                    baseCoefficient,
                    BigDecimal.ONE,
                    baseCoefficient,
                    "No age-specific modifier"
            );
        }

        var modifierEntity = modifierOpt.get();
        BigDecimal modifier = modifierEntity.getCoefficientModifier();
        BigDecimal modifiedCoefficient = baseCoefficient.multiply(modifier);

        return new AgeRiskPricingResult(
                riskCode,
                age,
                baseCoefficient,
                modifier,
                modifiedCoefficient,
                modifierEntity.getDescription()
        );
    }

    /**
     * Результат расчета возрастного модификатора
     */
    public record AgeRiskPricingResult(
            String riskCode,
            int age,
            BigDecimal baseCoefficient,
            BigDecimal ageModifier,
            BigDecimal modifiedCoefficient,
            String description
    ) {}
}
