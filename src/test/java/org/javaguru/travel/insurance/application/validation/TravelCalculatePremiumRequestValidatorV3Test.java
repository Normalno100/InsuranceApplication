package org.javaguru.travel.insurance.application.validation;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.v3.InsuredPerson;
import org.javaguru.travel.insurance.application.dto.v3.TravelCalculatePremiumRequestV3;
import org.javaguru.travel.insurance.application.validation.domain.commercial.CommercialValidator;
import org.javaguru.travel.insurance.application.validation.domain.coverage.CoverageValidator;
import org.javaguru.travel.insurance.application.validation.domain.person.PersonValidator;
import org.javaguru.travel.insurance.application.validation.domain.risks.SelectedRisksValidator;
import org.javaguru.travel.insurance.application.validation.domain.trip.TripValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Тесты для TravelCalculatePremiumRequestValidatorV3.
 *
 * task_135: Валидатор для нового V3 формата запроса.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumRequestValidatorV3 — task_135")
class TravelCalculatePremiumRequestValidatorV3Test {

    @Mock private PersonValidator personValidator;
    @Mock private TripValidator tripValidator;
    @Mock private CoverageValidator coverageValidator;
    @Mock private SelectedRisksValidator selectedRisksValidator;
    @Mock private CommercialValidator commercialValidator;

    private TravelCalculatePremiumRequestValidatorV3 validator;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidatorV3(
                personValidator, tripValidator, coverageValidator,
                selectedRisksValidator, commercialValidator);
    }

    // ── Проверка списка персон ────────────────────────────────────────────────

    @Nested
    @DisplayName("Валидация списка persons")
    class PersonsListValidation {

        @Test
        @DisplayName("должен вернуть CRITICAL ошибку когда persons null")
        void shouldReturnCriticalErrorWhenPersonsNull() {
            TravelCalculatePremiumRequestV3 request = buildRequest(null);

            List<ValidationError> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("persons");
            assertThat(errors.get(0).getSeverity()).isEqualTo(ValidationError.Severity.CRITICAL);
            verifyNoInteractions(personValidator, tripValidator, coverageValidator,
                    selectedRisksValidator, commercialValidator);
        }

        @Test
        @DisplayName("должен вернуть CRITICAL ошибку когда persons пустой")
        void shouldReturnCriticalErrorWhenPersonsEmpty() {
            TravelCalculatePremiumRequestV3 request = buildRequest(List.of());

            List<ValidationError> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0).getField()).isEqualTo("persons");
            assertThat(errors.get(0).getSeverity()).isEqualTo(ValidationError.Severity.CRITICAL);
        }
    }

    // ── Адресация ошибок персоны с индексом ──────────────────────────────────

    @Nested
    @DisplayName("Адресация ошибок с индексом персоны")
    class PersonIndexedErrors {

        @Test
        @DisplayName("должен добавлять префикс persons[0]. к ошибкам первой персоны")
        void shouldPrefixFirstPersonErrors() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(
                            ValidationError.error("personFirstName", "Must not be empty!")
                    ));
            allOtherValidatorsReturnNoErrors();

            List<ValidationError> errors = validator.validate(buildRequest(List.of(person())));

            assertThat(errors).anyMatch(e ->
                    "persons[0].personFirstName".equals(e.getField()));
        }

        @Test
        @DisplayName("должен использовать правильный индекс для второй персоны")
        void shouldUseCorrectIndexForSecondPerson() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of())  // первая персона OK
                    .thenReturn(List.of(
                            ValidationError.error("personBirthDate", "Must be in the past!")
                    ));
            allOtherValidatorsReturnNoErrors();

            TravelCalculatePremiumRequestV3 request = buildRequest(List.of(person(), person()));
            List<ValidationError> errors = validator.validate(request);

            assertThat(errors).anyMatch(e ->
                    "persons[1].personBirthDate".equals(e.getField()));
            assertThat(errors).noneMatch(e ->
                    "persons[0].personBirthDate".equals(e.getField()));
        }

        @Test
        @DisplayName("должен префиксировать ошибки для всех персон в списке")
        void shouldPrefixErrorsForAllPersons() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.error("personLastName", "empty")));
            allOtherValidatorsReturnNoErrors();

            TravelCalculatePremiumRequestV3 request = buildRequest(
                    List.of(person(), person(), person()));
            List<ValidationError> errors = validator.validate(request);

            assertThat(errors.stream().map(ValidationError::getField).toList())
                    .containsExactlyInAnyOrder(
                            "persons[0].personLastName",
                            "persons[1].personLastName",
                            "persons[2].personLastName"
                    );
        }
    }

    // ── Делегирование общих валидаторов ───────────────────────────────────────

    @Nested
    @DisplayName("Делегирование общих параметров поездки")
    class TripParametersDelegation {

        @Test
        @DisplayName("должен вызвать TripValidator для общих параметров")
        void shouldCallTripValidator() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person())));
            verify(tripValidator, times(1)).validate(any(), any());
        }

        @Test
        @DisplayName("должен вызвать CoverageValidator")
        void shouldCallCoverageValidator() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person())));
            verify(coverageValidator, times(1)).validate(any(), any());
        }

        @Test
        @DisplayName("должен вызвать SelectedRisksValidator")
        void shouldCallSelectedRisksValidator() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person())));
            verify(selectedRisksValidator, times(1)).validate(any(), any());
        }

        @Test
        @DisplayName("должен вызвать CommercialValidator")
        void shouldCallCommercialValidator() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person())));
            verify(commercialValidator, times(1)).validate(any(), any());
        }

        @Test
        @DisplayName("PersonValidator должен вызываться N раз для N персон")
        void shouldCallPersonValidatorNTimesForNPersons() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person(), person(), person())));
            verify(personValidator, times(3)).validate(any(), any());
        }

        @Test
        @DisplayName("TripValidator должен вызываться 1 раз независимо от числа персон")
        void shouldCallTripValidatorOnce() {
            allValidatorsReturnNoErrors();
            validator.validate(buildRequest(List.of(person(), person(), person())));
            verify(tripValidator, times(1)).validate(any(), any());
        }
    }

    // ── Успешная валидация ────────────────────────────────────────────────────

    @Test
    @DisplayName("должен вернуть пустой список при успешной валидации")
    void shouldReturnEmptyListOnSuccess() {
        allValidatorsReturnNoErrors();
        List<ValidationError> errors = validator.validate(buildRequest(List.of(person())));
        assertThat(errors).isEmpty();
    }

    // ── Стоп при критичной ошибке персоны ────────────────────────────────────

    @Test
    @DisplayName("должен остановить проверку trip/coverage если PersonValidator вернул CRITICAL")
    void shouldStopWhenPersonValidatorReturnsCritical() {
        when(personValidator.validate(any(), any()))
                .thenReturn(List.of(ValidationError.critical("personFirstName", "null!")));

        validator.validate(buildRequest(List.of(person())));

        verify(tripValidator, never()).validate(any(), any());
        verify(coverageValidator, never()).validate(any(), any());
    }

    // ── Сборка ошибок из разных доменов ──────────────────────────────────────

    @Test
    @DisplayName("должен собирать ошибки от персон и общих параметров")
    void shouldCollectErrorsFromPersonsAndTrip() {
        when(personValidator.validate(any(), any()))
                .thenReturn(List.of(ValidationError.error("personFirstName", "empty")));
        when(tripValidator.validate(any(), any()))
                .thenReturn(List.of(ValidationError.error("countryIsoCode", "unknown")));
        when(coverageValidator.validate(any(), any())).thenReturn(List.of());
        when(selectedRisksValidator.validate(any(), any())).thenReturn(List.of());
        when(commercialValidator.validate(any(), any())).thenReturn(List.of());

        List<ValidationError> errors = validator.validate(buildRequest(List.of(person())));

        assertThat(errors).hasSize(2);
        assertThat(errors).anyMatch(e -> e.getField().startsWith("persons["));
        assertThat(errors).anyMatch(e -> "countryIsoCode".equals(e.getField()));
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private void allValidatorsReturnNoErrors() {
        when(personValidator.validate(any(), any())).thenReturn(List.of());
        allOtherValidatorsReturnNoErrors();
    }

    private void allOtherValidatorsReturnNoErrors() {
        when(tripValidator.validate(any(), any())).thenReturn(List.of());
        when(coverageValidator.validate(any(), any())).thenReturn(List.of());
        when(selectedRisksValidator.validate(any(), any())).thenReturn(List.of());
        when(commercialValidator.validate(any(), any())).thenReturn(List.of());
    }

    private InsuredPerson person() {
        return InsuredPerson.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(TODAY.minusYears(35))
                .build();
    }

    private TravelCalculatePremiumRequestV3 buildRequest(List<InsuredPerson> persons) {
        return TravelCalculatePremiumRequestV3.builder()
                .persons(persons)
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .currency("EUR")
                .build();
    }
}