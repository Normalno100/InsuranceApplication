package org.javaguru.travel.insurance.application.validation;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
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
 * Тесты для TravelCalculatePremiumRequestValidator как оркестратора.
 *
 * task_136: Валидатор рефакторен — стал оркестратором доменных валидаторов.
 * Проверяем правильность делегирования и поведение при критичных ошибках.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumRequestValidator — task_136 orchestrator")
class TravelCalculatePremiumRequestValidatorOrchestratorTest {

    @Mock private PersonValidator personValidator;
    @Mock private TripValidator tripValidator;
    @Mock private CoverageValidator coverageValidator;
    @Mock private SelectedRisksValidator selectedRisksValidator;
    @Mock private CommercialValidator commercialValidator;

    private TravelCalculatePremiumRequestValidator validator;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidator(
                personValidator, tripValidator, coverageValidator,
                selectedRisksValidator, commercialValidator);
    }

    // ── Порядок вызовов ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("Порядок вызовов доменных валидаторов")
    class OrderOfExecution {

        @Test
        @DisplayName("должен вызвать PersonValidator первым")
        void shouldCallPersonValidatorFirst() {
            allValidatorsReturnNoErrors();
            var request = validRequest();

            validator.validate(request);

            // Порядок: Person → Trip → Coverage → Risks → Commercial
            var inOrder = inOrder(personValidator, tripValidator, coverageValidator,
                    selectedRisksValidator, commercialValidator);
            inOrder.verify(personValidator).validate(any(), any());
            inOrder.verify(tripValidator).validate(any(), any());
            inOrder.verify(coverageValidator).validate(any(), any());
            inOrder.verify(selectedRisksValidator).validate(any(), any());
            inOrder.verify(commercialValidator).validate(any(), any());
        }

        @Test
        @DisplayName("должен вызвать все 5 валидаторов при успешной валидации")
        void shouldCallAllFiveValidators() {
            allValidatorsReturnNoErrors();

            validator.validate(validRequest());

            verify(personValidator, times(1)).validate(any(), any());
            verify(tripValidator, times(1)).validate(any(), any());
            verify(coverageValidator, times(1)).validate(any(), any());
            verify(selectedRisksValidator, times(1)).validate(any(), any());
            verify(commercialValidator, times(1)).validate(any(), any());
        }
    }

    // ── Стоп при критичных ошибках ────────────────────────────────────────────

    @Nested
    @DisplayName("Стоп при критичных ошибках")
    class StopOnCriticalError {

        @Test
        @DisplayName("должен остановить оркестрацию если PersonValidator вернул CRITICAL")
        void shouldStopAfterPersonValidatorCriticalError() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.critical("personFirstName", "null!")));

            validator.validate(validRequest());

            verify(personValidator, times(1)).validate(any(), any());
            verify(tripValidator, never()).validate(any(), any());
            verify(coverageValidator, never()).validate(any(), any());
            verify(selectedRisksValidator, never()).validate(any(), any());
            verify(commercialValidator, never()).validate(any(), any());
        }

        @Test
        @DisplayName("должен остановить оркестрацию если TripValidator вернул CRITICAL")
        void shouldStopAfterTripValidatorCriticalError() {
            when(personValidator.validate(any(), any())).thenReturn(List.of());
            when(tripValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.critical("agreementDateFrom", "null!")));

            validator.validate(validRequest());

            verify(coverageValidator, never()).validate(any(), any());
            verify(selectedRisksValidator, never()).validate(any(), any());
            verify(commercialValidator, never()).validate(any(), any());
        }

        @Test
        @DisplayName("должен продолжать если ошибки не критичные (WARNING/ERROR)")
        void shouldContinueForNonCriticalErrors() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.error("personFirstName", "too long")));
            when(tripValidator.validate(any(), any())).thenReturn(List.of());
            when(coverageValidator.validate(any(), any())).thenReturn(List.of());
            when(selectedRisksValidator.validate(any(), any())).thenReturn(List.of());
            when(commercialValidator.validate(any(), any())).thenReturn(List.of());

            validator.validate(validRequest());

            // Все валидаторы должны быть вызваны
            verify(tripValidator, times(1)).validate(any(), any());
            verify(coverageValidator, times(1)).validate(any(), any());
            verify(selectedRisksValidator, times(1)).validate(any(), any());
            verify(commercialValidator, times(1)).validate(any(), any());
        }
    }

    // ── Сборка ошибок ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Сборка ошибок из всех валидаторов")
    class ErrorCollection {

        @Test
        @DisplayName("должен вернуть пустой список если ошибок нет")
        void shouldReturnEmptyListWhenNoErrors() {
            allValidatorsReturnNoErrors();

            List<ValidationError> errors = validator.validate(validRequest());

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("должен собирать ошибки от нескольких валидаторов")
        void shouldCollectErrorsFromMultipleValidators() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.error("personFirstName", "empty")));
            when(tripValidator.validate(any(), any()))
                    .thenReturn(List.of(ValidationError.error("countryIsoCode", "unknown")));
            when(coverageValidator.validate(any(), any())).thenReturn(List.of());
            when(selectedRisksValidator.validate(any(), any())).thenReturn(List.of());
            when(commercialValidator.validate(any(), any())).thenReturn(List.of());

            List<ValidationError> errors = validator.validate(validRequest());

            assertThat(errors).hasSize(2);
            assertThat(errors).anyMatch(e -> "personFirstName".equals(e.getField()));
            assertThat(errors).anyMatch(e -> "countryIsoCode".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть только ошибки до критичной (включительно)")
        void shouldReturnOnlyErrorsUpToCritical() {
            when(personValidator.validate(any(), any()))
                    .thenReturn(List.of(
                            ValidationError.error("personLastName", "empty"),
                            ValidationError.critical("personFirstName", "null!")
                    ));

            List<ValidationError> errors = validator.validate(validRequest());

            // Только ошибки от PersonValidator — остальные валидаторы не вызваны
            assertThat(errors).hasSize(2);
            assertThat(errors).anyMatch(e -> "personFirstName".equals(e.getField()));
            assertThat(errors).anyMatch(e -> "personLastName".equals(e.getField()));
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void allValidatorsReturnNoErrors() {
        when(personValidator.validate(any(), any())).thenReturn(List.of());
        when(tripValidator.validate(any(), any())).thenReturn(List.of());
        when(coverageValidator.validate(any(), any())).thenReturn(List.of());
        when(selectedRisksValidator.validate(any(), any())).thenReturn(List.of());
        when(commercialValidator.validate(any(), any())).thenReturn(List.of());
    }

    private TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(TODAY.minusYears(35))
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();
    }
}