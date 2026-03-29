package org.javaguru.travel.insurance.application.dto.v3;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты для V3 DTO-классов.
 *
 * task_137: Проверяем корректность создания DTO, дефолтных значений,
 * builder-паттернов и вспомогательных методов.
 */
@DisplayName("V3 DTOs — task_137")
class V3DtoTest {

    // ── InsuredPerson ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("InsuredPerson")
    class InsuredPersonTests {

        @Test
        @DisplayName("должен создаваться через builder со всеми полями")
        void shouldCreateViaBuilder() {
            InsuredPerson person = InsuredPerson.builder()
                    .personFirstName("Ivan")
                    .personLastName("Petrov")
                    .personBirthDate(LocalDate.of(1990, 5, 15))
                    .applyAgeCoefficient(true)
                    .build();

            assertThat(person.getPersonFirstName()).isEqualTo("Ivan");
            assertThat(person.getPersonLastName()).isEqualTo("Petrov");
            assertThat(person.getPersonBirthDate()).isEqualTo(LocalDate.of(1990, 5, 15));
            assertThat(person.getApplyAgeCoefficient()).isTrue();
        }

        @Test
        @DisplayName("должен создаваться через no-args конструктор")
        void shouldCreateViaNoArgsConstructor() {
            InsuredPerson person = new InsuredPerson();
            person.setPersonFirstName("Anna");
            person.setPersonLastName("Ivanova");
            person.setPersonBirthDate(LocalDate.of(1995, 3, 20));

            assertThat(person.getPersonFirstName()).isEqualTo("Anna");
            assertThat(person.getPersonLastName()).isEqualTo("Ivanova");
            assertThat(person.getPersonBirthDate()).isEqualTo(LocalDate.of(1995, 3, 20));
        }

        @Test
        @DisplayName("applyAgeCoefficient по умолчанию null (наследует глобальную настройку)")
        void shouldHaveNullApplyAgeCoefficientByDefault() {
            InsuredPerson person = InsuredPerson.builder()
                    .personFirstName("Ivan")
                    .personLastName("Petrov")
                    .personBirthDate(LocalDate.of(1990, 1, 1))
                    .build();

            assertThat(person.getApplyAgeCoefficient()).isNull();
        }
    }

    // ── PersonPremium ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PersonPremium")
    class PersonPremiumTests {

        @Test
        @DisplayName("должен создаваться через builder со всеми полями")
        void shouldCreateViaBuilder() {
            PersonPremium personPremium = PersonPremium.builder()
                    .firstName("Ivan")
                    .lastName("Petrov")
                    .age(35)
                    .ageGroup("Adults")
                    .premium(new BigDecimal("69.30"))
                    .ageCoefficient(new BigDecimal("1.10"))
                    .build();

            assertThat(personPremium.getFirstName()).isEqualTo("Ivan");
            assertThat(personPremium.getLastName()).isEqualTo("Petrov");
            assertThat(personPremium.getAge()).isEqualTo(35);
            assertThat(personPremium.getAgeGroup()).isEqualTo("Adults");
            assertThat(personPremium.getPremium()).isEqualByComparingTo("69.30");
            assertThat(personPremium.getAgeCoefficient()).isEqualByComparingTo("1.10");
        }

        @Test
        @DisplayName("должен создаваться через no-args конструктор")
        void shouldCreateViaNoArgsConstructor() {
            PersonPremium personPremium = new PersonPremium();
            personPremium.setFirstName("Elena");
            personPremium.setPremium(new BigDecimal("45.00"));

            assertThat(personPremium.getFirstName()).isEqualTo("Elena");
            assertThat(personPremium.getPremium()).isEqualByComparingTo("45.00");
        }
    }

    // ── PricingSummaryV3 ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("PricingSummaryV3")
    class PricingSummaryV3Tests {

        @Test
        @DisplayName("должен создаваться с полем totalPersonsPremium")
        void shouldCreateWithTotalPersonsPremium() {
            PricingSummaryV3 pricing = PricingSummaryV3.builder()
                    .totalPremium(new BigDecimal("156.24"))
                    .totalPersonsPremium(new BigDecimal("195.30"))
                    .baseAmount(new BigDecimal("195.30"))
                    .totalDiscount(new BigDecimal("39.06"))
                    .currency("EUR")
                    .includedRisks(List.of("SPORT_ACTIVITIES"))
                    .build();

            assertThat(pricing.getTotalPremium()).isEqualByComparingTo("156.24");
            assertThat(pricing.getTotalPersonsPremium()).isEqualByComparingTo("195.30");
            assertThat(pricing.getBaseAmount()).isEqualByComparingTo("195.30");
            assertThat(pricing.getTotalDiscount()).isEqualByComparingTo("39.06");
            assertThat(pricing.getCurrency()).isEqualTo("EUR");
            assertThat(pricing.getIncludedRisks()).containsExactly("SPORT_ACTIVITIES");
        }

        @Test
        @DisplayName("includedRisks должен иметь пустой список по умолчанию")
        void shouldHaveEmptyIncludedRisksByDefault() {
            PricingSummaryV3 pricing = PricingSummaryV3.builder()
                    .totalPremium(new BigDecimal("100.00"))
                    .build();

            assertThat(pricing.getIncludedRisks()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("totalPersonsPremium отличается от totalPremium на сумму скидок")
        void totalPersonsPremiumShouldDifferFromTotalByDiscount() {
            BigDecimal totalPersons = new BigDecimal("200.00");
            BigDecimal discount = new BigDecimal("40.00");
            BigDecimal total = totalPersons.subtract(discount);

            PricingSummaryV3 pricing = PricingSummaryV3.builder()
                    .totalPersonsPremium(totalPersons)
                    .totalDiscount(discount)
                    .totalPremium(total)
                    .build();

            BigDecimal calculated = pricing.getTotalPersonsPremium()
                    .subtract(pricing.getTotalDiscount());
            assertThat(calculated).isEqualByComparingTo(pricing.getTotalPremium());
        }
    }

    // ── TravelCalculatePremiumRequestV3 ──────────────────────────────────────

    @Nested
    @DisplayName("TravelCalculatePremiumRequestV3")
    class TravelCalculatePremiumRequestV3Tests {

        @Test
        @DisplayName("должен создаваться через builder со списком персон")
        void shouldCreateWithPersonsList() {
            InsuredPerson person1 = InsuredPerson.builder()
                    .personFirstName("Ivan").personLastName("Petrov")
                    .personBirthDate(LocalDate.of(1990, 1, 1)).build();
            InsuredPerson person2 = InsuredPerson.builder()
                    .personFirstName("Anna").personLastName("Petrova")
                    .personBirthDate(LocalDate.of(1993, 5, 15)).build();

            TravelCalculatePremiumRequestV3 request = TravelCalculatePremiumRequestV3.builder()
                    .persons(List.of(person1, person2))
                    .agreementDateFrom(LocalDate.of(2025, 6, 1))
                    .agreementDateTo(LocalDate.of(2025, 6, 15))
                    .countryIsoCode("ES")
                    .medicalRiskLimitLevel("50000")
                    .currency("EUR")
                    .build();

            assertThat(request.getPersons()).hasSize(2);
            assertThat(request.getPersons().get(0).getPersonFirstName()).isEqualTo("Ivan");
            assertThat(request.getPersons().get(1).getPersonFirstName()).isEqualTo("Anna");
            assertThat(request.getCountryIsoCode()).isEqualTo("ES");
            assertThat(request.getMedicalRiskLimitLevel()).isEqualTo("50000");
        }

        @Test
        @DisplayName("должен поддерживать все поля общих параметров поездки")
        void shouldSupportAllTripFields() {
            TravelCalculatePremiumRequestV3 request = TravelCalculatePremiumRequestV3.builder()
                    .persons(List.of())
                    .agreementDateFrom(LocalDate.of(2025, 7, 1))
                    .agreementDateTo(LocalDate.of(2025, 7, 14))
                    .countryIsoCode("DE")
                    .medicalRiskLimitLevel("100000")
                    .useCountryDefaultPremium(false)
                    .selectedRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"))
                    .currency("EUR")
                    .promoCode("SUMMER2025")
                    .personsCount(3)
                    .isCorporate(false)
                    .build();

            assertThat(request.getCountryIsoCode()).isEqualTo("DE");
            assertThat(request.getMedicalRiskLimitLevel()).isEqualTo("100000");
            assertThat(request.getUseCountryDefaultPremium()).isFalse();
            assertThat(request.getSelectedRisks()).containsExactly("SPORT_ACTIVITIES", "LUGGAGE_LOSS");
            assertThat(request.getCurrency()).isEqualTo("EUR");
            assertThat(request.getPromoCode()).isEqualTo("SUMMER2025");
            assertThat(request.getPersonsCount()).isEqualTo(3);
            assertThat(request.getIsCorporate()).isFalse();
        }

        @Test
        @DisplayName("useCountryDefaultPremium null по умолчанию (→ MEDICAL_LEVEL)")
        void shouldHaveNullUseCountryDefaultPremiumByDefault() {
            TravelCalculatePremiumRequestV3 request = TravelCalculatePremiumRequestV3.builder()
                    .persons(List.of())
                    .build();

            assertThat(request.getUseCountryDefaultPremium()).isNull();
        }
    }

    // ── TravelCalculatePremiumResponseV3 ─────────────────────────────────────

    @Nested
    @DisplayName("TravelCalculatePremiumResponseV3")
    class TravelCalculatePremiumResponseV3Tests {

        @Test
        @DisplayName("apiVersion должна быть '3.0' по умолчанию")
        void shouldHaveDefaultApiVersion() {
            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                    .success(true)
                    .build();

            assertThat(response.getApiVersion()).isEqualTo("3.0");
        }

        @Test
        @DisplayName("requestId и timestamp должны генерироваться автоматически")
        void shouldAutoGenerateRequestIdAndTimestamp() {
            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                    .build();

            assertThat(response.getRequestId()).isNotNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("errors по умолчанию пустой список")
        void shouldHaveEmptyErrorsByDefault() {
            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                    .build();

            assertThat(response.getErrors()).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("isSuccessful() → true только для SUCCESS + success=true")
        void shouldReturnTrueForSuccessfulResponse() {
            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                    .success(true)
                    .build();

            assertThat(response.isSuccessful()).isTrue();
        }

        @Test
        @DisplayName("isSuccessful() → false для VALIDATION_ERROR")
        void shouldReturnFalseForValidationError() {
            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                    .success(false)
                    .build();

            assertThat(response.isSuccessful()).isFalse();
        }

        @Test
        @DisplayName("hasErrors() → true когда список ошибок не пустой")
        void shouldReturnTrueWhenHasErrors() {
            var error = TravelCalculatePremiumResponseV3.ValidationError.builder()
                    .field("persons[0].personBirthDate")
                    .message("Must not be empty!")
                    .build();

            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR)
                    .errors(List.of(error))
                    .build();

            assertThat(response.hasErrors()).isTrue();
        }

        @Test
        @DisplayName("personPremiums содержит индивидуальные премии по персонам")
        void shouldContainPersonPremiums() {
            List<PersonPremium> premiums = List.of(
                    PersonPremium.builder()
                            .firstName("Ivan").lastName("Petrov")
                            .age(35).ageGroup("Adults")
                            .premium(new BigDecimal("69.30"))
                            .ageCoefficient(new BigDecimal("1.10"))
                            .build(),
                    PersonPremium.builder()
                            .firstName("Anna").lastName("Petrova")
                            .age(30).ageGroup("Young adults")
                            .premium(new BigDecimal("63.00"))
                            .ageCoefficient(new BigDecimal("1.00"))
                            .build()
            );

            TravelCalculatePremiumResponseV3 response = TravelCalculatePremiumResponseV3.builder()
                    .status(TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS)
                    .success(true)
                    .personPremiums(premiums)
                    .build();

            assertThat(response.getPersonPremiums()).hasSize(2);
            assertThat(response.getPersonPremiums().get(0).getFirstName()).isEqualTo("Ivan");
            assertThat(response.getPersonPremiums().get(1).getFirstName()).isEqualTo("Anna");
        }

        @Test
        @DisplayName("ValidationError поддерживает адресацию с индексом персоны")
        void shouldSupportPersonIndexedErrorField() {
            var error = TravelCalculatePremiumResponseV3.ValidationError.builder()
                    .field("persons[0].personBirthDate")
                    .message("Must not be empty!")
                    .code("validation.not_null")
                    .build();

            assertThat(error.getField()).isEqualTo("persons[0].personBirthDate");
            assertThat(error.getMessage()).isEqualTo("Must not be empty!");
            assertThat(error.getCode()).isEqualTo("validation.not_null");
        }

        @Test
        @DisplayName("ResponseStatus содержит все 4 значения")
        void shouldHaveAllFourResponseStatuses() {
            assertThat(TravelCalculatePremiumResponseV3.ResponseStatus.values())
                    .containsExactlyInAnyOrder(
                            TravelCalculatePremiumResponseV3.ResponseStatus.SUCCESS,
                            TravelCalculatePremiumResponseV3.ResponseStatus.VALIDATION_ERROR,
                            TravelCalculatePremiumResponseV3.ResponseStatus.DECLINED,
                            TravelCalculatePremiumResponseV3.ResponseStatus.REQUIRES_REVIEW
                    );
        }
    }
}