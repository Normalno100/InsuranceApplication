package org.javaguru.travel.insurance.application.validation.domain.person;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для PersonValidator.
 *
 * task_136: Проверяем что доменный валидатор корректно изолирует
 * правила валидации персональных данных.
 */
@DisplayName("PersonValidator")
class PersonValidatorTest {

    private PersonValidator validator;
    private ValidationContext context;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;        // 2026-03-18
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);         // 2026-04-17

    @BeforeEach
    void setUp() {
        validator = new PersonValidator();
        context = new ValidationContext(TestConstants.TEST_CLOCK);
    }

    // ── personFirstName ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("personFirstName validation")
    class PersonFirstNameTests {

        @Test
        @DisplayName("должен вернуть ошибку когда personFirstName null")
        void shouldFailWhenFirstNameNull() {
            var request = validRequest();
            request.setPersonFirstName(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personFirstName".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда personFirstName пустой")
        void shouldFailWhenFirstNameBlank() {
            var request = validRequest();
            request.setPersonFirstName("   ");

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personFirstName".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда personFirstName длиннее 100 символов")
        void shouldFailWhenFirstNameTooLong() {
            var request = validRequest();
            request.setPersonFirstName("A".repeat(101));

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personFirstName".equals(e.getField()));
        }
    }

    // ── personLastName ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("personLastName validation")
    class PersonLastNameTests {

        @Test
        @DisplayName("должен вернуть ошибку когда personLastName null")
        void shouldFailWhenLastNameNull() {
            var request = validRequest();
            request.setPersonLastName(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personLastName".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда personLastName пустой")
        void shouldFailWhenLastNameBlank() {
            var request = validRequest();
            request.setPersonLastName("");

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personLastName".equals(e.getField()));
        }
    }

    // ── personBirthDate ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("personBirthDate validation")
    class PersonBirthDateTests {

        @Test
        @DisplayName("должен вернуть ошибку когда personBirthDate null")
        void shouldFailWhenBirthDateNull() {
            var request = validRequest();
            request.setPersonBirthDate(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personBirthDate".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда personBirthDate сегодня (не в прошлом)")
        void shouldFailWhenBirthDateIsToday() {
            var request = validRequest();
            request.setPersonBirthDate(TODAY); // не в прошлом

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personBirthDate".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда возраст превышает 80 лет")
        void shouldFailWhenAgeOver80() {
            var request = validRequest();
            request.setPersonBirthDate(TODAY.minusYears(85));

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> "personBirthDate".equals(e.getField()));
        }
    }

    // ── Valid case ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("должен пройти валидацию для корректных персональных данных")
    void shouldPassForValidPersonData() {
        var request = validRequest();

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("PersonValidator не проверяет countryIsoCode — только персональные поля")
    void shouldNotValidateCountryIsoCode() {
        var request = validRequest();
        request.setCountryIsoCode(null); // не поле персональных данных

        List<ValidationError> errors = validator.validate(request, context);

        // Нет ошибки по countryIsoCode — это не зона ответственности PersonValidator
        assertThat(errors).noneMatch(e -> "countryIsoCode".equals(e.getField()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(TODAY.minusYears(35))  // 1991-03-18
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }
}