package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CalculationConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Сервис для чтения настроек расчёта премии из таблицы calculation_config.
 *
 * АРХИТЕКТУРА:
 *   1. По умолчанию настройка читается из calculation_config в БД.
 *   2. Результат кешируется (@Cacheable) — изменение в БД применяется
 *      после истечения кеша или перезапуска приложения.
 *   3. Клиент может переопределить настройку для конкретного запроса
 *      через поле applyAgeCoefficient в TravelCalculatePremiumRequest.
 *
 * КЛЮЧИ КОНФИГУРАЦИИ:
 *   AGE_COEFFICIENT_ENABLED — true/false (по умолчанию true)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CalculationConfigService {

    /** Ключ в таблице calculation_config */
    public static final String AGE_COEFFICIENT_ENABLED_KEY = "AGE_COEFFICIENT_ENABLED";

    private final CalculationConfigRepository configRepository;

    // ========================================
    // ПУБЛИЧНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Возвращает, включён ли возрастной коэффициент на уровне системы.
     *
     * Читает значение ключа AGE_COEFFICIENT_ENABLED из БД.
     * При отсутствии записи возвращает true (безопасный дефолт).
     *
     * @param date дата, на которую актуальна настройка (обычно agreementDateFrom)
     * @return true если коэффициент включён
     */
    @Cacheable(value = "calculationConfig", key = "'AGE_COEFFICIENT_ENABLED_' + #date")
    public boolean isAgeCoefficientEnabled(LocalDate date) {
        return getBooleanConfig(AGE_COEFFICIENT_ENABLED_KEY, date, true);
    }

    /**
     * Возвращает, включён ли возрастной коэффициент на текущую дату.
     */
    @Cacheable(value = "calculationConfig", key = "'AGE_COEFFICIENT_ENABLED_TODAY'")
    public boolean isAgeCoefficientEnabled() {
        return isAgeCoefficientEnabled(LocalDate.now());
    }

    /**
     * Определяет, нужно ли применять возрастной коэффициент для конкретного запроса.
     *
     * ПРИОРИТЕТ:
     *   1. Если requestOverride != null → использовать значение из запроса
     *      (клиент явно переопределил настройку).
     *   2. Иначе → читать из БД через isAgeCoefficientEnabled(date).
     *
     * @param requestOverride значение поля applyAgeCoefficient из Request (может быть null)
     * @param date            дата начала поездки (agreementDateFrom)
     * @return true если коэффициент нужно применять
     */
    public boolean resolveAgeCoefficientEnabled(Boolean requestOverride, LocalDate date) {
        if (requestOverride != null) {
            log.debug("AgeCoefficient override from request: {}", requestOverride);
            return requestOverride;
        }
        boolean fromDb = isAgeCoefficientEnabled(date);
        log.debug("AgeCoefficient from DB config for date {}: {}", date, fromDb);
        return fromDb;
    }

    // ========================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Читает boolean-настройку из БД.
     * При отсутствии или ошибке парсинга возвращает defaultValue.
     */
    private boolean getBooleanConfig(String key, LocalDate date, boolean defaultValue) {
        return configRepository.findActiveByKey(key, date)
                .map(entity -> {
                    String value = entity.getConfigValue();
                    try {
                        boolean parsed = Boolean.parseBoolean(value);
                        log.debug("Config '{}' = {} (parsed from '{}')", key, parsed, value);
                        return parsed;
                    } catch (Exception e) {
                        log.warn("Cannot parse config '{}' value '{}' as boolean, using default: {}",
                                key, value, defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.debug("Config '{}' not found in DB for date {}, using default: {}",
                            key, date, defaultValue);
                    return defaultValue;
                });
    }
}
