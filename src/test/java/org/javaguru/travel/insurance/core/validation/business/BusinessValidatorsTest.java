package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.rule.business.AgeValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.DateInPastValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.DateRangeValidator;
import org.javaguru.travel.insurance.application.validation.rule.business.TripDurationValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Улучшенные тесты для бизнес-валидаторов
 *
 * ЭТО ПРИМЕР ХОРОШИХ UNIT-ТЕСТОВ:
 * - Нет моков (валидаторы не имеют зависимостей)
 * - Явные тестовые данные
 * - Фокус на поведении
 * - Группировка через @Nested
 * - Параметризованные тесты где уместно
 */
@DisplayName("Business Validators")
class BusinessValidatorsTest extends BaseTestFixture {

    private final ValidationContext context = new ValidationContext();

    // ==========================================
    // DATE IN PAST VALIDATOR
    // ==========================================

    @Nested
    @DisplayName("DateInPastValidator")
    class DateInPastValidatorTests {

        private final DateInPastValidator<TravelCalculatePremiumRequest> validator =
                new DateInPastValidator<>(
                        "personBirthDate",
                        TravelCalculatePremiumRequest::getPersonBirthDate
                );

        @Test
        @DisplayName("Should pass when birth date is 30 years ago")
        void shouldPassWhenBirthDateInPast() {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(30))
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Should fail when birth date is today")
        void shouldFailWhenBirthDateIsToday() {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getField()).isEqualTo("personBirthDate");
                        assertThat(error.getMessage()).contains("must be in the past");
                    });
        }

        @Test
        @DisplayName("Should fail when birth date is tomorrow")
        void shouldFailWhenBirthDateInFuture() {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().plusDays(1))
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Should pass when birth date is null (handled by structural validators)")
        void shouldPassWhenBirthDateIsNull() {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(null)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then - бизнес-валидатор не проверяет null
            assertThat(result.isValid()).isTrue();
        }
    }

    // ==========================================
    // DATE RANGE VALIDATOR
    // ==========================================

    @Nested
    @DisplayName("DateRangeValidator")
    class DateRangeValidatorTests {

        private final DateRangeValidator validator = new DateRangeValidator();

        @Test
        @DisplayName("Should pass when dateTo is 14 days after dateFrom")
        void shouldPassWhenValidOrder() {
            // Given
            LocalDate from = LocalDate.of(2025, 6, 1);
            LocalDate to = LocalDate.of(2025, 6, 15);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(from)
                    .agreementDateTo(to)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should pass when dates are equal (single day trip)")
        void shouldPassWhenDatesEqual() {
            // Given - поездка на один день
            LocalDate date = LocalDate.of(2025, 6, 1);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(date)
                    .agreementDateTo(date)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should fail when dateTo is before dateFrom")
        void shouldFailWhenInvalidOrder() {
            // Given - даты в неправильном порядке
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 15))
                    .agreementDateTo(LocalDate.of(2025, 6, 1))
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getField()).isEqualTo("agreementDateTo");
                        assertThat(error.getMessage()).contains("agreementDateTo must be greater than or equal to agreementDateFrom!");
                    });
        }

        @Test
        @DisplayName("Should pass when one date is null")
        void shouldPassWhenOneDateNull() {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(null)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
        }
    }

    // ==========================================
    // AGE VALIDATOR
    // ==========================================

    @Nested
    @DisplayName("AgeValidator")
    class AgeValidatorTests {

        private final AgeValidator validator = new AgeValidator();

        @Test
        @DisplayName("Should pass and store age in context for 30-year-old person")
        void shouldPassAndStoreAgeForAdult() {
            // Given - человек 30 лет
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(30))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();

            // Проверяем что возраст сохранен в контексте
            assertThat(context.getAttribute("personAge", Integer.class))
                    .isPresent()
                    .hasValue(30);
        }

        @Test
        @DisplayName("Should fail when person is not born yet (negative age)")
        void shouldFailWhenAgeNegative() {
            // Given - дата рождения в будущем
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().plusYears(1))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getMessage()).contains("Person age must be at least 0 years!");
                    });
        }

        @Test
        @DisplayName("Should fail when age exceeds maximum allowed (80 years)")
        void shouldFailWhenAgeTooHigh() {
            // Given - человек 85 лет (превышает максимум)
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(85))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getMessage()).contains("80");
                    });
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 18, 35, 65, 75, 80})
        @DisplayName("Should pass for all valid ages from 0 to 80")
        void shouldPassForValidAges(int age) {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(age))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid())
                    .as("Age %d should be valid", age)
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {81, 85, 90, 100})
        @DisplayName("Should fail for ages above maximum (80)")
        void shouldFailForInvalidAges(int age) {
            // Given
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(age))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid())
                    .as("Age %d should be invalid", age)
                    .isFalse();
        }
    }

    // ==========================================
    // TRIP DURATION VALIDATOR
    // ==========================================

    @Nested
    @DisplayName("TripDurationValidator")
    class TripDurationValidatorTests {

        private final TripDurationValidator validator = new TripDurationValidator();

        @Test
        @DisplayName("Should pass and store duration for 14-day trip")
        void shouldPassAndStoreDurationForStandardTrip() {
            // Given - поездка на 14 дней
            LocalDate from = LocalDate.of(2025, 6, 2);
            LocalDate to = LocalDate.of(2025, 6, 15);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(from)
                    .agreementDateTo(to)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();

            // Проверяем что длительность сохранена
            assertThat(context.getAttribute("tripDuration", Long.class))
                    .isPresent()
                    .get()
                    .satisfies(duration -> {
                        assertThat(duration).isGreaterThan(0L);
                        assertThat(duration).isLessThanOrEqualTo(14L);
                    });
        }

        @Test
        @DisplayName("Should pass for single day trip")
        void shouldPassForSingleDayTrip() {
            // Given - поездка на 1 день
            LocalDate date = LocalDate.of(2025, 6, 1);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(date)
                    .agreementDateTo(date)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should pass for maximum allowed duration (365 days)")
        void shouldPassForMaximumDuration() {
            // Given - поездка ровно на 365 дней
            LocalDate from = LocalDate.of(2025, 1, 2);
            LocalDate to = LocalDate.of(2026, 1, 1);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(from)
                    .agreementDateTo(to)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should fail when duration exceeds maximum (366 days)")
        void shouldFailWhenDurationTooLong() {
            // Given - поездка больше года
            LocalDate from = LocalDate.of(2025, 1, 1);
            LocalDate to = LocalDate.of(2026, 1, 3); // 367 дней

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(from)
                    .agreementDateTo(to)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getMessage()).contains("365");
                    });
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 7, 14, 30, 60, 90, 180, 365})
        @DisplayName("Should pass for all valid durations from 1 to 365 days")
        void shouldPassForValidDurations(int days) {
            // Given
            LocalDate from = LocalDate.of(2025, 6, 1);
            LocalDate to = from.plusDays(days - 1);

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(from)
                    .agreementDateTo(to)
                    .build();

            // When
            var result = validator.validate(request, context);

            // Then
            assertThat(result.isValid())
                    .as("Duration of %d days should be valid", days)
                    .isTrue();
        }
    }
}