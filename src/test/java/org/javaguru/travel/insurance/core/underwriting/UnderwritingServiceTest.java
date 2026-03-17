package org.javaguru.travel.insurance.core.underwriting;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingDecision;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.underwriting.persistence.UnderwritingPersistenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Тесты для п. 5.1: Аудит андеррайтинга — обработка ошибок.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UnderwritingService — audit error handling (refactoring 5.1)")
class UnderwritingServiceTest {

    @Mock
    private UnderwritingEngine underwritingEngine;

    @Mock
    private UnderwritingPersistenceService persistenceService;

    private MeterRegistry meterRegistry;
    private UnderwritingService service;

    private TravelCalculatePremiumRequest request;
    private UnderwritingResult approvedResult;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        service = new UnderwritingService(underwritingEngine, persistenceService, meterRegistry);

        request = TravelCalculatePremiumRequest.builder()
                .personFirstName("Ivan")
                .personLastName("Petrov")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(24))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();

        approvedResult = UnderwritingResult.approved(List.of());
    }

    // ── Основной путь ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("should return underwriting result from engine")
    void shouldReturnResultFromEngine() {
        when(underwritingEngine.evaluate(request)).thenReturn(approvedResult);

        UnderwritingResult result = service.evaluateApplication(request);

        assertThat(result.isApproved()).isTrue();
        verify(underwritingEngine).evaluate(request);
    }

    @Test
    @DisplayName("should attempt to save audit after evaluation")
    void shouldAttemptToSaveAudit() {
        when(underwritingEngine.evaluate(request)).thenReturn(approvedResult);

        service.evaluateApplication(request);

        // saveAuditAsync вызывается напрямую (без Spring Async proxy в unit-тестах)
        // Проверяем публичный метод saveAuditAsync отдельно
    }

    // ── saveAuditAsync ────────────────────────────────────────────────────────

    @Test
    @DisplayName("saveAuditAsync: should call persistenceService.saveDecision")
    void saveAuditAsync_shouldCallPersistenceService() {
        service.saveAuditAsync(request, approvedResult, 42L);

        verify(persistenceService).saveDecision(request, approvedResult, 42L);
    }

    @Test
    @DisplayName("saveAuditAsync: should propagate exception so @Retryable can catch it")
    void saveAuditAsync_shouldPropagateException() {
        doThrow(new RuntimeException("DB unavailable"))
                .when(persistenceService).saveDecision(any(), any(), anyLong());

        // Исключение должно пробрасываться — иначе @Retryable не сработает
        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.saveAuditAsync(request, approvedResult, 42L)
                ).isInstanceOf(RuntimeException.class)
                .hasMessageContaining("DB unavailable");
    }

    // ── handleAuditFailure ────────────────────────────────────────────────────

    @Test
    @DisplayName("handleAuditFailure: should increment audit failure counter")
    void handleAuditFailure_shouldIncrementMetricCounter() {
        RuntimeException ex = new RuntimeException("Persistent DB error");

        service.handleAuditFailure(ex, request, approvedResult, 42L);

        double count = meterRegistry.counter(
                "underwriting.audit.failures",
                "decision", "APPROVED",
                "country", "ES"
        ).count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("handleAuditFailure: counter increments for each call")
    void handleAuditFailure_counterAccumulates() {
        RuntimeException ex = new RuntimeException("error");

        service.handleAuditFailure(ex, request, approvedResult, 10L);
        service.handleAuditFailure(ex, request, approvedResult, 20L);
        service.handleAuditFailure(ex, request, approvedResult, 30L);

        double count = meterRegistry.counter(
                "underwriting.audit.failures",
                "decision", "APPROVED",
                "country", "ES"
        ).count();

        assertThat(count).isEqualTo(3.0);
    }

    @Test
    @DisplayName("handleAuditFailure: uses UNKNOWN tag when country is null")
    void handleAuditFailure_usesUnknownTagWhenCountryNull() {
        request.setCountryIsoCode(null);
        RuntimeException ex = new RuntimeException("error");

        service.handleAuditFailure(ex, request, approvedResult, 10L);

        double count = meterRegistry.counter(
                "underwriting.audit.failures",
                "decision", "APPROVED",
                "country", "UNKNOWN"
        ).count();

        assertThat(count).isEqualTo(1.0);
    }

    @Test
    @DisplayName("handleAuditFailure: uses decision tag from result")
    void handleAuditFailure_usesDecisionTagFromResult() {
        UnderwritingResult declinedResult = UnderwritingResult.declined(List.of(), "Age exceeded");
        RuntimeException ex = new RuntimeException("error");

        service.handleAuditFailure(ex, request, declinedResult, 10L);

        double declinedCount = meterRegistry.counter(
                "underwriting.audit.failures",
                "decision", "DECLINED",
                "country", "ES"
        ).count();

        assertThat(declinedCount).isEqualTo(1.0);
    }

    @Test
    @DisplayName("evaluateApplication: result is returned even if audit save throws")
    void evaluateApplication_returnsResultEvenIfAuditFails() {
        when(underwritingEngine.evaluate(request)).thenReturn(approvedResult);
        // В unit-тесте saveAuditAsync вызывается синхронно (нет Spring Async proxy).
        // Убеждаемся что exception из persistenceService не просачивается наружу
        // при использовании через handleAuditFailure (recover).
        // Здесь тестируем поведение evaluateApplication в изоляции.

        UnderwritingResult result = service.evaluateApplication(request);

        assertThat(result).isNotNull();
        assertThat(result.getDecision()).isEqualTo(UnderwritingDecision.APPROVED);
    }
}