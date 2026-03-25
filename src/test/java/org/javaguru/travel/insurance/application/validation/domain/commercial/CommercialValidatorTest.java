package org.javaguru.travel.insurance.application.validation.domain.commercial;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для CommercialValidator.
 *
 * task_136: Проверяем что доменный валидатор корректно изолирует
 * правила валидации коммерческих параметров.
 */
@DisplayName("CommercialValidator")
class CommercialValidatorTest {

    private CommercialValidator validator;
    private ValidationContext context;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new CommercialValidator();
        context = new ValidationContext(TestConstants.TEST_CLOCK);
    }

    // ── currency ──────────────────────────────────────────────────────────────

    @ParameterizedTest
    @ValueSource(strings = {"EUR", "USD", "GBP", "CHF", "JPY"})
    @DisplayName("должен пройти для поддерживаемых валют")
    void shouldPassForSupportedCurrencies(String currency) {
        var request = validRequest();
        request.setCurrency(currency);

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).noneMatch(e -> "currency".equals(e.getField()));
    }

    @Test
    @DisplayName("должен пройти когда currency null (используется EUR по умолчанию)")
    void shouldPassWhenCurrencyNull() {
        var request = validRequest();
        request.setCurrency(null);

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).isEmpty();
    }

    @Test
    @DisplayName("должен вернуть ошибку для неподдерживаемой валюты")
    void shouldFailForUnsupportedCurrency() {
        var request = validRequest();
        request.setCurrency("RUB");

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).anyMatch(e -> "currency".equals(e.getField()));
    }

    @Test
    @DisplayName("CommercialValidator не проверяет персональные данные")
    void shouldNotValidatePersonFields() {
        var request = validRequest();
        request.setPersonFirstName(null);

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).noneMatch(e -> "personFirstName".equals(e.getField()));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TravelCalculatePremiumRequest validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(TODAY.minusYears(35))
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .currency("EUR")
                .build();
    }
}