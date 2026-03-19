package org.javaguru.travel.insurance;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Единый файл констант для тестов.
 *
 * ЭТАП 1 (рефакторинг): Фиксация дат через Clock
 *
 * Все тесты используют TEST_DATE и TEST_CLOCK вместо LocalDate.now(),
 * что гарантирует детерминированность независимо от даты запуска.
 */
public final class TestConstants {

    /**
     * Фиксированная дата для всех тестов.
     * Выбрана как дата первичного аудита (2026-03-18).
     */
    public static final LocalDate TEST_DATE = LocalDate.of(2026, 3, 18);

    /**
     * Фиксированные часы для тестов.
     * Использовать вместо Clock.systemDefaultZone() во всех тестовых классах.
     *
     * Пример использования:
     *   ValidationContext context = new ValidationContext(TestConstants.TEST_CLOCK);
     *   AgeCalculator calc = new AgeCalculator(repo, TestConstants.TEST_CLOCK);
     */
    public static final Clock TEST_CLOCK = Clock.fixed(
            TEST_DATE.atStartOfDay(ZoneOffset.UTC).toInstant(),
            ZoneOffset.UTC
    );

    private TestConstants() {
        // utility class — не инстанцировать
    }
}