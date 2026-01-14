package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.BaseTestFixture;
import org.javaguru.travel.insurance.MockSetupHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Финальная версия TravelCalculatePremiumRequestValidatorTest
 * Использует ImprovedMockSetupHelper с lenient моками в @BeforeEach
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TravelCalculatePremiumRequestValidator Tests")
class TravelCalculatePremiumRequestValidatorTest extends BaseTestFixture {

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private MedicalRiskLimitLevelRepository medicalRepository;

    @Mock
    private RiskTypeRepository riskRepository;

    @InjectMocks
    private TravelCalculatePremiumRequestValidator validator;

    private MockSetupHelper mockHelper;

    @BeforeEach
    void setUp() {
        mockHelper = new MockSetupHelper();

        // ✅ Базовая настройка с lenient - не будет ошибки если не все используется
        setupDefaultValidRepositories();
    }

    /**
     * Настройка валидных репозиториев по умолчанию
     * Используем lenient, так как не все тесты используют все моки
     */
    private void setupDefaultValidRepositories() {
        mockHelper.mockCountryLenient(countryRepository, defaultCountry());
        mockHelper.mockMedicalLevelLenient(medicalRepository, defaultMedicalLevel());
        mockHelper.mockRiskTypeLenient(riskRepository, mandatoryRisk());
    }

    // ========================================
    // HAPPY PATH
    // ========================================

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("passes validation when all fields valid")
        void shouldPassValidation() {
            var errors = validator.validate(validRequest());

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts request with optional risks")
        void shouldAcceptRequestWithOptionalRisks() {
            mockHelper.mockRiskTypeLenient(riskRepository,
                    optionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3")));

            var request = requestWithRisks("SPORT_ACTIVITIES");
            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts request with multiple risks")
        void shouldAcceptMultipleRisks() {
            mockHelper.mockRiskTypeLenient(riskRepository,
                    optionalRisk("SPORT_ACTIVITIES", new BigDecimal("0.3")));
            mockHelper.mockRiskTypeLenient(riskRepository,
                    optionalRisk("CHRONIC_DISEASES", new BigDecimal("0.4")));

            var request = requestWithRisks("SPORT_ACTIVITIES", "CHRONIC_DISEASES");
            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // FIELD VALIDATION
    // ========================================

    @Nested
    @DisplayName("Personal Info Validation")
    class PersonalInfoValidation {

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("fails when firstName is null or empty")
        void shouldFailWhenFirstNameInvalid(String firstName) {
            var request = validRequest();
            request.setPersonFirstName(firstName);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "personFirstName".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("fails when lastName is null or empty")
        void shouldFailWhenLastNameInvalid(String lastName) {
            var request = validRequest();
            request.setPersonLastName(lastName);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "personLastName".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @Test
        @DisplayName("fails when birthDate is null")
        void shouldFailWhenBirthDateNull() {
            var request = validRequest();
            request.setPersonBirthDate(null);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "personBirthDate".equals(e.getField()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"John", "Jean-Pierre", "Mary Ann", "Иван", "José", "O'Brien"})
        @DisplayName("accepts various valid first names")
        void shouldAcceptValidFirstNames(String firstName) {
            var request = validRequest();
            request.setPersonFirstName(firstName);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"Smith", "O'Connor", "van der Berg", "Петров", "García"})
        @DisplayName("accepts various valid last names")
        void shouldAcceptValidLastNames(String lastName) {
            var request = validRequest();
            request.setPersonLastName(lastName);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // DATE VALIDATION
    // ========================================

    @Nested
    @DisplayName("Date Validation")
    class DateValidation {

        @Test
        @DisplayName("fails when dateFrom is null")
        void shouldFailWhenDateFromNull() {
            var request = validRequest();
            request.setAgreementDateFrom(null);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "agreementDateFrom".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @Test
        @DisplayName("fails when dateTo is null")
        void shouldFailWhenDateToNull() {
            var request = validRequest();
            request.setAgreementDateTo(null);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "agreementDateTo".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @Test
        @DisplayName("fails when dateTo before dateFrom")
        void shouldFailWhenInvalidDateOrder() {
            var request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2025, 6, 10));
            request.setAgreementDateTo(LocalDate.of(2025, 6, 5));

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> e.getField().equals("agreementDateTo")
                            && e.getMessage().contains("after"));
        }

        @Test
        @DisplayName("accepts when dateTo equals dateFrom")
        void shouldAcceptSameDates() {
            var request = validRequest();
            LocalDate sameDate = LocalDate.of(2025, 6, 1);
            request.setAgreementDateFrom(sameDate);
            request.setAgreementDateTo(sameDate);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts when dateTo after dateFrom")
        void shouldAcceptValidDateOrder() {
            var request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2025, 6, 1));
            request.setAgreementDateTo(LocalDate.of(2025, 6, 15));

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // REPOSITORY VALIDATION
    // ========================================

    @Nested
    @DisplayName("Repository Validation")
    class RepositoryValidation {

        @ParameterizedTest
        @ValueSource(strings = {"XX", "YY", "ZZ", "INVALID"})
        @DisplayName("fails when country not found")
        void shouldFailWhenCountryNotFound(String invalidCode) {
            var request = validRequest();
            request.setCountryIsoCode(invalidCode);

            // ✅ Используем strict мок для конкретного теста
            mockHelper.mockCountryNotFound(countryRepository, invalidCode);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> e.getField().equals("countryIsoCode")
                            && e.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("accepts when country exists and active")
        void shouldAcceptValidCountry() {
            // ✅ Настроен в @BeforeEach с lenient
            var request = validRequest();
            request.setCountryIsoCode("ES");

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "99999", "UNKNOWN"})
        @DisplayName("fails when medical level not found")
        void shouldFailWhenMedicalLevelNotFound(String invalidCode) {
            var request = validRequest();
            request.setMedicalRiskLimitLevel(invalidCode);

            mockHelper.mockMedicalLevelNotFound(medicalRepository, invalidCode);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> e.getField().equals("medicalRiskLimitLevel")
                            && e.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("accepts when medical level exists and active")
        void shouldAcceptValidMedicalLevel() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel("50000");

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("fails when optional risk not found")
        void shouldFailWhenRiskNotFound() {
            var request = requestWithRisks("UNKNOWN_RISK");

            mockHelper.mockRiskNotFound(riskRepository, "UNKNOWN_RISK");

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> e.getField().equals("selectedRisks")
                            && e.getMessage().contains("not found"));
        }

        @Test
        @DisplayName("fails when mandatory risk selected manually")
        void shouldFailWhenMandatoryRiskSelected() {
            var request = requestWithRisks("TRAVEL_MEDICAL");

            // TRAVEL_MEDICAL уже настроен как mandatory в @BeforeEach

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> e.getField().equals("selectedRisks")
                            && e.getMessage().contains("mandatory"));
        }

        @Test
        @DisplayName("accepts empty risk list")
        void shouldAcceptEmptyRiskList() {
            var request = validRequest();
            request.setSelectedRisks(List.of());

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts null risk list")
        void shouldAcceptNullRiskList() {
            var request = validRequest();
            request.setSelectedRisks(null);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // MULTIPLE ERRORS
    // ========================================

    @Nested
    @DisplayName("Multiple Errors Tests")
    class MultipleErrorsTests {

        @Test
        @DisplayName("returns all errors when multiple fields invalid")
        void shouldReturnAllErrors() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personFirstName(null)
                    .personLastName(null)
                    .personBirthDate(null)
                    .agreementDateFrom(null)
                    .agreementDateTo(null)
                    .countryIsoCode(null)
                    .medicalRiskLimitLevel(null)
                    .build();

            var errors = validator.validate(request);

            assertThat(errors).hasSizeGreaterThanOrEqualTo(5);
        }

        @Test
        @DisplayName("returns errors for both names when empty")
        void shouldReturnErrorsForBothNames() {
            var request = validRequest();
            request.setPersonFirstName("");
            request.setPersonLastName("");

            var errors = validator.validate(request);

            assertThat(errors).hasSize(2);
            assertThat(errors)
                    .anyMatch(e -> "personFirstName".equals(e.getField()))
                    .anyMatch(e -> "personLastName".equals(e.getField()));
        }

        @Test
        @DisplayName("returns errors for both dates when null")
        void shouldReturnErrorsForBothDates() {
            var request = validRequest();
            request.setAgreementDateFrom(null);
            request.setAgreementDateTo(null);

            var errors = validator.validate(request);

            assertThat(errors).hasSizeGreaterThanOrEqualTo(2);
            assertThat(errors)
                    .anyMatch(e -> "agreementDateFrom".equals(e.getField()))
                    .anyMatch(e -> "agreementDateTo".equals(e.getField()));
        }

        @Test
        @DisplayName("returns errors for invalid names and dates")
        void shouldReturnErrorsForNamesAndDates() {
            var request = TravelCalculatePremiumRequest.builder()
                    .personFirstName("")
                    .personLastName("")
                    .personBirthDate(LocalDate.of(1990, 1, 1))
                    .agreementDateFrom(null)
                    .agreementDateTo(null)
                    .countryIsoCode("ES")
                    .medicalRiskLimitLevel("50000")
                    .build();

            var errors = validator.validate(request);

            assertThat(errors).hasSize(4); // firstName, lastName, dateFrom, dateTo
        }
    }

    // ========================================
    // EDGE CASES
    // ========================================

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCasesTests {

        @Test
        @DisplayName("accepts request with whitespace-padded values")
        void shouldTrimWhitespace() {
            var request = validRequest();
            request.setPersonFirstName("  John  ");
            request.setPersonLastName("  Doe  ");

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("fails when firstName is only whitespace")
        void shouldFailWhenFirstNameOnlyWhitespace() {
            var request = validRequest();
            request.setPersonFirstName("   ");

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "personFirstName".equals(e.getField()));
        }

        @Test
        @DisplayName("accepts very long names")
        void shouldAcceptVeryLongNames() {
            var request = validRequest();
            request.setPersonFirstName("A".repeat(100));
            request.setPersonLastName("B".repeat(100));

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts single character names")
        void shouldAcceptSingleCharacterNames() {
            var request = validRequest();
            request.setPersonFirstName("A");
            request.setPersonLastName("B");

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts dates spanning leap year")
        void shouldAcceptLeapYearDates() {
            var request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2024, 2, 28));
            request.setAgreementDateTo(LocalDate.of(2024, 3, 1));

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("accepts dates crossing year boundary")
        void shouldAcceptYearBoundaryCrossing() {
            var request = validRequest();
            request.setAgreementDateFrom(LocalDate.of(2024, 12, 25));
            request.setAgreementDateTo(LocalDate.of(2025, 1, 5));

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // COUNTRY ISO CODE VALIDATION
    // ========================================

    @Nested
    @DisplayName("Country ISO Code Validation")
    class CountryIsoCodeValidation {

        @Test
        @DisplayName("fails when country code is null")
        void shouldFailWhenCountryCodeNull() {
            var request = validRequest();
            request.setCountryIsoCode(null);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "countryIsoCode".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @Test
        @DisplayName("fails when country code is empty")
        void shouldFailWhenCountryCodeEmpty() {
            var request = validRequest();
            request.setCountryIsoCode("");

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "countryIsoCode".equals(e.getField()));
        }

        @Test
        @DisplayName("accepts country code with whitespace that gets trimmed")
        void shouldAcceptCountryCodeWithWhitespace() {
            var request = validRequest();
            request.setCountryIsoCode("  ES  ");

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"ES", "DE", "FR", "IT", "US", "TH"})
        @DisplayName("accepts various valid country codes")
        void shouldAcceptValidCountryCodes(String countryCode) {
            // Настраиваем разные страны
            mockHelper.mockCountryLenient(countryRepository,
                    createCountryEntity(countryCode, "Country", new BigDecimal("1.0")));

            var request = validRequest();
            request.setCountryIsoCode(countryCode);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }

    // ========================================
    // MEDICAL RISK LEVEL VALIDATION
    // ========================================

    @Nested
    @DisplayName("Medical Risk Level Validation")
    class MedicalRiskLevelValidation {

        @Test
        @DisplayName("fails when medical level is null")
        void shouldFailWhenMedicalLevelNull() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel(null);

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField())
                            && e.getMessage().contains("Must not be empty"));
        }

        @Test
        @DisplayName("fails when medical level is empty")
        void shouldFailWhenMedicalLevelEmpty() {
            var request = validRequest();
            request.setMedicalRiskLimitLevel("");

            var errors = validator.validate(request);

            assertThat(errors)
                    .isNotEmpty()
                    .anyMatch(e -> "medicalRiskLimitLevel".equals(e.getField()));
        }

        @ParameterizedTest
        @ValueSource(strings = {"5000", "10000", "20000", "50000", "100000"})
        @DisplayName("accepts various valid medical levels")
        void shouldAcceptValidMedicalLevels(String level) {
            mockHelper.mockMedicalLevelLenient(medicalRepository,
                    createMedicalLevel(level, new BigDecimal("2.00"), new BigDecimal(level)));

            var request = validRequest();
            request.setMedicalRiskLimitLevel(level);

            var errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }
    }
}