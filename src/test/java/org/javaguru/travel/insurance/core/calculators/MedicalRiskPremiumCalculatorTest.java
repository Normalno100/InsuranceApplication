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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
 * Тесты для упрощенного MedicalRiskPremiumCalculator
 * Теперь использует реальные repository вместо enum'ов
 */
@DisplayName("MedicalRiskPremiumCalculator Tests")
class MedicalRiskPremiumCalculatorTest {

    @Mock
    private AgeCalculator ageCalculator;

    @Mock
    private MedicalRiskLimitLevelRepository medicalLevelRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private RiskTypeRepository riskTypeRepository;

    private MedicalRiskPremiumCalculator calculator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        calculator = new MedicalRiskPremiumCalculator(
                ageCalculator,
                medicalLevelRepository,
                countryRepository,
                riskTypeRepository
        );
    }

    @Nested
    @DisplayName("Basic Premium Calculation")
    class BasicPremiumCalculation {

        @Test
        @DisplayName("Should calculate basic premium without additional risks")
        void shouldCalculateBasicPremium() {
            // Given: базовая ставка 1.50, возраст 1.0, страна 1.0, нет рисков, 5 дней
            // Ожидается: 1.50 × 1.0 × 1.0 × 1.0 × 5 = 7.50
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 5, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("7.50");
        }

        @Test
        @DisplayName("Should calculate premium with sport activities risk")
        void shouldApplyAdditionalRisks() {
            // Given: SPORT_ACTIVITIES коэффициент 0.3 → множитель (1 + 0.3) = 1.3
            // 1.50 × 1.0 × 1.0 × 1.3 × 10 = 19.50
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();
            mockOptionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3"));

            var request = createRequest("5000", "ES", 10, List.of("SPORT_ACTIVITIES"));

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("19.50");
        }

        @Test
        @DisplayName("Should sum multiple risk coefficients")
        void shouldSumMultipleRisks() {
            // Given: SPORT (0.3) + EXTREME (0.6) = (1 + 0.9) = 1.9
            // 1.50 × 1.0 × 1.0 × 1.9 × 3 = 8.55
            mockAge(25, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();
            mockOptionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3"));
            mockOptionalRisk("EXTREME_SPORT", new BigDecimal("0.6"));

            var request = createRequest("5000", "ES", 3,
                    List.of("SPORT_ACTIVITIES", "EXTREME_SPORT"));

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("8.55");
        }

        @Test
        @DisplayName("Should return zero for zero days")
        void shouldReturnZeroForZeroDays() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 0, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("Different Configurations")
    class DifferentConfigurations {

        @ParameterizedTest(name = "Level={0}, Country={1}, Age={2}, Days={3}")
        @CsvSource({
                "5000,  ES, 20, 1",     // Минимум
                "10000, DE, 45, 7",     // Средний
                "100000, ES, 70, 1",    // Максимальный возраст
                "500000, DE, 35, 60"    // Длинная поездка
        })
        @DisplayName("Should handle boundary values")
        void shouldHandleBoundaryValues(String level, String country, int age, int days) {
            mockAge(age, new BigDecimal("1.2"));
            mockMedicalLevel(level, new BigDecimal("7.00"));
            mockCountry(country, new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest(level, country, days, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isGreaterThan(BigDecimal.ZERO);
            assertThat(premium.scale()).isEqualTo(2);
        }

        @Test
        @DisplayName("Should apply age coefficient correctly")
        void shouldApplyAgeCoefficient() {
            // Given: возраст 60 лет, коэффициент 1.6
            // 2.00 × 1.6 × 1.0 × 1.0 × 10 = 32.00
            mockAge(60, new BigDecimal("1.6"));
            mockMedicalLevel("10000", new BigDecimal("2.00"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("10000", "ES", 10, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("32.00");
        }

        @Test
        @DisplayName("Should apply country coefficient correctly")
        void shouldApplyCountryCoefficient() {
            // Given: Thailand коэффициент 1.3
            // 2.00 × 1.0 × 1.3 × 1.0 × 5 = 13.00
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("10000", new BigDecimal("2.00"));
            mockCountry("TH", new BigDecimal("1.3"));
            mockMandatoryRisk();

            var request = createRequest("10000", "TH", 5, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("13.00");
        }

        @Test
        @DisplayName("Should combine all coefficients")
        void shouldCombineAllCoefficients() {
            // Given: базовая 4.5, возраст 1.3, страна 1.8, риски 0.4, 14 дней
            // 4.5 × 1.3 × 1.8 *1,002× 1.4 × 14 = 206,39
            mockAge(45, new BigDecimal("1.3"));
            mockMedicalLevel("50000", new BigDecimal("4.50"));
            mockCountry("IN", new BigDecimal("1.8"));
            mockMandatoryRisk();
            mockOptionalRisk("CHRONIC_DISEASES", new BigDecimal("0.4"));

            var request = createRequest("50000", "IN", 14,
                    List.of("CHRONIC_DISEASES"));

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("206.39");
        }
    }

    @Nested
    @DisplayName("Calculation Details")
    class CalculationDetails {

        @Test
        @DisplayName("Should return detailed calculation result")
        void shouldReturnCalculationDetails() {
            mockAge(35, new BigDecimal("1.1"));
            mockMedicalLevel("10000", new BigDecimal("2.00"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();
            mockOptionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3"));

            var request = createRequest("10000", "ES", 5,
                    List.of("SPORT_ACTIVITIES"));

            // When
            var result = calculator.calculatePremiumWithDetails(request);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.premium()).isNotNull();
            assertThat(result.baseRate()).isEqualByComparingTo("2.00");
            assertThat(result.age()).isEqualTo(35);
            assertThat(result.ageCoefficient()).isEqualByComparingTo("1.1");
            assertThat(result.countryCoefficient()).isEqualByComparingTo("1.0");
            assertThat(result.additionalRisksCoefficient()).isEqualByComparingTo("0.3");
            assertThat(result.days()).isEqualTo(5);
            assertThat(result.riskDetails()).isNotEmpty();
            assertThat(result.calculationSteps()).isNotEmpty();
        }

        @Test
        @DisplayName("Should include mandatory risk in details")
        void shouldIncludeMandatoryRisk() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 1, List.of());

            // When
            var result = calculator.calculatePremiumWithDetails(request);

            // Then
            assertThat(result.riskDetails())
                    .anyMatch(r -> r.riskCode().equals("TRAVEL_MEDICAL"));
        }

        @Test
        @DisplayName("Should include optional risks in details")
        void shouldIncludeOptionalRisks() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();
            mockOptionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3"));
            mockOptionalRisk("LUGGAGE_LOSS", new BigDecimal("0.1"));

            var request = createRequest("5000", "ES", 1,
                    List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"));

            // When
            var result = calculator.calculatePremiumWithDetails(request);

            // Then
            assertThat(result.riskDetails())
                    .anyMatch(r -> r.riskCode().equals("SPORT_ACTIVITIES"))
                    .anyMatch(r -> r.riskCode().equals("LUGGAGE_LOSS"));
        }

        @Test
        @DisplayName("Should include calculation steps")
        void shouldIncludeCalculationSteps() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 5, List.of());

            // When
            var result = calculator.calculatePremiumWithDetails(request);

            // Then
            assertThat(result.calculationSteps()).hasSize(4); // без доп. рисков
            assertThat(result.calculationSteps().get(0).description())
                    .contains("Base rate");
        }
    }

    @Nested
    @DisplayName("Error Handling")
    class ErrorHandling {

        @Test
        @DisplayName("Should throw exception when medical level not found")
        void shouldThrowWhenMedicalLevelNotFound() {
            mockAge(30, new BigDecimal("1.0"));
            when(medicalLevelRepository.findActiveByCode(any(), any()))
                    .thenReturn(Optional.empty());

            var request = createRequest("INVALID", "ES", 5, List.of());

            // When/Then
            assertThatThrownBy(() -> calculator.calculatePremium(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Medical level not found");
        }

        @Test
        @DisplayName("Should throw exception when country not found")
        void shouldThrowWhenCountryNotFound() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            when(countryRepository.findActiveByIsoCode(any(), any()))
                    .thenReturn(Optional.empty());

            var request = createRequest("5000", "XX", 5, List.of());

            // When/Then
            assertThatThrownBy(() -> calculator.calculatePremium(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Country not found");
        }

        @Test
        @DisplayName("Should skip unknown optional risks")
        void shouldSkipUnknownOptionalRisks() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();
            mockOptionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3"));

            // Unknown risk возвращает empty
            when(riskTypeRepository.findActiveByCode(eq("UNKNOWN_RISK"), any()))
                    .thenReturn(Optional.empty());

            var request = createRequest("5000", "ES", 10,
                    List.of("SPORT_ACTIVITIES", "UNKNOWN_RISK"));

            // When - should not throw, just skip unknown risk
            var premium = calculator.calculatePremium(request);

            // Then - calculates as if only SPORT_ACTIVITIES selected
            assertThat(premium).isEqualByComparingTo("19.50");
        }

        @Test
        @DisplayName("Should ignore mandatory risks in selectedRisks")
        void shouldIgnoreMandatoryRisks() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));

            var mandatoryRisk = createMandatoryRiskEntity();
            when(riskTypeRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                    .thenReturn(Optional.of(mandatoryRisk));

            var request = createRequest("5000", "ES", 5,
                    List.of("TRAVEL_MEDICAL")); // Попытка добавить mandatory

            // When
            var premium = calculator.calculatePremium(request);

            // Then - должен игнорировать и не добавлять коэффициент
            assertThat(premium).isEqualByComparingTo("7.50");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Should handle null selected risks")
        void shouldHandleNullSelectedRisks() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 5, null);

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("7.50");
        }

        @Test
        @DisplayName("Should handle empty selected risks")
        void shouldHandleEmptySelectedRisks() {
            mockAge(30, new BigDecimal("1.0"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 5, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium).isEqualByComparingTo("7.50");
        }

        @Test
        @DisplayName("Should round to 2 decimal places")
        void shouldRoundToTwoDecimalPlaces() {
            // Given: расчёт даст 10.12345...
            mockAge(30, new BigDecimal("1.111"));
            mockMedicalLevel("5000", new BigDecimal("1.50"));
            mockCountry("ES", new BigDecimal("1.0"));
            mockMandatoryRisk();

            var request = createRequest("5000", "ES", 6, List.of());

            // When
            var premium = calculator.calculatePremium(request);

            // Then
            assertThat(premium.scale()).isEqualTo(2);
        }
    }

    // ========== HELPER METHODS ==========

    private void mockAge(int age, BigDecimal coefficient) {
        var ageResult = new AgeCalculator.AgeCalculationResult(
                age,
                coefficient,
                "Age group"
        );
        when(ageCalculator.calculateAge(any(), any())).thenReturn(age);
        when(ageCalculator.getAgeCoefficient(age)).thenReturn(coefficient);
        when(ageCalculator.calculateAgeAndCoefficient(any(), any()))
                .thenReturn(ageResult);
    }

    private void mockMedicalLevel(String code, BigDecimal dailyRate) {
        var entity = new MedicalRiskLimitLevelEntity();
        entity.setCode(code);
        entity.setDailyRate(dailyRate);
        entity.setCoverageAmount(new BigDecimal(code));

        when(medicalLevelRepository.findActiveByCode(eq(code), any()))
                .thenReturn(Optional.of(entity));
    }

    private void mockCountry(String isoCode, BigDecimal coefficient) {
        var entity = new CountryEntity();
        entity.setIsoCode(isoCode);
        entity.setNameEn(isoCode.equals("ES") ? "Spain" :
                isoCode.equals("TH") ? "Thailand" :
                        isoCode.equals("IN") ? "India" : "Country");
        entity.setRiskCoefficient(coefficient);

        when(countryRepository.findActiveByIsoCode(eq(isoCode), any()))
                .thenReturn(Optional.of(entity));
    }

    private void mockMandatoryRisk() {
        var entity = createMandatoryRiskEntity();
        when(riskTypeRepository.findActiveByCode(eq("TRAVEL_MEDICAL"), any()))
                .thenReturn(Optional.of(entity));
    }

    private void mockOptionalRisk(String code, BigDecimal coefficient) {
        var entity = new RiskTypeEntity();
        entity.setCode(code);
        entity.setNameEn(code.replace("_", " "));
        entity.setCoefficient(coefficient);
        entity.setIsMandatory(false);

        when(riskTypeRepository.findActiveByCode(eq(code), any()))
                .thenReturn(Optional.of(entity));
    }

    private RiskTypeEntity createMandatoryRiskEntity() {
        var entity = new RiskTypeEntity();
        entity.setCode("TRAVEL_MEDICAL");
        entity.setNameEn("Medical Coverage");
        entity.setCoefficient(BigDecimal.ZERO);
        entity.setIsMandatory(true);
        return entity;
    }

    private TravelCalculatePremiumRequestV2 createRequest(
            String level,
            String country,
            int days,
            List<String> risks) {

        LocalDate from = LocalDate.of(2025, 1, 1);
        LocalDate to = from.plusDays(days);

        return TravelCalculatePremiumRequestV2.builder()
                .medicalRiskLimitLevel(level)
                .countryIsoCode(country)
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(from)
                .agreementDateTo(to)
                .selectedRisks(risks)
                .build();
    }
}