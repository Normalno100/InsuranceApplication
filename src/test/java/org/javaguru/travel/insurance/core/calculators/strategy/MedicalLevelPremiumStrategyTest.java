package org.javaguru.travel.insurance.core.calculators.strategy;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.BundleDiscountResult;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator.PremiumCalculationResult;
import org.javaguru.travel.insurance.core.services.CalculationConfigService;
import org.javaguru.travel.insurance.core.services.CountryDefaultDayPremiumService;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalLevelPremiumStrategyTest {

    private static final LocalDate DATE_FROM = LocalDate.of(2025, 6, 1);
    private static final LocalDate DATE_TO   = LocalDate.of(2025, 6, 15); // 14 дней
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1); // 35 лет

    @Mock private MedicalRiskLimitLevelRepository medicalLevelRepository;
    @Mock private CountryRepository countryRepository;
    @Mock private CountryDefaultDayPremiumService countryDefaultDayPremiumService;
    @Mock private SharedCalculationComponents shared;
    @Mock private CalculationStepsBuilder stepsBuilder;
    @Mock private CalculationConfigService calculationConfigService;

    @InjectMocks
    private MedicalLevelPremiumStrategy strategy;

    // =========================================================
    // applyAgeCoefficient = null → читает из DB
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsNull_resolvesFromDb() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(null, DATE_FROM);
    }

    @Test
    void calculate_whenDbEnablesCoefficient_passesEnabledTrueToShared() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.10"));

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, true);
    }

    @Test
    void calculate_whenDbDisablesCoefficient_passesEnabledFalseToShared() {
        var request = buildRequest(null);
        setupMocks(false, BigDecimal.ONE);

        strategy.calculate(request);

        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    // =========================================================
    // applyAgeCoefficient = true → принудительно включает коэффициент
    // =========================================================

    @Test
    void calculate_whenApplyAgeCoefficientIsTrue_resolvesToTrue() {
        var request = buildRequest(true);
        setupMocks(true, new BigDecimal("1.10"));

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
        setupMocks(false, BigDecimal.ONE);

        strategy.calculate(request);

        verify(calculationConfigService).resolveAgeCoefficientEnabled(false, DATE_FROM);
        verify(shared).calculateAge(BIRTH_DATE, DATE_FROM, false);
    }

    @Test
    void calculate_whenAgeCoefficientDisabled_premiumNotAffectedByAge() {
        // При enabled=false AgeCalculator вернёт 1.0, что не меняет базовую премию
        var request = buildRequest(false);
        BigDecimal ageCoeffDisabled = BigDecimal.ONE;
        setupMocks(false, ageCoeffDisabled);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(BigDecimal.ONE);
    }

    @Test
    void calculate_whenAgeCoefficientEnabled_premiumUsesAgeCoefficient() {
        var request = buildRequest(true);
        BigDecimal ageCoeff = new BigDecimal("1.30"); // Middle-aged
        setupMocks(true, ageCoeff);

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.ageCoefficient()).isEqualByComparingTo(ageCoeff);
    }

    // =========================================================
    // Корректность итогового расчёта
    // =========================================================

    @Test
    void calculate_returnsResultWithCorrectMode() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.00"));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.calculationMode().name()).isEqualTo("MEDICAL_LEVEL");
    }

    @Test
    void calculate_returnsResultWithAge() {
        var request = buildRequest(null);
        setupMocks(true, new BigDecimal("1.00"));

        PremiumCalculationResult result = strategy.calculate(request);

        assertThat(result.age()).isEqualTo(35);
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
     * Настраивает все моки для корректного прохождения calculate().
     *
     * @param ageCoefficientEnabled результат resolveAgeCoefficientEnabled()
     * @param ageCoeff              возвращаемый коэффициент возраста
     */
    private void setupMocks(boolean ageCoefficientEnabled, BigDecimal ageCoeff) {
        // MedicalLevel
        var medicalLevel = mock(MedicalRiskLimitLevelEntity.class);
        when(medicalLevel.getDailyRate()).thenReturn(new BigDecimal("2.00"));
        when(medicalLevel.getCoverageAmount()).thenReturn(new BigDecimal("10000.00"));
        when(medicalLevelRepository.findActiveByCode(eq("10000"), eq(DATE_FROM)))
                .thenReturn(Optional.of(medicalLevel));

        // Country
        var country = mock(CountryEntity.class);
        when(country.getRiskCoefficient()).thenReturn(new BigDecimal("1.0"));
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
        when(shared.getDurationCoefficient(eq(14L), eq(DATE_FROM))).thenReturn(new BigDecimal("0.95"));
        when(shared.calculateAdditionalRisks(any(), eq(35), eq(DATE_FROM)))
                .thenReturn(new SharedCalculationComponents.AdditionalRisksResult(BigDecimal.ZERO, List.of()));
        when(shared.calculateBundleDiscount(any(), any(), eq(DATE_FROM)))
                .thenReturn(new BundleDiscountResult(null, BigDecimal.ZERO));
        when(shared.buildRiskDetails(any(), any(), any(), any(), any(), anyInt(), eq(35), eq(DATE_FROM)))
                .thenReturn(List.of());

        // CountryDefaultDayPremiumService (для CountryInfo)
        when(countryDefaultDayPremiumService.findDefaultDayPremium(eq("ES"), eq(DATE_FROM)))
                .thenReturn(Optional.empty());

        // CalculationStepsBuilder
        when(stepsBuilder.buildMedicalLevelSteps(any(), any(), any(), any(), any(),
                anyLong(), any(), any(), any()))
                .thenReturn(List.of());
    }
}
