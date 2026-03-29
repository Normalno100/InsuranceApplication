package org.javaguru.travel.insurance.application.service;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.v3.InsuredPerson;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты для MultiPersonPremiumCalculationService.
 *
 * task_134: Логика multi-person расчёта.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MultiPersonPremiumCalculationService — task_134")
class MultiPersonPremiumCalculationServiceTest {

    @Mock private MedicalRiskPremiumCalculator medicalRiskCalculator;
    @Mock private UnderwritingService underwritingService;

    private MultiPersonPremiumCalculationService service;

    private static final LocalDate REF = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = REF.plusDays(30);
    private static final LocalDate DATE_TO = DATE_FROM.plusDays(14);

    @BeforeEach
    void setUp() {
        service = new MultiPersonPremiumCalculationService(medicalRiskCalculator, underwritingService);
    }

    // ── Одна персона ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Одна персона")
    class SinglePerson {

        @Test
        @DisplayName("должен рассчитать премию для одной персоны")
        void shouldCalculatePremiumForSinglePerson() {
            InsuredPerson person = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person));

            stubUnderwritingApproved();
            stubCalculatorResult(new BigDecimal("69.30"), 35, new BigDecimal("1.10"), "Adults");

            GroupPremiumResult result = service.calculateForGroup(List.of(person), request);

            assertThat(result.totalPremium()).isEqualByComparingTo("69.30");
            assertThat(result.personPremiums()).hasSize(1);
            assertThat(result.personPremiums().get(0).getFirstName()).isEqualTo("Ivan");
            assertThat(result.personPremiums().get(0).getAge()).isEqualTo(35);
            assertThat(result.isApproved()).isTrue();
        }

        @Test
        @DisplayName("PersonPremium должен содержать все поля из результата калькулятора")
        void shouldPopulatePersonPremiumFromCalculatorResult() {
            InsuredPerson person = buildPerson("Anna", "Smirnova", REF.minusYears(25));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person));

            stubUnderwritingApproved();
            stubCalculatorResult(new BigDecimal("57.75"), 25, new BigDecimal("1.00"), "Young adults");

            GroupPremiumResult result = service.calculateForGroup(List.of(person), request);

            var pp = result.personPremiums().get(0);
            assertThat(pp.getFirstName()).isEqualTo("Anna");
            assertThat(pp.getLastName()).isEqualTo("Smirnova");
            assertThat(pp.getAge()).isEqualTo(25);
            assertThat(pp.getAgeGroup()).isEqualTo("Young adults");
            assertThat(pp.getAgeCoefficient()).isEqualByComparingTo("1.00");
            assertThat(pp.getPremium()).isEqualByComparingTo("57.75");
        }
    }

    // ── Несколько персон ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Несколько персон")
    class MultiplePersons {

        @Test
        @DisplayName("totalPremium должен быть суммой индивидуальных премий")
        void totalPremiumShouldBeSumOfIndividualPremiums() {
            InsuredPerson person1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson person2 = buildPerson("Anna", "Petrova", REF.minusYears(25));
            InsuredPerson person3 = buildPerson("Alex", "Petrov", REF.minusYears(10));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person1, person2, person3));

            stubUnderwritingApproved();
            // Каждый вызов возвращает разные результаты
            when(medicalRiskCalculator.calculatePremiumWithDetails(any()))
                    .thenReturn(
                            buildCalcResult(new BigDecimal("69.30"), 35, new BigDecimal("1.10")),
                            buildCalcResult(new BigDecimal("57.75"), 25, new BigDecimal("1.00")),
                            buildCalcResult(new BigDecimal("51.98"), 10, new BigDecimal("0.90"))
                    );

            GroupPremiumResult result = service.calculateForGroup(
                    List.of(person1, person2, person3), request);

            BigDecimal expectedTotal = new BigDecimal("69.30")
                    .add(new BigDecimal("57.75"))
                    .add(new BigDecimal("51.98"));

            assertThat(result.totalPremium()).isEqualByComparingTo(expectedTotal);
            assertThat(result.personPremiums()).hasSize(3);
        }

        @Test
        @DisplayName("порядок personPremiums должен совпадать с порядком персон в запросе")
        void shouldPreservePersonOrder() {
            InsuredPerson person1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson person2 = buildPerson("Anna", "Smirnova", REF.minusYears(30));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person1, person2));

            stubUnderwritingApproved();
            when(medicalRiskCalculator.calculatePremiumWithDetails(any()))
                    .thenReturn(
                            buildCalcResult(new BigDecimal("69.30"), 35, new BigDecimal("1.10")),
                            buildCalcResult(new BigDecimal("63.00"), 30, new BigDecimal("1.00"))
                    );

            GroupPremiumResult result = service.calculateForGroup(List.of(person1, person2), request);

            assertThat(result.personPremiums().get(0).getFirstName()).isEqualTo("Ivan");
            assertThat(result.personPremiums().get(1).getFirstName()).isEqualTo("Anna");
        }

        @Test
        @DisplayName("firstPersonDetails должен содержать детали расчёта первой персоны")
        void firstPersonDetailsShouldBeFromFirstPerson() {
            InsuredPerson person1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson person2 = buildPerson("Anna", "Petrova", REF.minusYears(25));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person1, person2));

            stubUnderwritingApproved();
            var firstDetails = buildCalcResult(new BigDecimal("69.30"), 35, new BigDecimal("1.10"));
            var secondDetails = buildCalcResult(new BigDecimal("57.75"), 25, new BigDecimal("1.00"));
            when(medicalRiskCalculator.calculatePremiumWithDetails(any()))
                    .thenReturn(firstDetails, secondDetails);

            GroupPremiumResult result = service.calculateForGroup(List.of(person1, person2), request);

            // firstPersonDetails соответствует ПЕРВОЙ персоне
            assertThat(result.firstPersonDetails().ageDetails().age()).isEqualTo(35);
        }
    }

    // ── Андеррайтинг ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Агрегация андеррайтинга")
    class UnderwritingAggregation {

        @Test
        @DisplayName("APPROVED если все персоны APPROVED")
        void shouldBeApprovedWhenAllPersonsApproved() {
            InsuredPerson p1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson p2 = buildPerson("Anna", "Petrova", REF.minusYears(30));
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(p1, p2));

            stubUnderwritingApproved();
            stubCalculatorResult(new BigDecimal("60.00"), 35, new BigDecimal("1.10"), "Adults");

            GroupPremiumResult result = service.calculateForGroup(List.of(p1, p2), request);

            assertThat(result.isApproved()).isTrue();
            assertThat(result.isDeclined()).isFalse();
            assertThat(result.requiresReview()).isFalse();
        }

        @Test
        @DisplayName("DECLINED если хотя бы одна персона DECLINED")
        void shouldBeDeclinedWhenAnyPersonDeclined() {
            InsuredPerson p1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson p2 = buildPerson("Nikolai", "Smirnov", REF.minusYears(85)); // слишком старый

            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(p1, p2));

            // Первая персона одобрена, вторая отклонена
            when(underwritingService.evaluateApplication(any()))
                    .thenReturn(UnderwritingResult.approved(List.of()))
                    .thenReturn(UnderwritingResult.declined(
                            List.of(RuleResult.blocking("AgeRule", "Age 85 exceeds max 80")),
                            "Age 85 exceeds max 80"
                    ));
            stubCalculatorResult(new BigDecimal("60.00"), 35, new BigDecimal("1.10"), "Adults");

            GroupPremiumResult result = service.calculateForGroup(List.of(p1, p2), request);

            assertThat(result.isDeclined()).isTrue();
            assertThat(result.isApproved()).isFalse();
            assertThat(result.groupUnderwriting().getDeclineReason())
                    .contains("Age 85 exceeds max 80");
        }

        @Test
        @DisplayName("REQUIRES_REVIEW если хотя бы одна персона REQUIRES_REVIEW (и нет DECLINED)")
        void shouldRequireReviewWhenAnyPersonRequiresReview() {
            InsuredPerson p1 = buildPerson("Ivan", "Petrov", REF.minusYears(35));
            InsuredPerson p2 = buildPerson("Nikolai", "Smirnov", REF.minusYears(77)); // 77 лет

            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(p1, p2));

            when(underwritingService.evaluateApplication(any()))
                    .thenReturn(UnderwritingResult.approved(List.of()))
                    .thenReturn(UnderwritingResult.requiresReview(
                            List.of(RuleResult.reviewRequired("AgeRule", "Age 77 requires review")),
                            "Age 77 requires review"
                    ));
            stubCalculatorResult(new BigDecimal("60.00"), 77, new BigDecimal("2.50"), "Very elderly");

            GroupPremiumResult result = service.calculateForGroup(List.of(p1, p2), request);

            assertThat(result.requiresReview()).isTrue();
            assertThat(result.isDeclined()).isFalse();
        }

        @Test
        @DisplayName("DECLINED имеет приоритет над REQUIRES_REVIEW")
        void declinedShouldTakePriorityOverReview() {
            InsuredPerson p1 = buildPerson("Ivan", "Petrov", REF.minusYears(77)); // review
            InsuredPerson p2 = buildPerson("Nikolai", "Smirnov", REF.minusYears(85)); // decline

            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(p1, p2));

            when(underwritingService.evaluateApplication(any()))
                    .thenReturn(UnderwritingResult.requiresReview(
                            List.of(), "Age 77 requires review"))
                    .thenReturn(UnderwritingResult.declined(
                            List.of(), "Age 85 exceeds max"));
            stubCalculatorResult(new BigDecimal("60.00"), 77, new BigDecimal("2.50"), "Very elderly");

            GroupPremiumResult result = service.calculateForGroup(List.of(p1, p2), request);

            assertThat(result.isDeclined()).isTrue();
        }
    }

    // ── Минимальная премия ────────────────────────────────────────────────────

    @Test
    @DisplayName("должен применить минимальную премию (10 EUR) если расчётная меньше")
    void shouldApplyMinimumPremiumWhenCalculatedIsLower() {
        InsuredPerson person = buildPerson("Baby", "Petrov", REF.minusYears(1));
        TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person));

        stubUnderwritingApproved();
        // Очень маленькая расчётная премия
        stubCalculatorResult(new BigDecimal("1.50"), 1, new BigDecimal("1.10"), "Infants and toddlers");

        GroupPremiumResult result = service.calculateForGroup(List.of(person), request);

        assertThat(result.personPremiums().get(0).getPremium())
                .isGreaterThanOrEqualTo(BigDecimal.valueOf(10.00));
    }

    // ── GroupPremiumResult helpers ────────────────────────────────────────────

    @Test
    @DisplayName("GroupPremiumResult.isApproved() работает корректно")
    void groupPremiumResultIsApprovedShouldWork() {
        InsuredPerson person = buildPerson("Ivan", "Petrov", REF.minusYears(35));
        TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person));
        stubUnderwritingApproved();
        stubCalculatorResult(new BigDecimal("69.30"), 35, new BigDecimal("1.10"), "Adults");

        GroupPremiumResult result = service.calculateForGroup(List.of(person), request);

        assertThat(result.isApproved()).isTrue();
        assertThat(result.isDeclined()).isFalse();
        assertThat(result.requiresReview()).isFalse();
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private InsuredPerson buildPerson(String firstName, String lastName, LocalDate birthDate) {
        return InsuredPerson.builder()
                .personFirstName(firstName)
                .personLastName(lastName)
                .personBirthDate(birthDate)
                .build();
    }

    private TravelCalculatePremiumRequestV3 buildRequest(List<InsuredPerson> persons) {
        return TravelCalculatePremiumRequestV3.builder()
                .persons(persons)
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_TO)
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .currency("EUR")
                .build();
    }

    private void stubUnderwritingApproved() {
        when(underwritingService.evaluateApplication(any()))
                .thenReturn(UnderwritingResult.approved(List.of()));
    }

    private void stubCalculatorResult(BigDecimal premium, int age,
                                      BigDecimal ageCoeff, String ageGroup) {
        when(medicalRiskCalculator.calculatePremiumWithDetails(any()))
                .thenReturn(buildCalcResult(premium, age, ageCoeff));
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult buildCalcResult(
            BigDecimal premium, int age, BigDecimal ageCoeff) {

        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(
                age, ageCoeff, age <= 17 ? "Children and teenagers" :
                age <= 30 ? "Young adults" :
                        age <= 40 ? "Adults" : "Middle-aged");

        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Spain", new BigDecimal("1.0"), null, null, "EUR");

        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14, new BigDecimal("0.95"), BigDecimal.ZERO, new BigDecimal("1.0"),
                new BigDecimal("50000"));

        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(
                List.of(),
                new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));

        var payoutLimitDetails = new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                new BigDecimal("50000"), new BigDecimal("50000"), false);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                premium, new BigDecimal("4.50"), ageDetails, countryDetails, tripDetails,
                riskDetails, MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                List.of(), payoutLimitDetails);
    }
}