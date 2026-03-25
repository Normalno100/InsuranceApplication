package org.javaguru.travel.insurance.application.validation.domain.trip;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.domain.model.entity.Country;
import org.javaguru.travel.insurance.domain.model.valueobject.CountryCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Тесты для TripValidator.
 *
 * task_136: Проверяем что доменный валидатор корректно изолирует
 * правила валидации параметров поездки.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TripValidator")
class TripValidatorTest {

    @Mock
    private ReferenceDataPort referenceDataPort;

    private TripValidator validator;
    private ValidationContext context;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new TripValidator(referenceDataPort);
        context = new ValidationContext(TestConstants.TEST_CLOCK);
    }

    // ── agreementDateFrom ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("agreementDateFrom validation")
    class AgreementDateFromTests {

        @Test
        @DisplayName("должен вернуть ошибку когда agreementDateFrom null")
        void shouldFailWhenDateFromNull() {
            var request = validRequest();
            request.setAgreementDateFrom(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "agreementDateFrom".equals(e.getField()));
        }
    }

    // ── agreementDateTo ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("agreementDateTo validation")
    class AgreementDateToTests {

        @Test
        @DisplayName("должен вернуть ошибку когда agreementDateTo null")
        void shouldFailWhenDateToNull() {
            var request = validRequest();
            request.setAgreementDateTo(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "agreementDateTo".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда dateTo раньше dateFrom")
        void shouldFailWhenDateToBeforeDateFrom() {
            var request = validRequest();
            request.setAgreementDateFrom(DATE_FROM.plusDays(10));
            request.setAgreementDateTo(DATE_FROM); // dateTo < dateFrom

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "agreementDateTo".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда поездка более 365 дней")
        void shouldFailWhenTripTooLong() {
            var request = validRequest();
            request.setAgreementDateTo(DATE_FROM.plusDays(366));

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "agreementDateTo".equals(e.getField()));
        }
    }

    // ── countryIsoCode ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("countryIsoCode validation")
    class CountryIsoCodeTests {

        @Test
        @DisplayName("должен вернуть ошибку когда countryIsoCode null")
        void shouldFailWhenCountryNull() {
            var request = validRequest();
            request.setCountryIsoCode(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "countryIsoCode".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда countryIsoCode неверный формат (1 буква)")
        void shouldFailWhenCountryWrongFormat() {
            var request = validRequest();
            request.setCountryIsoCode("E");

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "countryIsoCode".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда страна не найдена в справочнике")
        void shouldFailWhenCountryNotFound() {
            when(referenceDataPort.findCountry(any(CountryCode.class), any()))
                    .thenReturn(Optional.empty());

            var request = validRequest();

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "countryIsoCode".equals(e.getField()));
        }
    }

    // ── Valid case ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("должен пройти валидацию для корректных параметров поездки")
    void shouldPassForValidTripData() {
        stubCountryFound();
        var request = validRequest();

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("TripValidator не проверяет personFirstName — только параметры поездки")
    void shouldNotValidatePersonFields() {
        stubCountryFound();
        var request = validRequest();
        request.setPersonFirstName(null); // не зона ответственности TripValidator

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).noneMatch(e -> "personFirstName".equals(e.getField()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void stubCountryFound() {
        Country country = mock(Country.class);
        when(referenceDataPort.findCountry(any(CountryCode.class), any()))
                .thenReturn(Optional.of(country));
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