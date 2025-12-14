package org.javaguru.travel.insurance.core;

import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TravelCalculatePremiumRequestValidatorV2ImplTest {

    @Mock
    private ValidationRuleV2 rule1;

    @Mock
    private ValidationRuleV2 rule2;

    @Mock
    private ValidationRuleV2 rule3;

    private TravelCalculatePremiumRequestValidatorV2Impl validator;

    @BeforeEach
    void setUp() {
        validator = new TravelCalculatePremiumRequestValidatorV2Impl(
                List.of(rule1, rule2, rule3)
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoValidationErrors() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();

        when(rule1.validate(any())).thenReturn(Optional.empty());
        when(rule2.validate(any())).thenReturn(Optional.empty());
        when(rule3.validate(any())).thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldReturnOneErrorWhenOneRuleFails() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        ValidationError expectedError = new ValidationError("field1", "Error message");

        when(rule1.validate(any())).thenReturn(Optional.of(expectedError));
        when(rule2.validate(any())).thenReturn(Optional.empty());
        when(rule3.validate(any())).thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(1, errors.size());
        assertEquals("field1", errors.get(0).getField());
        assertEquals("Error message", errors.get(0).getMessage());
    }

    @Test
    void shouldReturnMultipleErrorsWhenMultipleRulesFail() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        ValidationError error1 = new ValidationError("field1", "Error 1");
        ValidationError error2 = new ValidationError("field2", "Error 2");

        when(rule1.validate(any())).thenReturn(Optional.of(error1));
        when(rule2.validate(any())).thenReturn(Optional.of(error2));
        when(rule3.validate(any())).thenReturn(Optional.empty());

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(2, errors.size());
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("field1")));
        assertTrue(errors.stream().anyMatch(e -> e.getField().equals("field2")));
    }

    @Test
    void shouldReturnAllErrorsWhenAllRulesFail() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        ValidationError error1 = new ValidationError("field1", "Error 1");
        ValidationError error2 = new ValidationError("field2", "Error 2");
        ValidationError error3 = new ValidationError("field3", "Error 3");

        when(rule1.validate(any())).thenReturn(Optional.of(error1));
        when(rule2.validate(any())).thenReturn(Optional.of(error2));
        when(rule3.validate(any())).thenReturn(Optional.of(error3));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals(3, errors.size());
    }

    @Test
    void shouldApplyAllRulesEvenIfSomeFail() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        ValidationError error = new ValidationError("field1", "Error");

        when(rule1.validate(any())).thenReturn(Optional.of(error));
        when(rule2.validate(any())).thenReturn(Optional.empty());
        when(rule3.validate(any())).thenReturn(Optional.empty());

        // When
        validator.validate(request);

        // Then
        // Все правила должны быть применены (проверяется через mockito)
        // даже если первое правило вернуло ошибку
    }

    @Test
    void shouldHandleEmptyRulesList() {
        // Given
        TravelCalculatePremiumRequestValidatorV2Impl emptyValidator =
                new TravelCalculatePremiumRequestValidatorV2Impl(List.of());
        TravelCalculatePremiumRequestV2 request = createValidRequest();

        // When
        List<ValidationError> errors = emptyValidator.validate(request);

        // Then
        assertTrue(errors.isEmpty());
    }

    @Test
    void shouldPreserveErrorOrder() {
        // Given
        TravelCalculatePremiumRequestV2 request = createValidRequest();
        ValidationError error1 = new ValidationError("field1", "Error 1");
        ValidationError error2 = new ValidationError("field2", "Error 2");
        ValidationError error3 = new ValidationError("field3", "Error 3");

        when(rule1.validate(any())).thenReturn(Optional.of(error1));
        when(rule2.validate(any())).thenReturn(Optional.of(error2));
        when(rule3.validate(any())).thenReturn(Optional.of(error3));

        // When
        List<ValidationError> errors = validator.validate(request);

        // Then
        assertEquals("field1", errors.get(0).getField());
        assertEquals("field2", errors.get(1).getField());
        assertEquals("field3", errors.get(2).getField());
    }

    private TravelCalculatePremiumRequestV2 createValidRequest() {
        return TravelCalculatePremiumRequestV2.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(1))
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .build();
    }
}