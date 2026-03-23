package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.AgeCoefficientEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.AgeCoefficientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

/**
 * Тесты переключателя возрастного коэффициента (task_116).
 *
 * ЭТАП 6 (рефакторинг): Добавление отсутствующих тестов.
 *
 * ПОКРЫВАЕМ:
 *   - getAgeCoefficient(age, date, enabled=true) применяет коэффициент из БД
 *   - getAgeCoefficient(age, date, enabled=false) всегда возвращает 1.0
 *   - calculateAgeAndCoefficient() с флагом enabled/disabled
 *   - PersonAgeCalculator делегирует флаг в AgeCalculator
 *
 * ОПОРНАЯ ДАТА: TestConstants.TEST_DATE = 2026-03-18
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AgeCoefficientSwitch — task_116")
class AgeCoefficientSwitchTest {

    @Mock
    private AgeCoefficientRepository ageCoefficientRepository;

    @InjectMocks
    private AgeCalculator ageCalculator;

    private static final LocalDate AGREEMENT_DATE = TestConstants.TEST_DATE.plusDays(30); // 2026-04-17

    // ── enabled=true ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Когда enabled=true")
    class WhenEnabled {

        @Test
        @DisplayName("должен вернуть коэффициент из БД")
        void shouldReturnCoefficientFromDbWhenEnabled() {
            BigDecimal dbCoefficient = new BigDecimal("1.30");
            when(ageCoefficientRepository.findCoefficientForAge(45, AGREEMENT_DATE))
                    .thenReturn(Optional.of(entityWithCoefficient(dbCoefficient)));

            BigDecimal result = ageCalculator.getAgeCoefficient(45, AGREEMENT_DATE, true);

            assertThat(result).isEqualByComparingTo(dbCoefficient);
        }

        @Test
        @DisplayName("должен обращаться к репозиторию когда enabled=true")
        void shouldCallRepositoryWhenEnabled() {
            when(ageCoefficientRepository.findCoefficientForAge(anyInt(), any()))
                    .thenReturn(Optional.empty());

            ageCalculator.getAgeCoefficient(35, AGREEMENT_DATE, true);

            verify(ageCoefficientRepository).findCoefficientForAge(35, AGREEMENT_DATE);
        }

        @Test
        @DisplayName("должен использовать fallback когда БД пустая, enabled=true")
        void shouldUseFallbackWhenDbEmptyAndEnabled() {
            when(ageCoefficientRepository.findCoefficientForAge(anyInt(), any()))
                    .thenReturn(Optional.empty());

            // Возраст 35 → Adults 31-40 → fallback коэффициент 1.1
            BigDecimal result = ageCalculator.getAgeCoefficient(35, AGREEMENT_DATE, true);

            assertThat(result).isEqualByComparingTo(new BigDecimal("1.1"));
        }
    }

    // ── enabled=false ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Когда enabled=false (коэффициент отключён)")
    class WhenDisabled {

        @Test
        @DisplayName("должен вернуть 1.0 для молодого возраста")
        void shouldReturnOneForYoungAge() {
            BigDecimal result = ageCalculator.getAgeCoefficient(25, AGREEMENT_DATE, false);

            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("должен вернуть 1.0 для пожилого возраста (коэффициент 2.5 отключён)")
        void shouldReturnOneForElderlyAge() {
            BigDecimal result = ageCalculator.getAgeCoefficient(75, AGREEMENT_DATE, false);

            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("должен вернуть 1.0 для возраста с повышенным риском (51-60)")
        void shouldReturnOneForHighRiskAge() {
            BigDecimal result = ageCalculator.getAgeCoefficient(55, AGREEMENT_DATE, false);

            assertThat(result).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("НЕ должен обращаться к репозиторию когда enabled=false")
        void shouldNotCallRepositoryWhenDisabled() {
            ageCalculator.getAgeCoefficient(45, AGREEMENT_DATE, false);

            verifyNoInteractions(ageCoefficientRepository);
        }

        @Test
        @DisplayName("должен вернуть 1.0 для любого допустимого возраста")
        void shouldAlwaysReturnOneForAnyAge() {
            int[] ages = {0, 5, 17, 18, 30, 31, 40, 41, 50, 51, 60, 61, 70, 71, 80};

            for (int age : ages) {
                BigDecimal result = ageCalculator.getAgeCoefficient(age, AGREEMENT_DATE, false);
                assertThat(result)
                        .as("Для возраста %d при disabled=false ожидается 1.0", age)
                        .isEqualByComparingTo(BigDecimal.ONE);
            }

            verifyNoInteractions(ageCoefficientRepository);
        }
    }

    // ── calculateAgeAndCoefficient ────────────────────────────────────────────

    @Nested
    @DisplayName("calculateAgeAndCoefficient() с флагом enabled")
    class CalculateAgeAndCoefficientWithFlag {

        @Test
        @DisplayName("enabled=false: коэффициент в результате равен 1.0")
        void shouldReturnOneCoefficientWhenDisabled() {
            LocalDate birthDate = TestConstants.TEST_DATE.minusYears(35); // 35 лет

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE, false);

            assertThat(result.coefficient()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(result.age()).isEqualTo(35);
        }

        @Test
        @DisplayName("enabled=true: коэффициент берётся из БД")
        void shouldReturnDbCoefficientWhenEnabled() {
            LocalDate birthDate = TestConstants.TEST_DATE.minusYears(55); // 55 лет → Senior 1.6
            when(ageCoefficientRepository.findCoefficientForAge(anyInt(), any()))
                    .thenReturn(Optional.of(entityWithCoefficient(new BigDecimal("1.60"))));

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE, true);

            assertThat(result.coefficient()).isEqualByComparingTo(new BigDecimal("1.60"));
        }

        @Test
        @DisplayName("enabled=false: возраст и описание группы сохраняются корректно")
        void shouldPreserveAgeAndDescriptionWhenDisabled() {
            LocalDate birthDate = TestConstants.TEST_DATE.minusYears(25); // 25 лет

            AgeCalculator.AgeCalculationResult result =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE, false);

            assertThat(result.age()).isEqualTo(25);
            assertThat(result.description()).isEqualTo("Young adults");
            assertThat(result.coefficient()).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("enabled=false → НЕ обращается к репозиторию")
        void shouldNotCallRepositoryWhenDisabledInCalculate() {
            LocalDate birthDate = TestConstants.TEST_DATE.minusYears(65);

            ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE, false);

            verifyNoInteractions(ageCoefficientRepository);
        }

        @Test
        @DisplayName("enabled=true (дефолт) работает так же как enabled=true явно")
        void defaultCalculateEqualsEnabledTrue() {
            LocalDate birthDate = TestConstants.TEST_DATE.minusYears(35);
            BigDecimal dbCoeff = new BigDecimal("1.10");

            when(ageCoefficientRepository.findCoefficientForAge(anyInt(), any()))
                    .thenReturn(Optional.of(entityWithCoefficient(dbCoeff)));

            AgeCalculator.AgeCalculationResult withDefault =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE);
            AgeCalculator.AgeCalculationResult withTrue =
                    ageCalculator.calculateAgeAndCoefficient(birthDate, AGREEMENT_DATE, true);

            assertThat(withDefault.coefficient()).isEqualByComparingTo(withTrue.coefficient());
        }
    }

    // ── Влияние на расчёт премии ──────────────────────────────────────────────

    @Nested
    @DisplayName("Влияние на расчёт финальной премии")
    class PremiumImpact {

        @Test
        @DisplayName("disabled: премия не зависит от возраста (коэффициент=1.0 для всех)")
        void premiumShouldBeAgeIndependentWhenDisabled() {
            // При disabled все возрасты дают коэффициент 1.0
            BigDecimal young = ageCalculator.getAgeCoefficient(18, AGREEMENT_DATE, false);
            BigDecimal adult = ageCalculator.getAgeCoefficient(45, AGREEMENT_DATE, false);
            BigDecimal elderly = ageCalculator.getAgeCoefficient(75, AGREEMENT_DATE, false);

            assertThat(young).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(adult).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(elderly).isEqualByComparingTo(BigDecimal.ONE);
        }

        @Test
        @DisplayName("enabled: пожилой (75 лет) дороже молодого (25 лет)")
        void elderlyPremiumShouldBeHigherThanYoungWhenEnabled() {
            when(ageCoefficientRepository.findCoefficientForAge(eq(25), any()))
                    .thenReturn(Optional.of(entityWithCoefficient(new BigDecimal("1.00"))));
            when(ageCoefficientRepository.findCoefficientForAge(eq(75), any()))
                    .thenReturn(Optional.of(entityWithCoefficient(new BigDecimal("2.50"))));

            BigDecimal youngCoeff  = ageCalculator.getAgeCoefficient(25, AGREEMENT_DATE, true);
            BigDecimal elderlyCoeff = ageCalculator.getAgeCoefficient(75, AGREEMENT_DATE, true);

            assertThat(elderlyCoeff).isGreaterThan(youngCoeff);
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private AgeCoefficientEntity entityWithCoefficient(BigDecimal coefficient) {
        AgeCoefficientEntity entity = new AgeCoefficientEntity();
        entity.setCoefficient(coefficient);
        return entity;
    }
}