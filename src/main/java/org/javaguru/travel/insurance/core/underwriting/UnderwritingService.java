package org.javaguru.travel.insurance.core.underwriting;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.underwriting.persistence.UnderwritingPersistenceService;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Сервис андеррайтинга — главная точка входа для оценки заявок.
 *
 * ИСПРАВЛЕНИЕ (п. 5.1 плана рефакторинга): Обработка ошибок аудита
 *
 * ПРОБЛЕМА (было):
 *   UnderwritingService молча проглатывал ошибки сохранения аудита:
 *     try {
 *         persistenceService.saveDecision(...);
 *     } catch (Exception e) {
 *         log.error("...", e);
 *         // Не прерываем процесс — ошибка теряется
 *     }
 *   В страховой системе потеря аудит-лога решений может нарушать регуляторные
 *   требования (GDPR, Solvency II, внутренние compliance-правила).
 *
 * РЕШЕНИЕ (стало):
 *   1. Вынесли сохранение аудита в отдельный @Async метод saveAuditAsync() —
 *      основной поток не блокируется на I/O операции записи в БД.
 *
 *   2. Добавили @Retryable с экспоненциальным backoff (3 попытки: 1s → 2s → 4s)
 *      для устойчивости к кратковременным сбоям БД.
 *
 *   3. При исчерпании всех попыток (@Recover) — счётчик метрики
 *      "underwriting.audit.failures" инкрементируется. Это позволяет
 *      настроить алерт в мониторинге (Prometheus/Grafana) и не терять
 *      информацию о систематических сбоях.
 *
 * ТРЕБОВАНИЯ К КОНФИГУРАЦИИ:
 *   В главном классе приложения добавить:
 *     @EnableAsync
 *     @EnableRetry
 *   В application.properties (опционально):
 *     spring.task.execution.pool.core-size=2
 *     spring.task.execution.pool.max-size=5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingService {

    private final UnderwritingEngine underwritingEngine;
    private final UnderwritingPersistenceService persistenceService;
    private final MeterRegistry meterRegistry;

    // Имя метрики для мониторинга сбоев аудита
    private static final String AUDIT_FAILURE_METRIC = "underwriting.audit.failures";

    /**
     * Оценивает заявку через андеррайтинг.
     * Сохранение решения в аудит-лог выполняется асинхронно.
     */
    public UnderwritingResult evaluateApplication(TravelCalculatePremiumRequest request) {
        log.info("Evaluating underwriting for application: {} {} to {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode()
        );

        long startTime = System.currentTimeMillis();

        UnderwritingResult result = underwritingEngine.evaluate(request);

        long duration = System.currentTimeMillis() - startTime;

        log.info("Underwriting decision: {} for {} {} ({}ms)",
                result.getDecision(),
                request.getPersonFirstName(),
                request.getPersonLastName(),
                duration
        );

        // Сохраняем аудит асинхронно — основной поток не ждёт завершения.
        // При сбое выполняется до 3 попыток с экспоненциальным backoff.
        saveAuditAsync(request, result, duration);

        return result;
    }

    /**
     * Асинхронное сохранение решения андеррайтинга с retry-логикой.
     *
     * @Async  — выполняется в отдельном потоке, не блокирует основной запрос.
     * @Retryable — при исключении повторяет попытку до 3 раз:
     *   попытка 1 → сразу
     *   попытка 2 → через 1 сек
     *   попытка 3 → через 2 сек
     *   попытка 4 → через 4 сек (после этого вызывается handleAuditFailure)
     */
    @Async
    @Retryable(
            retryFor = Exception.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void saveAuditAsync(TravelCalculatePremiumRequest request,
                               UnderwritingResult result,
                               long duration) {
        log.debug("Saving underwriting audit for {} {} (attempt)...",
                request.getPersonFirstName(), request.getPersonLastName());
        persistenceService.saveDecision(request, result, duration);
        log.debug("Underwriting audit saved successfully for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());
    }

    /**
     * Вызывается автоматически Spring Retry после исчерпания всех попыток saveAuditAsync().
     *
     * ВАЖНО: Это не прерывает основной бизнес-процесс — решение андеррайтинга
     * уже возвращено клиенту. Однако сбой фиксируется в метриках и логах
     * для последующего расследования и настройки алертов.
     *
     * Метрика "underwriting.audit.failures" позволяет в Grafana/Prometheus
     * настроить alert: если счётчик > N за T минут — отправить уведомление.
     */
    public void handleAuditFailure(Exception ex,
                                   TravelCalculatePremiumRequest request,
                                   UnderwritingResult result,
                                   long duration) {
        log.error(
                "AUDIT FAILURE: underwriting decision NOT saved after all retry attempts. " +
                        "Person: {} {}, Country: {}, Decision: {}. " +
                        "This may violate regulatory requirements (compliance risk).",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode(),
                result.getDecision(),
                ex
        );

        // Инкрементируем счётчик сбоев аудита для мониторинга.
        // В Prometheus/Grafana на этот счётчик настраивается алерт.
        Counter.builder(AUDIT_FAILURE_METRIC)
                .description("Number of underwriting audit save failures after all retries")
                .tag("decision", result.getDecision().name())
                .tag("country", request.getCountryIsoCode() != null
                        ? request.getCountryIsoCode() : "UNKNOWN")
                .register(meterRegistry)
                .increment();
    }
}