package org.javaguru.travel.insurance.application.validation.domain.risks;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // Позволяет настраивать стабы, которые не вызываются в каждом тесте
@DisplayName("SelectedRisksValidator")
class SelectedRisksValidatorTest {

    @Mock
    private ReferenceDataPort referenceDataPort;

    private SelectedRisksValidator validator;
    private ValidationContext context;

    private static final LocalDate TODAY = TestConstants.TEST_DATE;
    private static final LocalDate DATE_FROM = TODAY.plusDays(30);

    @BeforeEach
    void setUp() {
        validator = new SelectedRisksValidator(referenceDataPort);
        context = new ValidationContext(TestConstants.TEST_CLOCK);
    }

    @Nested
    @DisplayName("Пустой или null список рисков")
    class EmptyOrNullRisks {
        @Test
        @DisplayName("должен пройти когда selectedRisks = null")
        void shouldPassWhenSelectedRisksNull() {
            var request = requestWithRisks(null);
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).isEmpty();
        }

        @Test
        @DisplayName("должен пройти когда selectedRisks = пустой список")
        void shouldPassWhenSelectedRisksEmpty() {
            var request = requestWithRisks(Collections.emptyList());
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("TRAVEL_MEDICAL в selectedRisks")
    class MandatoryRiskValidation {
        @Test
        @DisplayName("должен вернуть ошибку когда TRAVEL_MEDICAL передан явно")
        void shouldFailWhenTravelMedicalInSelectedRisks() {
            var request = requestWithRisks(List.of("TRAVEL_MEDICAL"));
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).isNotEmpty();
            assertThat(errors).anyMatch(e -> e.getMessage().toLowerCase().contains("mandatory"));
        }
    }

    @Nested
    @DisplayName("Дубликаты в списке рисков")
    class DuplicateRisks {
        @Test
        @DisplayName("должен вернуть ошибку при дублировании риска")
        void shouldFailWhenDuplicateRisk() {
            stubRiskFound("SPORT_ACTIVITIES", false);
            var request = requestWithRisks(List.of("SPORT_ACTIVITIES", "SPORT_ACTIVITIES"));
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).anyMatch(e -> e.getMessage().toLowerCase().contains("multiple"));
        }

        @Test
        @DisplayName("должен указать правильный индекс дубликата в поле ошибки")
        void shouldReportCorrectIndexForDuplicate() {
            stubRiskFound("LUGGAGE_LOSS", false);
            var request = requestWithRisks(List.of("LUGGAGE_LOSS", "LUGGAGE_LOSS"));
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).anyMatch(e -> e.getField().contains("[1]"));
        }
    }

    @Nested
    @DisplayName("Несуществующий код риска")
    class NonExistentRisk {
        @Test
        @DisplayName("должен вернуть ошибку когда риск не найден в справочнике")
        void shouldFailWhenRiskNotFound() {
            // Настраиваем, чтобы для любого кода возвращался пустой Optional
            when(referenceDataPort.findRisk(any(), eq(DATE_FROM))).thenReturn(Optional.empty());

            var request = requestWithRisks(List.of("UNKNOWN_RISK"));
            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).anyMatch(e -> e.getMessage().toLowerCase().contains("not found"));
        }
    }

    @Nested
    @DisplayName("Корректные сценарии")
    class ValidScenarios {
        @Test
        @DisplayName("должен пройти для нескольких уникальных корректных рисков")
        void shouldPassForMultipleUniqueValidRisks() {
            stubRiskFound("SPORT_ACTIVITIES", false);
            stubRiskFound("LUGGAGE_LOSS", false);

            var request = requestWithRisks(List.of("SPORT_ACTIVITIES", "LUGGAGE_LOSS"));
            List<ValidationError> errors = validator.validate(request, context);

            assertThat(errors).isEmpty();
        }
    }

    @Nested
    @DisplayName("Null и пустые элементы в списке (игнорируются)")
    class NullAndBlankElements {
        @Test
        @DisplayName("должен игнорировать null элементы в списке")
        void shouldIgnoreNullElementsInList() {
            stubRiskFound("SPORT_ACTIVITIES", false);
            var request = requestWithRisks(Arrays.asList("SPORT_ACTIVITIES", null));
            List<ValidationError> errors = validator.validate(request, context);
            assertThat(errors).isEmpty();
        }
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private TravelCalculatePremiumRequest requestWithRisks(List<String> risks) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(14))
                .selectedRisks(risks)
                .build();
    }

    /**
     * Исправленный стаб. Использует argThat для сравнения значений RiskCode,
     * что решает проблему с несовпадением ссылок объектов.
     */
    private void stubRiskFound(String riskCodeStr, boolean mandatory) {
        Risk risk = mock(Risk.class);
        when(risk.isMandatory()).thenReturn(mandatory);
        when(risk.getCode()).thenReturn(new RiskCode(riskCodeStr));

        // Добавляем коэффициент, чтобы избежать NullPointerException, если валидатор его читает
        var coeff = new org.javaguru.travel.insurance.domain.model.valueobject.Coefficient(new BigDecimal("1.0"));
        when(risk.getBaseCoefficient()).thenReturn(coeff);

        // Ключевое изменение: сравниваем по значению внутри RiskCode
        when(referenceDataPort.findRisk(
                argThat(rc -> rc != null && riskCodeStr.equals(rc.value())),
                eq(DATE_FROM)
        )).thenReturn(Optional.of(risk));
    }
}