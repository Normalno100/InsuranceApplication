package org.javaguru.travel.insurance.application.validation.business;

import org.javaguru.travel.insurance.TestConstants;
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
 * Тесты для бизнес-валидаторов.
 *
 * ЭТАП 1 (рефакторинг): Фиксация дат через Clock.
 *
 * БЫЛО:
 *   private final ValidationContext context = new ValidationContext();
 *   LocalDate.now().minusYears(35)  — нестабильно
 *
 * СТАЛО:
 *   private static final LocalDate TODAY = TestConstants.TEST_DATE;  // 2026-03-18
 *   private final ValidationContext context = new ValidationContext(TestConstants.TEST_CLOCK);
 *   TODAY.minusYears(35)  — всегда 1991-03-18
 */
@DisplayName("Business Validators")
class BusinessValidatorsTest {

    private static final LocalDate TODAY = TestConstants.TEST_DATE; // 2026-03-18

    private final ValidationContext context =
            new ValidationContext(TestConstants.TEST_CLOCK);

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
        @DisplayName("Должен пройти когда дата рождения 30 лет назад")
        void shouldPassWhenBirthDateInPast() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.minusYears(30)) // 1996-03-18
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
            assertThat(result.getErrors()).isEmpty();
        }

        @Test
        @DisplayName("Должен провалиться когда дата рождения сегодня")
        void shouldFailWhenBirthDateIsToday() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY) // 2026-03-18
                    .build();

            var result = validator.validate(request, context);

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
        @DisplayName("Должен провалиться когда дата рождения завтра")
        void shouldFailWhenBirthDateInFuture() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.plusDays(1))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("Должен пройти когда дата рождения null (проверяется другим валидатором)")
        void shouldPassWhenBirthDateIsNull() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(null)
                    .build();

            var result = validator.validate(request, context);

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
        @DisplayName("Должен пройти когда dateTo на 14 дней позже dateFrom")
        void shouldPassWhenValidOrder() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(TODAY.plusDays(44))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Должен пройти когда даты совпадают (однодневная поездка)")
        void shouldPassWhenDatesEqual() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Должен провалиться когда dateTo раньше dateFrom")
        void shouldFailWhenInvalidOrder() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(44))
                    .agreementDateTo(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> {
                        assertThat(error.getField()).isEqualTo("agreementDateTo");
                        assertThat(error.getMessage())
                                .contains("agreementDateTo must be greater than or equal to agreementDateFrom!");
                    });
        }

        @Test
        @DisplayName("Должен пройти когда одна из дат null")
        void shouldPassWhenOneDateNull() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(null)
                    .build();

            var result = validator.validate(request, context);

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
        @DisplayName("Должен пройти и сохранить возраст в контексте для 35-летнего")
        void shouldPassAndStoreAgeForAdult() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.minusYears(35)) // 1991-03-18
                    .agreementDateFrom(TODAY.plusDays(30)) // 2026-04-17
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
            assertThat(context.getAttribute("personAge", Integer.class))
                    .isPresent()
                    .hasValue(35);
        }

        @Test
        @DisplayName("Должен провалиться когда birthDate позже agreementDateFrom — возраст отрицательный")
        void shouldFailWhenAgeNegative() {
            // AgeValidator вычисляет: age = Period.between(birthDate, agreementDateFrom).getYears()
            // Для отрицательного возраста birthDate должен быть ПОЗЖЕ agreementDateFrom.
            //
            // Неверный вариант: TODAY.plusYears(1) vs TODAY.plusDays(30)
            //   birthDate = 2027-03-18, agreementDateFrom = 2026-04-17
            //   → birthDate > agreementDateFrom → age отрицательный
            //   НО: если Period.between возвращает 0 в getYears() (только месяцы/дни),
            //   то проверка age < 0 не сработает.
            //
            // Надёжный вариант: birthDate на несколько лет позже agreementDateFrom:
            //   birthDate = TODAY + 2 года = 2028-03-18
            //   agreementDateFrom = TODAY + 30 дней = 2026-04-17
            //   Period.between(2028-03-18, 2026-04-17).getYears() = -2
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.plusYears(2))   // 2028-03-18 — на 2 года позже
                    .agreementDateFrom(TODAY.plusDays(30)) // 2026-04-17
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error ->
                            assertThat(error.getMessage())
                                    .contains("Person age must be at least 0 years!"));
        }

        @Test
        @DisplayName("Должен провалиться когда возраст превышает максимум (80 лет)")
        void shouldFailWhenAgeTooHigh() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.minusYears(85))  // 1941-03-18 — 85 лет
                    .agreementDateFrom(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> assertThat(error.getMessage()).contains("80"));
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 1, 18, 35, 65, 75, 80})
        @DisplayName("Должен пройти для всех допустимых возрастов от 0 до 80")
        void shouldPassForValidAges(int age) {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.minusYears(age))
                    .agreementDateFrom(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid())
                    .as("Возраст %d должен быть допустимым", age)
                    .isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {81, 85, 90, 100})
        @DisplayName("Должен провалиться для возрастов выше максимума (80)")
        void shouldFailForInvalidAges(int age) {
            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(TODAY.minusYears(age))
                    .agreementDateFrom(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid())
                    .as("Возраст %d должен быть недопустимым", age)
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
        @DisplayName("Должен пройти и сохранить длительность для 14-дневной поездки")
        void shouldPassAndStoreDurationForStandardTrip() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(TODAY.plusDays(43))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
            assertThat(context.getAttribute("tripDuration", Long.class))
                    .isPresent()
                    .get()
                    .satisfies(duration -> {
                        assertThat(duration).isGreaterThan(0L);
                        assertThat(duration).isLessThanOrEqualTo(14L);
                    });
        }

        @Test
        @DisplayName("Должен пройти для однодневной поездки")
        void shouldPassForSingleDayTrip() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(TODAY.plusDays(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Должен провалиться когда длительность превышает максимум (366 дней)")
        void shouldFailWhenDurationTooLong() {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(10))
                    .agreementDateTo(TODAY.plusDays(10).plusDays(366))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors())
                    .hasSize(1)
                    .first()
                    .satisfies(error -> assertThat(error.getMessage()).contains("365"));
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 7, 14, 30, 60, 90, 180, 365})
        @DisplayName("Должен пройти для всех допустимых длительностей от 1 до 365 дней")
        void shouldPassForValidDurations(int days) {
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(TODAY.plusDays(30))
                    .agreementDateTo(TODAY.plusDays(30).plusDays(days - 1))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid())
                    .as("Длительность %d дней должна быть допустимой", days)
                    .isTrue();
        }
    }
}