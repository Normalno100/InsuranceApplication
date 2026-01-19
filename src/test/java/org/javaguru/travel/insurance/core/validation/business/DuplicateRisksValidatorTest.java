package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.ValidationContext;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DuplicateRisksValidator Tests")
class DuplicateRisksValidatorTest {

    private final DuplicateRisksValidator validator = new DuplicateRisksValidator();
    private final ValidationContext context = new ValidationContext();

    @Nested
    @DisplayName("Valid Cases - No Duplicates")
    class ValidCases {

        @Test
        @DisplayName("should pass when no risks selected")
        void shouldPassWhenNoRisks() {
            var request = createRequest(null);

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when empty list")
        void shouldPassWhenEmptyList() {
            var request = createRequest(List.of());

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when single risk")
        void shouldPassWhenSingleRisk() {
            var request = createRequest(List.of("SPORT_ACTIVITIES"));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should pass when multiple unique risks")
        void shouldPassWhenMultipleUniqueRisks() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "EXTREME_SPORT",
                    "CHRONIC_DISEASES"
            ));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("should ignore null and empty elements")
        void shouldIgnoreNullAndEmptyElements() {
            var request = createRequest(Arrays.asList(
                    "SPORT_ACTIVITIES",
                    null,
                    "",
                    "EXTREME_SPORT",
                    "   "
            ));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isTrue();
        }
    }

    @Nested
    @DisplayName("Invalid Cases - Duplicates Found")
    class InvalidCases {

        @Test
        @DisplayName("should fail when same risk appears twice")
        void shouldFailWhenDuplicateRisk() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "SPORT_ACTIVITIES"
            ));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(1);

            var error = result.getErrors().get(0);
            assertThat(error.getField()).isEqualTo("selectedRisks[1]");
            assertThat(error.getMessage()).contains("SPORT_ACTIVITIES");
            assertThat(error.getMessage()).contains("multiple times");
        }

        @Test
        @DisplayName("should fail when risk appears three times")
        void shouldFailWhenTripleRisk() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "EXTREME_SPORT",
                    "SPORT_ACTIVITIES",
                    "CHRONIC_DISEASES",
                    "SPORT_ACTIVITIES"
            ));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(2); // Индексы 2 и 4

            // Проверяем что ошибки для правильных индексов
            var fields = result.getErrors().stream()
                    .map(e -> e.getField())
                    .toList();

            assertThat(fields).contains("selectedRisks[2]", "selectedRisks[4]");
        }

        @Test
        @DisplayName("should fail when multiple risks duplicated")
        void shouldFailWhenMultipleDuplicates() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "EXTREME_SPORT",
                    "SPORT_ACTIVITIES",  // дубликат
                    "CHRONIC_DISEASES",
                    "EXTREME_SPORT"      // дубликат
            ));

            var result = validator.validate(request, context);

            assertThat(result.isValid()).isFalse();
            assertThat(result.getErrors()).hasSize(2);

            // Одна ошибка для SPORT_ACTIVITIES[2]
            // Одна ошибка для EXTREME_SPORT[4]
            var fields = result.getErrors().stream()
                    .map(e -> e.getField())
                    .toList();

            assertThat(fields).contains("selectedRisks[2]", "selectedRisks[4]");
        }

        @Test
        @DisplayName("should include risk code in error parameters")
        void shouldIncludeRiskCodeInParameters() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "SPORT_ACTIVITIES"
            ));

            var result = validator.validate(request, context);

            var error = result.getErrors().get(0);
            assertThat(error.getParameters()).containsEntry("riskCode", "SPORT_ACTIVITIES");
            assertThat(error.getParameters()).containsEntry("index", 1);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("should handle duplicates after filtering null/empty")
        void shouldHandleDuplicatesAfterFiltering() {
            var request = createRequest(Arrays.asList(
                    "SPORT_ACTIVITIES",
                    null,
                    "",
                    "SPORT_ACTIVITIES"
            ));

            var result = validator.validate(request, context);

            // После фильтрации остаётся ["SPORT", "SPORT"]
            // Дубликат на индексе 3 (в оригинальном списке)
            assertThat(result.isValid()).isFalse();
        }

        @Test
        @DisplayName("should be case-sensitive")
        void shouldBeCaseSensitive() {
            var request = createRequest(List.of(
                    "SPORT_ACTIVITIES",
                    "sport_activities"
            ));

            var result = validator.validate(request, context);

            // Разный регистр = разные риски, не дубликаты
            assertThat(result.isValid()).isTrue();
        }
    }

    // Helper method
    private TravelCalculatePremiumRequest createRequest(List<String> risks) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now())
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .selectedRisks(risks)
                .build();
    }
}