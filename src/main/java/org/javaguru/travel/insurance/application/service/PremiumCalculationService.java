package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.v3.InsuredPerson;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Сервис расчета премии.
 *
 * ЦЕЛЬ: Координация расчета базовой премии.
 *
 * task_134: Добавлен метод calculateForGroup() — делегирование
 * в MultiPersonPremiumCalculationService для V3 запросов.
 *
 * ОБЯЗАННОСТИ:
 * 1. Делегирование расчета калькулятору (V2 — одна персона)
 * 2. Делегирование в MultiPersonPremiumCalculationService (V3 — группа)
 * 3. Применение минимальной премии
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PremiumCalculationService {

    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final MultiPersonPremiumCalculationService multiPersonService;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    /**
     * Рассчитывает премию для одной персоны (V2 API).
     */
    public PremiumCalculationResult calculate(TravelCalculatePremiumRequest request) {
        log.debug("Calculating premium for country: {}, medical level: {}",
                request.getCountryIsoCode(), request.getMedicalRiskLimitLevel());

        var calculatorResult = medicalRiskCalculator.calculatePremiumWithDetails(request);

        BigDecimal finalPremium = applyMinimumPremium(calculatorResult.premium());

        log.info("Premium calculated: {} EUR (before discounts)", finalPremium);

        return new PremiumCalculationResult(finalPremium, calculatorResult);
    }

    /**
     * Рассчитывает групповую премию для нескольких персон (V3 API).
     *
     * task_134: Делегирует в MultiPersonPremiumCalculationService.
     * V2 API (одна персона) продолжает работать через метод calculate().
     *
     * @param request запрос V3 со списком персон
     * @return результат расчёта для группы
     */
    public GroupPremiumResult calculateForGroup(TravelCalculatePremiumRequestV3 request) {
        log.debug("Calculating group premium for {} persons, country: {}",
                request.getPersons() != null ? request.getPersons().size() : 0,
                request.getCountryIsoCode());

        return multiPersonService.calculateForGroup(request.getPersons(), request);
    }

    /**
     * Адаптер V2 → V3: создаёт GroupPremiumResult из одной персоны V2 запроса.
     *
     * Позволяет V2 API использовать ту же логику через MultiPersonPremiumCalculationService.
     *
     * @param request V2 запрос с одной персоной
     * @return результат как если бы запрос был на группу из одной персоны
     */
    public GroupPremiumResult calculateSinglePersonAsGroup(TravelCalculatePremiumRequest request) {
        log.debug("Adapting V2 single-person request to V3 group format");

        // Создаём V3 запрос с одной персоной из V2 данных
        InsuredPerson singlePerson = InsuredPerson.builder()
                .personFirstName(request.getPersonFirstName())
                .personLastName(request.getPersonLastName())
                .personBirthDate(request.getPersonBirthDate())
                .applyAgeCoefficient(request.getApplyAgeCoefficient())
                .build();

        TravelCalculatePremiumRequestV3 v3Request = TravelCalculatePremiumRequestV3.builder()
                .persons(List.of(singlePerson))
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                .countryIsoCode(request.getCountryIsoCode())
                .medicalRiskLimitLevel(request.getMedicalRiskLimitLevel())
                .useCountryDefaultPremium(request.getUseCountryDefaultPremium())
                .selectedRisks(request.getSelectedRisks())
                .currency(request.getCurrency())
                .personsCount(request.getPersonsCount())
                .isCorporate(request.getIsCorporate())
                .build();

        return multiPersonService.calculateForGroup(List.of(singlePerson), v3Request);
    }

    /**
     * Применяет минимальную премию.
     */
    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0) {
            log.debug("Applying minimum premium: {} -> {}", premium, MIN_PREMIUM);
            return MIN_PREMIUM;
        }
        return premium;
    }

    /**
     * Результат расчета премии (V2 — одна персона).
     */
    public record PremiumCalculationResult(
            BigDecimal premium,
            MedicalRiskPremiumCalculator.PremiumCalculationResult details
    ) {}
}