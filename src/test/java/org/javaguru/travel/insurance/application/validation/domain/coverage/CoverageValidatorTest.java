package org.javaguru.travel.insurance.application.validation.domain.coverage;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
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
 * Тесты для CoverageValidator.
 *
 * task_136: Доменный валидатор уровня медицинского покрытия.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CoverageValidator — task_136")
class CoverageValidatorTest {

    @Mock
    private ReferenceDataPort referenceDataPort;

    private CoverageValidator validator;
    private ValidationContext context;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new CoverageValidator(referenceDataPort);
        context = new ValidationContext(TestConstants.TEST_CLOCK);
    }

    // ── medicalRiskLimitLevel обязательность ──────────────────────────────────

    @Nested
    @DisplayName("medicalRiskLimitLevel — обязательность")
    class MedicalRiskLimitLevelRequired {

        @Test
        @DisplayName("должен вернуть ошибку когда medicalRiskLimitLevel null и режим MEDICAL_LEVEL")
        void shouldFailWhenLevelNullInMedicalLevelMode() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel(null);
            request.setUseCountryDefaultPremium(false);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField()));
        }

        @Test
        @DisplayName("должен вернуть ошибку когда medicalRiskLimitLevel пустой")
        void shouldFailWhenLevelBlankInMedicalLevelMode() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel("   ");
            request.setUseCountryDefaultPremium(false);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField()));
        }

        @Test
        @DisplayName("должен пройти когда medicalRiskLimitLevel null в режиме COUNTRY_DEFAULT")
        void shouldPassWhenLevelNullInCountryDefaultMode() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel(null);
            request.setUseCountryDefaultPremium(true);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).noneMatch(e -> "medicalRiskLimitLevel".equals(e.getField()));
        }

        @Test
        @DisplayName("должен пройти когда useCountryDefaultPremium=true — уровень не проверяется")
        void shouldSkipValidationInCountryDefaultMode() {
            var request = validRequest();
            request.setUseCountryDefaultPremium(true);
            request.setMedicalRiskLimitLevel(null);

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isEmpty();
        }
    }

    // ── существование уровня в справочнике ───────────────────────────────────

    @Nested
    @DisplayName("medicalRiskLimitLevel — существование в справочнике")
    class MedicalRiskLimitLevelExistence {

        @Test
        @DisplayName("должен пройти когда уровень найден в справочнике")
        void shouldPassWhenLevelExistsInReference() {
            stubLevelFound();
            var request = validRequest();

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("должен вернуть ошибку когда уровень не найден в справочнике")
        void shouldFailWhenLevelNotFoundInReference() {
            when(referenceDataPort.findMedicalLevel(any(), any()))
                    .thenReturn(Optional.empty());
            var request = validRequest();

            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField()));
        }
    }

    // ── изоляция — не проверяет чужие поля ───────────────────────────────────

    @Test
    @DisplayName("CoverageValidator не проверяет personFirstName")
    void shouldNotValidatePersonFields() {
        stubLevelFound();
        var request = validRequest();
        request.setPersonFirstName(null);

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).noneMatch(e -> "personFirstName".equals(e.getField()));
    }

    @Test
    @DisplayName("CoverageValidator не проверяет countryIsoCode")
    void shouldNotValidateCountryField() {
        stubLevelFound();
        var request = validRequest();
        request.setCountryIsoCode(null);

        List<ValidationError> errors = validator.validate(request, context);

        assertThat(errors).noneMatch(e -> "countryIsoCode".equals(e.getField()));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void stubLevelFound() {
        when(referenceDataPort.findMedicalLevel(eq("50000"), any()))
                .thenReturn(Optional.of(mock(MedicalRiskLimitLevel.class)));
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
                .useCountryDefaultPremium(false)
                .build();
    }
}