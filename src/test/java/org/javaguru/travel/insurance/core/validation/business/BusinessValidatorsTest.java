package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.ValidationContext;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Business Validators Tests")
class BusinessValidatorsTest {

    private final ValidationContext context = new ValidationContext();

    // ========== DATE IN PAST VALIDATOR ==========

    @Nested
    @DisplayName("DateInPastValidator")
    class DateInPastValidatorTest {

        @Test
        @DisplayName("should pass when date is in the past")
        void shouldPassWhenDateInPast() {
            var validator = new DateInPastValidator<>(
                    "personBirthDate",
                    TravelCalculatePremiumRequest::getPersonBirthDate
            );

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(30))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should fail when date is today")
        void shouldFailWhenDateIsToday() {
            var validator = new DateInPastValidator<>(
                    "personBirthDate",
                    TravelCalculatePremiumRequest::getPersonBirthDate
            );

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now())
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("in the past");
        }

        @Test
        @DisplayName("should fail when date is in future")
        void shouldFailWhenDateInFuture() {
            var validator = new DateInPastValidator<>(
                    "personBirthDate",
                    TravelCalculatePremiumRequest::getPersonBirthDate
            );

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().plusDays(1))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should pass when date is null (handled by other validators)")
        void shouldPassWhenNull() {
            var validator = new DateInPastValidator<>(
                    "personBirthDate",
                    TravelCalculatePremiumRequest::getPersonBirthDate
            );

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(null)
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }
    }

    // ========== DATE RANGE VALIDATOR ==========

    @Nested
    @DisplayName("DateRangeValidator")
    class DateRangeValidatorTest {

        @Test
        @DisplayName("should pass when dateTo is after dateFrom")
        void shouldPassWhenValidOrder() {
            var validator = new DateRangeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(LocalDate.of(2025, 6, 15))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when dates are equal")
        void shouldPassWhenDatesEqual() {
            var validator = new DateRangeValidator();

            var date = LocalDate.of(2025, 6, 1);
            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(date)
                    .agreementDateTo(date)
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should fail when dateTo is before dateFrom")
        void shouldFailWhenInvalidOrder() {
            var validator = new DateRangeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 15))
                    .agreementDateTo(LocalDate.of(2025, 6, 1))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getField()).isEqualTo("agreementDateTo");
        }

        @Test
        @DisplayName("should pass when one date is null")
        void shouldPassWhenOneDateNull() {
            var validator = new DateRangeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(null)
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }
    }

    // ========== AGE VALIDATOR ==========

    @Nested
    @DisplayName("AgeValidator")
    class AgeValidatorTest {

        @Test
        @DisplayName("should pass for valid age")
        void shouldPassForValidAge() {
            var validator = new AgeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(30))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should store age in context")
        void shouldStoreAgeInContext() {
            var validator = new AgeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(30))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            validator.validate(request, context);

            assertThat(context.getAttribute("personAge", Integer.class))
                    .isPresent()
                    .hasValue(30);
        }

        @Test
        @DisplayName("should fail when age is negative")
        void shouldFailWhenAgeNegative() {
            var validator = new AgeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().plusYears(1))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should fail when age exceeds maximum")
        void shouldFailWhenAgeTooHigh() {
            var validator = new AgeValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .personBirthDate(LocalDate.now().minusYears(85))
                    .agreementDateFrom(LocalDate.now())
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("80");
        }
    }

    // ========== TRIP DURATION VALIDATOR ==========

    @Nested
    @DisplayName("TripDurationValidator")
    class TripDurationValidatorTest {

        @Test
        @DisplayName("should pass for valid duration")
        void shouldPassForValidDuration() {
            var validator = new TripDurationValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(LocalDate.of(2025, 6, 15))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should store duration in context")
        void shouldStoreDurationInContext() {
            var validator = new TripDurationValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(LocalDate.of(2025, 6, 15))
                    .build();

            validator.validate(request, context);

            assertThat(context.getAttribute("tripDuration", Long.class))
                    .isPresent()
                    .get()
                    .satisfies(duration -> assertThat(duration).isGreaterThan(0));
        }

        @Test
        @DisplayName("should fail when duration exceeds maximum")
        void shouldFailWhenDurationTooLong() {
            var validator = new TripDurationValidator();

            var request = TravelCalculatePremiumRequest.builder()
                    .agreementDateFrom(LocalDate.of(2025, 1, 1))
                    .agreementDateTo(LocalDate.of(2026, 1, 1))
                    .build();

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors().get(0).getMessage()).contains("365");
        }
    }
}