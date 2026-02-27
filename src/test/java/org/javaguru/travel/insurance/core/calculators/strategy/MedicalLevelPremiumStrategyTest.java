package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
import org.javaguru.travel.insurance.core.services.PayoutLimitService;
import org.javaguru.travel.insurance.core.services.PayoutLimitService.PayoutLimitResult;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryRepository;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalLevelPremiumStrategyTest {

    private static final LocalDate DATE_FROM  = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO    = LocalDate.of(2025, 6, 15); // 14 дней
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);  // 35 лет

    private static final BigDecimal COVERAGE       = new BigDecimal("10000.00");
    private static final BigDecimal DAILY_RATE     = new BigDecimal("2.00");
    private static final BigDecimal DURATION_COEFF = new BigDecimal("0.95");
    private static final BigDecimal COUNTRY_COEFF  = new BigDecimal("1.0");

    @Mock private MedicalRiskLimitLevelRepository medicalLevelRepository;
    @Mock private CountryRepository               countryRepository;
    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private SharedCalculationComponents     shared;
    @Mock private CalculationStepsBuilder         stepsBuilder;
    @Mock private CalculationConfigService        calculationConfigService;
    @Mock private PayoutLimitService              payoutLimitService;   // task_117

    @InjectMocks
    private MedicalLevelPremiumStrategy strategy;

    // =========================================================
    // applyAgeCoefficient = null → читает из DB
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsNull_resolvesFromDb() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"), noPayoutLimit());

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(null, DATE_FROM);
    }

    @Test
    void calculate_whenDbEnablesCoefficient_passesEnabledTrueToShared() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"), noPayoutLimit());

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, true);
    }

    @Test
    void calculate_whenDbDisablesCoefficient_passesEnabledFalseToShared() {
        var request = buildRequest(null);
        setupMocks(false, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    // =========================================================
    // applyAgeCoefficient = true → принудительно включает коэффициент
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsTrue_resolvesToTrue() {
        var request = buildRequest(true);
        setupMocks(true, new BigDecimal("1.10"), noPayoutLimit());

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(true, DATE_FROM);
        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, true);
    }

    // =========================================================
    // applyAgeCoefficient = false → принудительно отключает
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsFalse_resolvesToFalse() {
        var request = buildRequest(false);
        setupMocks(false, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(false, DATE_FROM);
        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    @Test
    void calculate_whenAgeCoefficientDisabled_premiumNotAffectedByAge() {
        var request = buildRequest(false);
        setupMocks(false, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void calculate_whenAgeCoefficientEnabled_premiumUsesAgeCoefficient() {
        var request = buildRequest(true);
        BigDecimal ageCoeff = new BigDecimal("1.30");
        setupMocks(true, ageCoeff, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(ageCoeff);
    }

    // =========================================================
    // Корректность итогового расчёта
    // =========================================================

    @Test
    void calculate_returnsResultWithCorrectMode() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("MEDICAL_LEVEL");
    }

    @Test
    void calculate_returnsResultWithAge() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.age()).isEqualTo(35);
    }

    // =========================================================
    // task_117: лимит выплат НЕ применяется (maxPayout == coverage)
    // =========================================================

    @Test
    void calculate_whenPayoutLimitNotApplied_flagIsFalse() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitApplied()).isFalse();
    }

    @Test
    void calculate_whenPayoutLimitNotApplied_payoutLimitServiceIsCalled() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        // PayoutLimitService всегда вызывается — решение принимает он сам
        verify(payoutLimitService).applyPayoutLimit(any(), eq(COVERAGE), any());
    }

    @Test
    void calculate_whenPayoutLimitNotApplied_stepsBuilderCalledWithNullLimit() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, noPayoutLimit());

        strategy.calculate(request);

        // payoutLimit=null означает что лимит не применялся → шаг коррекции не добавляется
        verify(stepsBuilder).buildMedicalLevelSteps(
                any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(),
                isNull(),   // payoutLimit
                any());     // rawBasePremium
    }

    // =========================================================
    // task_117: лимит выплат ПРИМЕНЯЕТСЯ (maxPayout < coverage)
    // =========================================================

    @Test
    void calculate_whenPayoutLimitApplied_flagIsTrue() {
        var request = buildRequest(null);
        setupMocks(true, BigDecimal.ONE, withPayoutLimit(new BigDecimal("8000.00")));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.payoutLimitApplied()).isTrue();
    }

    @Test
    void calculate_whenPayoutLimitApplied_resultContainsAppliedLimit() {
        var request = buildRequest(null);
        BigDecimal limit = new BigDecimal("8000.00");
        setupMocks(true, BigDecimal.ONE, withPayoutLimit(limit));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.appliedPayoutLimit()).isEqualByComparingTo(limit);
    }

    @Test
    void calculate_whenPayoutLimitApplied_stepsBuilderCalledWithLimit() {
        var request = buildRequest(null);
        BigDecimal limit = new BigDecimal("8000.00");
        setupMocks(true, BigDecimal.ONE, withPayoutLimit(limit));

        strategy.calculate(request);

        verify(stepsBuilder).buildMedicalLevelSteps(
                any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(),
                eq(limit),  // payoutLimit передан
                any());     // rawBasePremium
    }

    @Test
    void calculate_whenPayoutLimitApplied_adjustedPremiumUsedForBundleDiscount() {
        var request = buildRequest(null);
        BigDecimal adjustedPremium = new BigDecimal("23.00");
        setupMocks(true, BigDecimal.ONE,
                new PayoutLimitResult(adjustedPremium, new BigDecimal("8000.00"), true));

        strategy.calculate(request);

        // bundleDiscount рассчитывается от скорректированной премии
        verify(shared).calculateBundleDiscount(any(), eq(adjustedPremium), eq(DATE_FROM));
    }

    // =========================================================
    // helpers
    // =========================================================

    private TravelCalculatePremiumRequest buildRequest(Boolean applyAgeCoefficient) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(BIRTH_DATE)
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_TO)
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .applyAgeCoefficient(applyAgeCoefficient)
                .selectedRisks(Collections.emptyList())
                .build();
    }

    /**
     * PayoutLimitResult: лимит не применялся (maxPayout == coverage).
     * stepsBuilder получит payoutLimit=null.
     */
    private PayoutLimitResult noPayoutLimit() {
        return new PayoutLimitResult(
                new BigDecimal("26.60"),  // rawPremium без изменений
                COVERAGE,
                false);
    }

    /**
     * PayoutLimitResult: лимит применён (maxPayout < coverage), премия скорректирована.
     */
    private PayoutLimitResult withPayoutLimit(BigDecimal appliedLimit) {
        BigDecimal adjustedPremium = new BigDecimal("21.00");
        return new PayoutLimitResult(adjustedPremium, appliedLimit, true);
    }

    /**
     * Настраивает все моки для корректного прохождения calculate().
     *
     * @param ageCoefficientEnabled результат resolveAgeCoefficientEnabled()
     * @param ageCoeff              возвращаемый коэффициент возраста
     * @param payoutLimitResult     результат PayoutLimitService (task_117)
     */
    private void setupMocks(boolean ageCoefficientEnabled,
                            BigDecimal ageCoeff,
                            PayoutLimitResult payoutLimitResult) {

        // MedicalLevel
        var medicalLevel = mock(MedicalRiskLimitLevelEntity.class);
        when(medicalLevel.getDailyRate()).thenReturn(DAILY_RATE);
        when(medicalLevel.getCoverageAmount()).thenReturn(COVERAGE);
        when(medicalLevel.getMaxPayoutAmount()).thenReturn(null); // task_117: null = лимит определяет сервис
        when(medicalLevelRepository.findActiveByCode(eq("10000"), eq(DATE_FROM)))
                .thenReturn(Optional.of(medicalLevel));

        // Country
        var country = mock(CountryEntity.class);
        when(country.getRiskCoefficient()).thenReturn(COUNTRY_COEFF);
        when(country.getNameEn()).thenReturn("Spain");
        when(countryRepository.findActiveByIsoCode(eq("ES"), eq(DATE_FROM)))
                .thenReturn(Optional.of(country));

        // CalculationConfigService
        when(calculationConfigService.resolveAgeCoefficientEnabled(any(), eq(DATE_FROM)))
                .thenReturn(ageCoefficientEnabled);

        // SharedCalculationComponents
        var ageResult = new AgeCalculator.AgeCalculationResult(35, ageCoeff, "Adults");
        when(shared.calculateAge(eq(BIRTH_DATE), eq(DATE_FROM), eq(ageCoefficientEnabled)))
                .thenReturn(ageResult);
        when(shared.calculateDays(DATE_FROM, DATE_TO)).thenReturn(14L);
        when(shared.getDurationCoefficient(eq(14L), eq(DATE_FROM))).thenReturn(DURATION_COEFF);
        when(shared.calculateAdditionalRisks(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(BigDecimal.ZERO, List.of()));
        when(shared.calculateBundleDiscount(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));
        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), eq(35), eq(DATE_FROM)))
                .thenReturn(List.of());

        // task_117: PayoutLimitService
        when(payoutLimitService.applyPayoutLimit(any(), any(), any()))
                .thenReturn(payoutLimitResult);

        // CountryDefaultDayPremiumService
        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("ES"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        // CalculationStepsBuilder — новая сигнатура с payoutLimit и rawBasePremium
        when(stepsBuilder.buildMedicalLevelSteps(
                any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any(),
                any(),   // payoutLimit (nullable)
                any()))  // rawBasePremium
                .thenReturn(List.of());
    }
}