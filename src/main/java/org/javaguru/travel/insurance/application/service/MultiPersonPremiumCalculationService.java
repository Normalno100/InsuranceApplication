package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.v3.InsuredPerson;
import org.javaguru.travel.insurance.application.dto.v3.PersonPremium;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingDecision;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Сервис расчёта страховой премии для группы застрахованных.
 *
 * task_134: Реализует логику multi-person расчёта.
 *
 * АРХИТЕКТУРА:
 *   Для каждой персоны вызывается существующий MedicalRiskPremiumCalculator
 *   с индивидуальными данными персоны (дата рождения, applyAgeCoefficient).
 *   Общие параметры поездки (страна, даты, риски) одинаковы для всех персон.
 *
 * ОБЩАЯ ПРЕМИЯ:
 *   totalPremium = сумма базовых премий всех персон
 *   Скидки применяются к totalPremium в DiscountApplicationService.
 *
 * АНДЕРРАЙТИНГ:
 *   Наиболее строгое решение из всех персон:
 *   - Если хотя бы одна → DECLINED, весь полис → DECLINED
 *   - Если хотя бы одна → REQUIRES_REVIEW, полис → REQUIRES_REVIEW
 *   - Иначе → APPROVED
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultiPersonPremiumCalculationService {

    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final UnderwritingService underwritingService;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    /**
     * Рассчитывает премии для группы застрахованных.
     *
     * @param persons  список застрахованных персон
     * @param request  общие параметры запроса (страна, даты, риски и т.д.)
     * @return результат расчёта для группы
     */
    public GroupPremiumResult calculateForGroup(
            List<InsuredPerson> persons,
            TravelCalculatePremiumRequestV3 request) {

        log.info("Calculating group premium for {} persons", persons.size());

        List<PersonPremium> personPremiums = new ArrayList<>();
        BigDecimal totalPremium = BigDecimal.ZERO;
        MedicalRiskPremiumCalculator.PremiumCalculationResult firstPersonDetails = null;

        // Флаги для агрегации андеррайтинга
        boolean anyDeclined = false;
        boolean anyRequiresReview = false;
        List<RuleResult> allRuleResults = new ArrayList<>();
        String groupDeclineReason = null;

        for (int i = 0; i < persons.size(); i++) {
            InsuredPerson person = persons.get(i);

            log.debug("Calculating premium for person {} ({} {})",
                    i + 1, person.getPersonFirstName(), person.getPersonLastName());

            // Создаём адаптированный запрос для данной персоны
            TravelCalculatePremiumRequest personRequest = adaptToPersonRequest(person, request);

            // Андеррайтинг для персоны
            UnderwritingResult personUnderwriting = underwritingService.evaluateApplication(personRequest);
            allRuleResults.addAll(personUnderwriting.getRuleResults());

            if (personUnderwriting.isDeclined()) {
                anyDeclined = true;
                groupDeclineReason = personUnderwriting.getDeclineReason();
                log.warn("Person {} {}: underwriting DECLINED — {}",
                        person.getPersonFirstName(), person.getPersonLastName(),
                        personUnderwriting.getDeclineReason());
            } else if (personUnderwriting.requiresManualReview()) {
                anyRequiresReview = true;
                if (groupDeclineReason == null) {
                    groupDeclineReason = personUnderwriting.getDeclineReason();
                }
                log.info("Person {} {}: underwriting REQUIRES_REVIEW — {}",
                        person.getPersonFirstName(), person.getPersonLastName(),
                        personUnderwriting.getDeclineReason());
            }

            // Расчёт премии для персоны
            MedicalRiskPremiumCalculator.PremiumCalculationResult calcResult =
                    medicalRiskCalculator.calculatePremiumWithDetails(personRequest);

            BigDecimal personPremium = applyMinimumPremium(calcResult.premium());

            // Сохраняем детали первой персоны для метаданных ответа
            if (i == 0) {
                firstPersonDetails = calcResult;
            }

            PersonPremium pp = PersonPremium.builder()
                    .firstName(person.getPersonFirstName())
                    .lastName(person.getPersonLastName())
                    .age(calcResult.ageDetails().age())
                    .ageGroup(calcResult.ageDetails().ageGroupDescription())
                    .premium(personPremium)
                    .ageCoefficient(calcResult.ageDetails().ageCoefficient())
                    .build();

            personPremiums.add(pp);
            totalPremium = totalPremium.add(personPremium);

            log.debug("Person {} {}: age={}, ageCoeff={}, premium={}",
                    person.getPersonFirstName(), person.getPersonLastName(),
                    calcResult.ageDetails().age(),
                    calcResult.ageDetails().ageCoefficient(),
                    personPremium);
        }

        totalPremium = totalPremium.setScale(2, RoundingMode.HALF_UP);

        // Формируем групповое решение андеррайтинга
        UnderwritingResult groupUnderwriting;
        if (anyDeclined) {
            groupUnderwriting = UnderwritingResult.declined(allRuleResults, groupDeclineReason);
        } else if (anyRequiresReview) {
            groupUnderwriting = UnderwritingResult.requiresReview(allRuleResults, groupDeclineReason);
        } else {
            groupUnderwriting = UnderwritingResult.approved(allRuleResults);
        }

        log.info("Group premium calculation complete: {} persons, totalPremium={}, underwriting={}",
                persons.size(), totalPremium, groupUnderwriting.getDecision());

        return new GroupPremiumResult(
                totalPremium,
                personPremiums,
                groupUnderwriting,
                firstPersonDetails
        );
    }

    /**
     * Адаптирует данные персоны и общие параметры запроса V3
     * в запрос V2 для MedicalRiskPremiumCalculator.
     */
    private TravelCalculatePremiumRequest adaptToPersonRequest(
            InsuredPerson person,
            TravelCalculatePremiumRequestV3 request) {

        return TravelCalculatePremiumRequest.builder()
                .personFirstName(person.getPersonFirstName())
                .personLastName(person.getPersonLastName())
                .personBirthDate(person.getPersonBirthDate())
                .applyAgeCoefficient(person.getApplyAgeCoefficient())
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                .countryIsoCode(request.getCountryIsoCode())
                .medicalRiskLimitLevel(request.getMedicalRiskLimitLevel())
                .useCountryDefaultPremium(request.getUseCountryDefaultPremium())
                .selectedRisks(request.getSelectedRisks())
                .currency(request.getCurrency())
                // promoCode и скидки применяются к итоговой сумме полиса, не к персоне
                .promoCode(null)
                .personsCount(1)
                .isCorporate(false)
                .build();
    }

    /**
     * Применяет минимальную премию к индивидуальной премии персоны.
     */
    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium == null || premium.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0) {
            return MIN_PREMIUM;
        }
        return premium;
    }
}