package org.javaguru.travel.insurance.application.service;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.v3.*;
import org.javaguru.travel.insurance.application.validation.TravelCalculatePremiumRequestValidatorV3;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
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
 * Тесты для TravelCalculatePremiumServiceV3.
 *
 * task_135: Сервис-оркестратор для V3 API.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumServiceV3 — task_135")
class TravelCalculatePremiumServiceV3Test {

    @Mock private TravelCalculatePremiumRequestValidatorV3 validator;
    @Mock private PremiumCalculationService premiumCalculationService;
    @Mock private DiscountApplicationService discountApplicationService;

    private TravelCalculatePremiumServiceV3 service;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        service = new TravelCalculatePremiumServiceV3(
                validator, premiumCalculationService, discountApplicationService);
    }

    // ── VALIDATION_ERROR ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Сценарий VALIDATION_ERROR")
    class ValidationErrorScenario {

        @Test
        @DisplayName("должен вернуть VALIDATION_ERROR когда валидация не прошла")
        void shouldReturnValidationErrorWhenValidationFails() {
            when(validator.validate(any()))
                    .thenReturn(List.of(
                            ValidationError.error("persons[0].personFirstName", "Must not be empty!")
                    ));

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getStatus())
                    .isEqualTo(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR);
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getErrors()).hasSize(1);
            assertThat(response.getErrors().get(0).getField())
                    .isEqualTo("persons[0].personFirstName");
        }

        @Test
        @DisplayName("должен не вызывать расчёт при ошибке валидации")
        void shouldNotCallCalculationWhenValidationFails() {
            when(validator.validate(any()))
                    .thenReturn(List.of(ValidationError.critical("persons", "Must not be empty!")));

            service.calculatePremium(validRequest());

            verifyNoInteractions(premiumCalculationService);
            verifyNoInteractions(discountApplicationService);
        }

        @Test
        @DisplayName("должен передавать все ошибки валидации в ответ")
        void shouldIncludeAllValidationErrors() {
            when(validator.validate(any()))
                    .thenReturn(List.of(
                            ValidationError.error("persons[0].personFirstName", "empty"),
                            ValidationError.error("persons[0].personBirthDate", "null"),
                            ValidationError.error("countryIsoCode", "unknown")
                    ));

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getErrors()).hasSize(3);
        }
    }

    // ── DECLINED ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Сценарий DECLINED")
    class DeclinedScenario {

        @Test
        @DisplayName("должен вернуть DECLINED когда андеррайтинг отклонил")
        void shouldReturnDeclinedWhenUnderwritingDeclined() {
            stubValidationPassed();
            stubGroupResultDeclined();

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getStatus())
                    .isEqualTo(TravelCalculatePremiumResponseV3.ResponseStatus.DECLINED);
            assertThat(response.getSuccess()).isFalse();
            assertThat(response.getUnderwriting()).isNotNull();
            assertThat(response.getUnderwriting().getDecision()).isEqualTo("DECLINED");
        }

        @Test
        @DisplayName("должен не применять скидки при DECLINED")
        void shouldNotApplyDiscountsWhenDeclined() {
            stubValidationPassed();
            stubGroupResultDeclined();

            service.calculatePremium(validRequest());

            verifyNoInteractions(discountApplicationService);
        }
    }

    // ── REQUIRES_REVIEW ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Сценарий REQUIRES_REVIEW")
    class RequiresReviewScenario {

        @Test
        @DisplayName("должен вернуть REQUIRES_REVIEW когда андеррайтинг требует проверки")
        void shouldReturnRequiresReviewWhenUnderwritingRequiresReview() {
            stubValidationPassed();
            stubGroupResultRequiresReview();

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getStatus())
                    .isEqualTo(TravelCalculatePremiumResponseV3.ResponseStatus.REQUIRES_REVIEW);
            assertThat(response.getSuccess()).isFalse();
        }
    }

    // ── SUCCESS ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Сценарий SUCCESS")
    class SuccessScenario {

        @Test
        @DisplayName("должен вернуть SUCCESS при успешном расчёте")
        void shouldReturnSuccessWhenCalculationSucceeds() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("138.60"));
            stubDiscountResult(new BigDecimal("138.60"), BigDecimal.ZERO);

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getStatus())
                    .isEqualTo(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS);
            assertThat(response.getSuccess()).isTrue();
        }

        @Test
        @DisplayName("ответ должен содержать personPremiums от GroupPremiumResult")
        void shouldIncludePersonPremiumsInResponse() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("138.60"));
            stubDiscountResult(new BigDecimal("138.60"), BigDecimal.ZERO);

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getPersonPremiums()).isNotNull().hasSize(2);
        }

        @Test
        @DisplayName("pricing.totalPersonsPremium должен совпадать с groupResult.totalPremium до скидок")
        void pricingTotalPersonsPremiumShouldMatchGroupResultTotalPremium() {
            stubValidationPassed();
            BigDecimal groupTotal = new BigDecimal("138.60");
            stubGroupResultApproved(groupTotal);
            stubDiscountResult(new BigDecimal("110.88"), new BigDecimal("27.72"));

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getPricing().getTotalPersonsPremium())
                    .isEqualByComparingTo(groupTotal);
        }

        @Test
        @DisplayName("pricing.totalPremium должен отражать финальную цену после скидок")
        void pricingTotalPremiumShouldReflectFinalPriceAfterDiscounts() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("138.60"));
            BigDecimal finalPremium = new BigDecimal("110.88");
            stubDiscountResult(finalPremium, new BigDecimal("27.72"));

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getPricing().getTotalPremium())
                    .isEqualByComparingTo(finalPremium);
        }

        @Test
        @DisplayName("должен содержать информацию об андеррайтинге в ответе")
        void shouldIncludeUnderwritingInfoInResponse() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("100.00"));
            stubDiscountResult(new BigDecimal("100.00"), BigDecimal.ZERO);

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getUnderwriting()).isNotNull();
            assertThat(response.getUnderwriting().getDecision()).isEqualTo("APPROVED");
        }

        @Test
        @DisplayName("должен содержать trip summary с данными из первой персоны")
        void shouldIncludeTripSummaryInResponse() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("100.00"));
            stubDiscountResult(new BigDecimal("100.00"), BigDecimal.ZERO);

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getTrip()).isNotNull();
            assertThat(response.getTrip().getCountryCode()).isEqualTo("ES");
            assertThat(response.getTrip().getDateFrom()).isEqualTo(DATE_FROM);
        }

        @Test
        @DisplayName("должен передавать валюту из запроса")
        void shouldPassCurrencyFromRequest() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("100.00"));
            stubDiscountResult(new BigDecimal("100.00"), BigDecimal.ZERO);

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

            assertThat(response.getPricing().getCurrency()).isEqualTo("EUR");
        }

        @Test
        @DisplayName("должен использовать EUR как дефолтную валюту")
        void shouldUseEurAsDefaultCurrency() {
            stubValidationPassed();
            stubGroupResultApproved(new BigDecimal("100.00"));
            stubDiscountResult(new BigDecimal("100.00"), BigDecimal.ZERO);

            TravelCalculatePremiumRequestV3 request = TravelCalculatePremiumRequestV3.builder()
                    .persons(List.of(person()))
                    .agreementDateFrom(DATE_FROM)
                    .agreementDateTo(DATE_FROM.plusDays(14))
                    .countryIsoCode("ES")
                    .medicalRiskLimitLevel("50000")
                    .currency(null) // нет валюты
                    .build();

            TravelCalculatePremiumResponseV3 response = service.calculatePremium(request);

            assertThat(response.getPricing().getCurrency()).isEqualTo("EUR");
        }
    }

    // ── Метаданные ответа ─────────────────────────────────────────────────────

    @Test
    @DisplayName("apiVersion должен быть '3.0' в любом ответе")
    void shouldAlwaysHaveApiVersion30() {
        when(validator.validate(any()))
                .thenReturn(List.of(ValidationError.error("persons", "empty")));

        TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

        assertThat(response.getApiVersion()).isEqualTo("3.0");
    }

    @Test
    @DisplayName("requestId должен быть сгенерирован в ответе")
    void shouldGenerateRequestId() {
        when(validator.validate(any()))
                .thenReturn(List.of(ValidationError.error("persons", "empty")));

        TravelCalculatePremiumResponseV3 response = service.calculatePremium(validRequest());

        assertThat(response.getRequestId()).isNotNull();
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private void stubValidationPassed() {
        when(validator.validate(any())).thenReturn(List.of());
    }

    private void stubGroupResultApproved(BigDecimal totalPremium) {
        List<PersonPremium> personPremiums = List.of(
                PersonPremium.builder().firstName("Ivan").lastName("Petrov")
                        .age(35).ageGroup("Adults")
                        .premium(totalPremium.divide(new BigDecimal("2")))
                        .ageCoefficient(new BigDecimal("1.10")).build(),
                PersonPremium.builder().firstName("Anna").lastName("Petrova")
                        .age(30).ageGroup("Young adults")
                        .premium(totalPremium.divide(new BigDecimal("2")))
                        .ageCoefficient(new BigDecimal("1.00")).build()
        );

        var calcDetails = buildCalcDetails();
        GroupPremiumResult groupResult = new GroupPremiumResult(
                totalPremium, personPremiums,
                UnderwritingResult.approved(List.of()), calcDetails);

        when(premiumCalculationService.calculateForGroup(any())).thenReturn(groupResult);
    }

    private void stubGroupResultDeclined() {
        GroupPremiumResult groupResult = new GroupPremiumResult(
                BigDecimal.ZERO, List.of(),
                UnderwritingResult.declined(List.of(), "Age 85 exceeds max"),
                null);
        when(premiumCalculationService.calculateForGroup(any())).thenReturn(groupResult);
    }

    private void stubGroupResultRequiresReview() {
        GroupPremiumResult groupResult = new GroupPremiumResult(
                new BigDecimal("100.00"), List.of(),
                UnderwritingResult.requiresReview(List.of(), "Age 77 requires review"),
                buildCalcDetails());
        when(premiumCalculationService.calculateForGroup(any())).thenReturn(groupResult);
    }

    private void stubDiscountResult(BigDecimal finalPremium, BigDecimal totalDiscount) {
        when(discountApplicationService.applyDiscounts(any(), any()))
                .thenReturn(new DiscountApplicationService.DiscountApplicationResult(
                        finalPremium.add(totalDiscount),
                        totalDiscount,
                        finalPremium,
                        List.of()
                ));
    }

    private InsuredPerson person() {
        return InsuredPerson.builder()
                .personFirstName("Ivan").personLastName("Petrov")
                .personBirthDate(TODAY.minusYears(35))
                .build();
    }

    private TravelCalculatePremiumRequestV3 validRequest() {
        return TravelCalculatePremiumRequestV3.builder()
                .persons(List.of(person(), person()))
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .currency("EUR")
                .build();
    }

    private MedicalRiskPremiumCalculator.PremiumCalculationResult buildCalcDetails() {
        var ageDetails = new MedicalRiskPremiumCalculator.AgeDetails(35, new BigDecimal("1.10"), "Adults");
        var countryDetails = new MedicalRiskPremiumCalculator.CountryDetails(
                "Spain", new BigDecimal("1.0"), null, null, "EUR");
        var tripDetails = new MedicalRiskPremiumCalculator.TripDetails(
                14, new BigDecimal("0.95"), BigDecimal.ZERO, BigDecimal.ONE, new BigDecimal("50000"));
        var riskDetails = new MedicalRiskPremiumCalculator.RiskDetails(List.of(),
                new MedicalRiskPremiumCalculator.BundleDiscountResult(null, BigDecimal.ZERO));
        var payoutLimitDetails = new MedicalRiskPremiumCalculator.PayoutLimitDetails(
                new BigDecimal("50000"), new BigDecimal("50000"), false);

        return new MedicalRiskPremiumCalculator.PremiumCalculationResult(
                new BigDecimal("69.30"), new BigDecimal("4.50"), ageDetails, countryDetails,
                tripDetails, riskDetails, MedicalRiskPremiumCalculator.CalculationMode.MEDICAL_LEVEL,
                List.of(), payoutLimitDetails);
    }
}