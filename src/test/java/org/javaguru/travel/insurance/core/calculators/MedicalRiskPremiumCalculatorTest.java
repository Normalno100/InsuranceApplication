package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Упрощённые тесты калькулятора премий
 * Фокус: бизнес-формула, а не детали реализации
 */
@DisplayName("MedicalRiskPremiumCalculator")
class MedicalRiskPremiumCalculatorTest {

    @Mock private AgeCalculator ageCalculator;
    @Mock private MedicalRiskLimitLevelRepository medicalRepo;
    @Mock private CountryRepository countryRepo;
    @Mock private RiskTypeRepository riskRepo;

    private MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new MedicalRiskPremiumCalculator(ageCalculator, medicalRepo, countryRepo, riskRepo);
    }

    // ========== BUSINESS FORMULA ==========

    @Test
    @DisplayName("base premium = rate × age coeff × country coeff × days")
    void shouldCalculateBasePremium() {
        // Given: 2.00 × 1.0 × 1.0 × 5 = 10.00
        mockDependencies(
                new BigDecimal("2.00"), // daily rate
                new BigDecimal("1.0"),  // age coeff
                new BigDecimal("1.0")   // country coeff
        );

        var request = createRequest(5, List.of());

        // When
        var premium = calculator.calculatePremium(request);

        // Then
        assertThat(premium).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("additional risks = base × (1 + sum of coefficients)")
    void shouldApplyAdditionalRisks() {
        // Given: 2.00 × 1.0 × 1.0 × 1.3 × 5 = 13.00 (sport 0.3)
        mockDependencies(
                new BigDecimal("2.00"),
                new BigDecimal("1.0"),
                new BigDecimal("1.0")
        );
        mockOptionalRisk("SPORT", new BigDecimal("0.3"));

        var request = createRequest(5, List.of("SPORT"));

        // When
        var premium = calculator.calculatePremium(request);

        // Then
        assertThat(premium).isEqualByComparingTo("13.00");
    }

    @Test
    @DisplayName("multiple risks = sum coefficients then apply")
    void shouldSumMultipleRisks() {
        // Given: sport 0.3 + extreme 0.6 = 0.9 → 2.00 × 1.9 × 3 = 11.40
        mockDependencies(
                new BigDecimal("2.00"),
                new BigDecimal("1.0"),
                new BigDecimal("1.0")
        );
        mockOptionalRisk("SPORT", new BigDecimal("0.3"));
        mockOptionalRisk("EXTREME", new BigDecimal("0.6"));

        var request = createRequest(3, List.of("SPORT", "EXTREME"));

        // When
        var premium = calculator.calculatePremium(request);

        // Then
        assertThat(premium).isEqualByComparingTo("11.40");
    }

    @Test
    @DisplayName("all coefficients combined correctly")
    void shouldCombineAllCoefficients() {
        // Given: 4.5 × 1.3(age) × 1.8(country) × 1.4(risk 0.4) × 10 = 147.42
        mockDependencies(
                new BigDecimal("4.5"),
                new BigDecimal("1.3"),
                new BigDecimal("1.8")
        );
        mockOptionalRisk("CHRONIC", new BigDecimal("0.4"));

        var request = createRequest(10, List.of("CHRONIC"));

        // When
        var premium = calculator.calculatePremium(request);

        // Then
        assertThat(premium).isEqualByComparingTo("147.42");
    }

    // ========== EDGE CASES ==========

    @Test
    @DisplayName("zero days = zero premium")
    void shouldReturnZeroForZeroDays() {
        mockDependencies(new BigDecimal("2.00"), new BigDecimal("1.0"), new BigDecimal("1.0"));

        var premium = calculator.calculatePremium(createRequest(0, List.of()));

        assertThat(premium).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("mandatory risks ignored in selected risks")
    void shouldIgnoreMandatoryRisks() {
        mockDependencies(new BigDecimal("2.00"), new BigDecimal("1.0"), new BigDecimal("1.0"));

        var mandatoryRisk = new RiskTypeEntity();
        mandatoryRisk.setCode("TRAVEL_MEDICAL");
        mandatoryRisk.setIsMandatory(true);
        mandatoryRisk.setCoefficient(BigDecimal.ZERO);
        when(riskRepo.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                .thenReturn(Optional.of(mandatoryRisk));

        var premium = calculator.calculatePremium(
                createRequest(5, List.of("TRAVEL_MEDICAL"))
        );

        // Should calculate as if no extra risks
        assertThat(premium).isEqualByComparingTo("10.00");
    }

    // ========== ERROR HANDLING ==========

    @Test
    @DisplayName("throws when medical level not found")
    void shouldThrowWhenMedicalLevelNotFound() {
        when(medicalRepo.findActiveByCode(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.calculatePremium(createRequest(1, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Medical level not found");
    }

    @Test
    @DisplayName("throws when country not found")
    void shouldThrowWhenCountryNotFound() {
        mockMedicalLevel();
        when(countryRepo.findActiveByIsoCode(any(), any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> calculator.calculatePremium(createRequest(1, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Country not found");
    }

    // ========== HELPERS ==========

    private void mockDependencies(BigDecimal rate, BigDecimal ageCoeff, BigDecimal countryCoeff) {
        mockAge(30, ageCoeff);
        mockMedicalLevel(rate);
        mockCountry(countryCoeff);
        mockMandatoryRisk();
    }

    private void mockAge(int age, BigDecimal coeff) {
        var result = new AgeCalculator.AgeCalculationResult(age, coeff, "Group");
        when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
        when(ageCalculator.calculateAgeAndCoefficient(any(), any())).thenReturn(result);
    }

    private void mockMedicalLevel() {
        mockMedicalLevel(new BigDecimal("2.00"));
    }

    private void mockMedicalLevel(BigDecimal rate) {
        var entity = new MedicalRiskLimitLevelEntity();
        entity.setCode("10000");
        entity.setDailyRate(rate);
        entity.setCoverageAmount(new BigDecimal("10000"));
        when(medicalRepo.findActiveByCode(any(), any())).thenReturn(Optional.of(entity));
    }

    private void mockCountry(BigDecimal coeff) {
        var entity = new CountryEntity();
        entity.setIsoCode("ES");
        entity.setNameEn("Spain");
        entity.setRiskCoefficient(coeff);
        when(countryRepo.findActiveByIsoCode(any(), any())).thenReturn(Optional.of(entity));
    }

    private void mockMandatoryRisk() {
        var entity = new RiskTypeEntity();
        entity.setCode("TRAVEL_MEDICAL");
        entity.setNameEn("Medical");
        entity.setCoefficient(BigDecimal.ZERO);
        entity.setIsMandatory(true);
        when(riskRepo.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                .thenReturn(Optional.of(entity));
    }

    private void mockOptionalRisk(String code, BigDecimal coeff) {
        var entity = new RiskTypeEntity();
        entity.setCode(code);
        entity.setNameEn(code);
        entity.setCoefficient(coeff);
        entity.setIsMandatory(false);
        when(riskRepo.findActiveByCode(eq(code), any())).thenReturn(Optional.of(entity));
    }

    private TravelCalculatePremiumRequestV2 createRequest(int days, List<String> risks) {
        LocalDate from = LocalDate.of(2025, 1, 1);
        return TravelCalculatePremiumRequestV2.builder()
                .medicalRiskLimitLevel("10000")
                .countryIsoCode("ES")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(from)
                .agreementDateTo(from.plusDays(days))
                .selectedRisks(risks)
                .build();
    }
}