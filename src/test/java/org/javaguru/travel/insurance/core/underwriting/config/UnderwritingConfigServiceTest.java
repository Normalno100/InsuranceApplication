package org.javaguru.travel.insurance.core.underwriting.config;

import org.javaguru.travel.insurance.core.domain.entities.UnderwritingRuleConfigEntity;
import org.javaguru.travel.insurance.core.repositories.UnderwritingRuleConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@DisplayName("Underwriting Config Service Tests")
class UnderwritingConfigServiceTest {

    @Mock
    private UnderwritingRuleConfigRepository repository;

    private UnderwritingConfigService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new UnderwritingConfigService(repository);
    }

    @Test
    @DisplayName("should load int parameter from database")
    void shouldLoadIntParameter() {
        // Given
        UnderwritingRuleConfigEntity config = new UnderwritingRuleConfigEntity();
        config.setRuleName("AgeRule");
        config.setParameterName("MAX_AGE");
        config.setParameterValue("80");

        when(repository.findActiveConfig(eq("AgeRule"), eq("MAX_AGE"), any(LocalDate.class)))
                .thenReturn(Optional.of(config));

        // When
        int value = service.getIntParameter("AgeRule", "MAX_AGE", 75);

        // Then
        assertThat(value).isEqualTo(80);
    }

    @Test
    @DisplayName("should return default value when config not found")
    void shouldReturnDefaultWhenNotFound() {
        // Given
        when(repository.findActiveConfig(any(), any(), any()))
                .thenReturn(Optional.empty());

        // When
        int value = service.getIntParameter("SomeRule", "SOME_PARAM", 100);

        // Then
        assertThat(value).isEqualTo(100);
    }

    @Test
    @DisplayName("should load BigDecimal parameter")
    void shouldLoadBigDecimalParameter() {
        // Given
        UnderwritingRuleConfigEntity config = new UnderwritingRuleConfigEntity();
        config.setParameterValue("100000");

        when(repository.findActiveConfig(any(), any(), any()))
                .thenReturn(Optional.of(config));

        // When
        BigDecimal value = service.getBigDecimalParameter(
                "MedicalCoverageRule", "THRESHOLD", new BigDecimal("50000"));

        // Then
        assertThat(value).isEqualByComparingTo("100000");
    }

    @Test
    @DisplayName("should handle invalid number format")
    void shouldHandleInvalidNumberFormat() {
        // Given
        UnderwritingRuleConfigEntity config = new UnderwritingRuleConfigEntity();
        config.setParameterValue("not_a_number");

        when(repository.findActiveConfig(any(), any(), any()))
                .thenReturn(Optional.of(config));

        // When
        int value = service.getIntParameter("Rule", "PARAM", 50);

        // Then
        assertThat(value).isEqualTo(50); // Should return default
    }
}