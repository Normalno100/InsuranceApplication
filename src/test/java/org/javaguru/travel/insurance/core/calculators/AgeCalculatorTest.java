package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.AgeCoefficientEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.AgeCoefficientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AgeCalculator")
class AgeCalculatorTest {

    @Mock
    private AgeCoefficientRepository ageCoefficientRepository;

    @InjectMocks
    private AgeCalculator ageCalculator;

    // =========================================================
    // calculateAge
    // =========================================================

    @Nested
    @DisplayName("calculateAge()")
    class CalculateAgeTests {

        @Test
        @DisplayName("should calculate age correctly on birthday")
        void shouldCalculateAgeOnBirthday() {
            LocalDate birthDate = LocalDate.of(1990, 6, 15);
            LocalDate referenceDate = LocalDate.of(2025, 6, 15);

            int age = ageCalculator.calculateAge(birthDate, referenceDate);

            assertThat(age).isEqualTo(35);
        }

        @Test
        @DisplayName("should calculate age correctly one day before birthday")
        void shouldCalculateAgeDayBeforeBirthday() {
            LocalDate birthDate = LocalDate.of(1990, 6, 15);
            LocalDate referenceDate = LocalDate.of(2025, 6, 14);

            int age = ageCalculator.calculateAge(birthDate, referenceDate);

            assertThat(age).isEqualTo(34);
        }

        @Test
        @DisplayName("should calculate age correctly one day after birthday")
        void shouldCalculateAgeDayAfterBirthday() {
            LocalDate birthDate = LocalDate.of(1990, 6, 15);
            LocalDate referenceDate = LocalDate.of(2025, 6, 16);

            int age = ageCalculator.calculateAge(birthDate, referenceDate);

            assertThat(age).isEqualTo(35);
        }

        @Test
        @DisplayName("should return 0 for newborn")
        void shouldReturnZeroForNewborn() {
            LocalDate today = LocalDate.now();

            int age = ageCalculator.calculateAge(today, today);

            assertThat(age).isEqualTo(0);
        }

        @Test
        @DisplayName("should use current date when referenceDate is null")
        void shouldUseCurrentDateWhenReferenceDateIsNull() {
            LocalDate birthDate = LocalDate.now().minusYears(30);

            int age = ageCalculator.calculateAge(birthDate, null);

            assertThat(age).isEqualTo(30);
        }

        @Test
        @DisplayName("should throw exception when birthDate is null")
        void shouldThrowExceptionWhenBirthDateIsNull() {
            assertThatThrownBy(() -> ageCalculator.calculateAge(null, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth date cannot be null");
        }

        @Test
        @DisplayName("should throw exception when birthDate is in the future")
        void shouldThrowExceptionWhenBirthDateIsInFuture() {
            LocalDate futureBirthDate = LocalDate.now().plusDays(1);

            assertThatThrownBy(() -> ageCalculator.calculateAge(futureBirthDate, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Birth date cannot be in the future");
        }

        @Test
        @DisplayName("should calculate age across year boundary correctly")
        void shouldCalculateAgeAcrossYearBoundary() {
            LocalDate birthDate = LocalDate.of(1990, 12, 31);
            LocalDate referenceDate = LocalDate.of(2025, 1, 1);

            int age = ageCalculator.calculateAge(birthDate, referenceDate);

            assertThat(age).isEqualTo(34);
        }
    }

    // =========================================================
    // getAgeCoefficient — чтение из БД
    // =========================================================

    @Nested
    @DisplayName("getAgeCoefficient() — DB lookup")
    class GetAgeCoefficientFromDbTests {

        @ParameterizedTest(name = "age={0} → coefficient={1}")
        @CsvSource({
                "3,  1.10",
                "5,  1.10",
                "6,  0.90",
                "17, 0.90",
                "18, 1.00",
                "30, 1.00",
                "31, 1.10",
                "40, 1.10",
                "41, 1.30",
                "50, 1.30",
                "51, 1.60",
                "60, 1.60",
                "61, 2.00",
                "70, 2.00",
                "71, 2.50",
                "80, 2.50"
        })
        @DisplayName("should return coefficient from DB for all age ranges")
        void shouldReturnCoefficientFromDbForAllAgeRanges(int age, String expectedCoefficient) {
            LocalDate date = LocalDate.of(2025, 1, 1);
            AgeCoefficientEntity entity = entityWithCoefficient(new BigDecimal(expectedCoefficient));
            when(ageCoefficientRepository.findCoefficientForAge(age, date))
                    .thenReturn(Optional.of(entity));

            BigDecimal result = ageCalculator.getAgeCoefficient(age, date);

            assertThat(result).isEqualByComparingTo(new BigDecimal(expectedCoefficient));
        }

        @Test
        @DisplayName("should pass referenceDate to repository")
        void shouldPassReferenceDateToRepository() {
            LocalDate specificDate = LocalDate.of(2024, 6, 1);
            AgeCoefficientEntity entity = entityWithCoefficient(new BigDecimal("1.30"));
            when(ageCoefficientRepository.findCoefficientForAge(45, specificDate))
                    .thenReturn(Optional.of(entity));

            ageCalculator.getAgeCoefficient(45, specificDate);

            verify(ageCoefficientRepository).findCoefficientForAge(45, specificDate);
        }

        @Test
        @DisplayName("getAgeCoefficient(int) should use current date")
        void shouldUseCurrentDateInSingleArgOverload() {
            AgeCoefficientEntity entity = entityWithCoefficient(new BigDecimal("1.00"));
            when(ageCoefficientRepository.findCoefficientForAge(eq(25), any(LocalDate.class)))
                    .thenReturn(Optional.of(entity));

            BigDecimal result = ageCalculator.getAgeCoefficient(25);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1.00"));
        }

        @Test
        @DisplayName("should return exact BigDecimal value from DB entity")
        void shouldReturnExactBigDecimalFromDb() {
            LocalDate date = LocalDate.now();
            BigDecimal exactCoefficient = new BigDecimal("1.3500");
            AgeCoefficientEntity entity = entityWithCoefficient(exactCoefficient);
            when(ageCoefficientRepository.findCoefficientForAge(45, date))
                    .thenReturn(Optional.of(entity));

            BigDecimal result = ageCalculator.getAgeCoefficient(45, date);

            assertThat(result).isEqualByComparingTo(exactCoefficient);
        }
    }

    // =========================================================
    // getAgeCoefficient — fallback при пустом результате из БД
    // =========================================================

    @Nested
    @DisplayName("getAgeCoefficient() — fallback when DB is empty")
    class GetAgeCoefficientFallbackTests {

        @ParameterizedTest(name = "age={0} → fallback coefficient={1}")
        @CsvSource({
                "0,  1.1",
                "5,  1.1",
                "6,  0.9",
                "17, 0.9",
                "18, 1.0",
                "30, 1.0",
                "31, 1.1",
                "40, 1.1",
                "41, 1.3",
                "50, 1.3",
                "51, 1.6",
                "60, 1.6",
                "61, 2.0",
                "70, 2.0",
                "71, 2.5",
                "80, 2.5"
        })
        @DisplayName("should fall back to hardcoded values when DB returns empty")
        void shouldFallBackToHardcodedValuesWhenDbReturnsEmpty(int age, String expectedCoefficient) {
            LocalDate date = LocalDate.of(2025, 1, 1);
            when(ageCoefficientRepository.findCoefficientForAge(age, date))
                    .thenReturn(Optional.empty());

            BigDecimal result = ageCalculator.getAgeCoefficient(age, date);

            assertThat(result).isEqualByComparingTo(new BigDecimal(expectedCoefficient));
        }

        @Test
        @DisplayName("should consult DB before falling back")
        void shouldConsultDbBeforeFallingBack() {
            LocalDate date = LocalDate.now();
            when(ageCoefficientRepository.findCoefficientForAge(25, date))
                    .thenReturn(Optional.empty());

            ageCalculator.getAgeCoefficient(25, date);

            verify(ageCoefficientRepository, times(1)).findCoefficientForAge(25, date);
        }
    }

    // =========================================================
    // getAgeCoefficient — граничные и невалидные значения
    // =========================================================

    @Nested
    @DisplayName("getAgeCoefficient() — validation")
    class GetAgeCoefficientValidationTests {

        @Test
        @DisplayName("should throw exception for negative age")
        void shouldThrowExceptionForNegativeAge() {
            assertThatThrownBy(() -> ageCalculator.getAgeCoefficient(-1, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Age cannot be negative");

            verifyNoInteractions(ageCoefficientRepository);
        }

        @Test
        @DisplayName("should throw exception for age over 80")
        void shouldThrowExceptionForAgeOver80() {
            assertThatThrownBy(() -> ageCalculator.getAgeCoefficient(81, LocalDate.now()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Insurance not available for persons over 80 years old");

            verifyNoInteractions(ageCoefficientRepository);
        }

        @Test
        @DisplayName("should accept age 0 — boundary")
        void shouldAcceptAgeZero() {
            LocalDate date = LocalDate.now();
            when(ageCoefficientRepository.findCoefficientForAge(0, date))
                    .thenReturn(Optional.of(entityWithCoefficient(new BigDecimal("1.10"))));

            BigDecimal result = ageCalculator.getAgeCoefficient(0, date);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1.10"));
        }

        @Test
        @DisplayName("should accept age 80 — boundary")
        void shouldAcceptAge80() {
            LocalDate date = LocalDate.now();
            when(ageCoefficientRepository.findCoefficientForAge(80, date))
                    .thenReturn(Optional.of(entityWithCoefficient(new BigDecimal("2.50"))));

            BigDecimal result = ageCalculator.getAgeCoefficient(80, date);

            assertThat(result).isEqualByComparingTo(new BigDecimal("2.50"));
        }
    }

    // =========================================================
    // getAgeGroupDescription
    // =========================================================

    @Nested
    @DisplayName("getAgeGroupDescription()")
    class GetAgeGroupDescriptionTests {

        @ParameterizedTest(name = "age={0} → \"{1}\"")
        @CsvSource({
                "0,  Infants and toddlers",
                "5,  Infants and toddlers",
                "6,  Children and teenagers",
                "17, Children and teenagers",
                "18, Young adults",
                "30, Young adults",
                "31, Adults",
                "40, Adults",
                "41, Middle-aged",
                "50, Middle-aged",
                "51, Senior",
                "60, Senior",
                "61, Elderly",
                "70, Elderly",
                "71, Very elderly",
                "80, Very elderly"
        })
        @DisplayName("should return correct description for each age range")
        void shouldReturnCorrectDescriptionForEachAgeRange(int age, String expectedDescription) {
            assertThat(ageCalculator.getAgeGroupDescription(age)).isEqualTo(expectedDescription);
        }
    }

    // =========================================================
    // isAgeValid
    // =========================================================

    @Nested
    @DisplayName("isAgeValid()")
    class IsAgeValidTests {

        @ParameterizedTest(name = "age={0} → valid={1}")
        @CsvSource({
                "0,   true",
                "40,  true",
                "80,  true",
                "-1,  false",
                "81,  false",
                "100, false"
        })
        @DisplayName("should validate age range correctly")
        void shouldValidateAgeRangeCorrectly(int age, boolean expectedValid) {
            assertThat(ageCalculator.isAgeValid(age)).isEqualTo(expectedValid);
        }
    }

    // =========================================================
    // calculateAgeAndCoefficient
    // =========================================================

    @Nested
    @DisplayName("calculateAgeAndCoefficient()")
    class CalculateAgeAndCoefficientTests {

        @Test
        @DisplayName("should return correct age in result")
        void shouldReturnCorrectAgeInResult() {
            LocalDate birthDate = LocalDate.of(1990, 1, 1);
            LocalDate referenceDate = LocalDate.of(2025, 6, 1);
            stubRepository(35, referenceDate, new BigDecimal("1.10"));

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            assertThat(result.age()).isEqualTo(35);
        }

        @Test
        @DisplayName("should return coefficient from DB for calculated age")
        void shouldReturnCoefficientFromDbForCalculatedAge() {
            LocalDate birthDate = LocalDate.of(1975, 3, 10);
            LocalDate referenceDate = LocalDate.of(2025, 3, 10); // ровно 50 лет
            stubRepository(50, referenceDate, new BigDecimal("1.30"));

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            assertThat(result.coefficient()).isEqualByComparingTo(new BigDecimal("1.30"));
        }

        @Test
        @DisplayName("should return correct description in result")
        void shouldReturnCorrectDescriptionInResult() {
            LocalDate birthDate = LocalDate.of(2000, 1, 1);
            LocalDate referenceDate = LocalDate.of(2025, 1, 1); // 25 лет
            stubRepository(25, referenceDate, new BigDecimal("1.00"));

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            assertThat(result.description()).isEqualTo("Young adults");
        }

        @Test
        @DisplayName("should pass referenceDate to repository — temporal correctness")
        void shouldPassReferenceDateToRepositoryForTemporalCorrectness() {
            LocalDate birthDate = LocalDate.of(1980, 5, 1);
            LocalDate referenceDate = LocalDate.of(2026, 1, 1); // 45 лет
            stubRepository(45, referenceDate, new BigDecimal("1.30"));

            ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            verify(ageCoefficientRepository).findCoefficientForAge(45, referenceDate);
        }

        @Test
        @DisplayName("should use fallback when DB is empty")
        void shouldUseFallbackWhenDbIsEmpty() {
            LocalDate birthDate = LocalDate.of(1960, 1, 1);
            LocalDate referenceDate = LocalDate.of(2025, 1, 1); // 65 лет
            when(ageCoefficientRepository.findCoefficientForAge(65, referenceDate))
                    .thenReturn(Optional.empty());

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            assertThat(result.coefficient()).isEqualByComparingTo(new BigDecimal("2.0"));
            assertThat(result.age()).isEqualTo(65);
            assertThat(result.description()).isEqualTo("Elderly");
        }

        @Test
        @DisplayName("result record should expose age, coefficient and description")
        void resultRecordShouldExposeAllFields() {
            LocalDate birthDate = LocalDate.of(1955, 7, 20);
            LocalDate referenceDate = LocalDate.of(2025, 7, 20); // 70 лет
            stubRepository(70, referenceDate, new BigDecimal("2.00"));

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, referenceDate);

            assertThat(result.age()).isEqualTo(70);
            assertThat(result.coefficient()).isEqualByComparingTo(new BigDecimal("2.00"));
            assertThat(result.description()).isEqualTo("Elderly");
        }
    }

    // =========================================================
    // Вспомогательные методы
    // =========================================================

    private AgeCoefficientEntity entityWithCoefficient(BigDecimal coefficient) {
        AgeCoefficientEntity entity = new AgeCoefficientEntity();
        entity.setCoefficient(coefficient);
        return entity;
    }

    private void stubRepository(int age, LocalDate date, BigDecimal coefficient) {
        when(ageCoefficientRepository.findCoefficientForAge(age, date))
                .thenReturn(Optional.of(entityWithCoefficient(coefficient)));
    }
}