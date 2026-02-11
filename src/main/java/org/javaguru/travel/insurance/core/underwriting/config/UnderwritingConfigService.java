package org.javaguru.travel.insurance.core.underwriting.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.UnderwritingRuleConfigRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Сервис для загрузки конфигурации правил из БД
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UnderwritingConfigService {

    private final UnderwritingRuleConfigRepository configRepository;

    /**
     * Получает integer параметр правила
     */
    @Cacheable(value = "underwritingConfig", key = "#ruleName + '_' + #parameterName")
    public int getIntParameter(String ruleName, String parameterName, int defaultValue) {
        return getParameter(ruleName, parameterName, LocalDate.now())
                .map(value -> {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse {} parameter {} as int: {}, using default: {}",
                                ruleName, parameterName, value, defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.debug("Parameter {} not found for rule {}, using default: {}",
                            parameterName, ruleName, defaultValue);
                    return defaultValue;
                });
    }

    /**
     * Получает long параметр правила
     */
    @Cacheable(value = "underwritingConfig", key = "#ruleName + '_' + #parameterName + '_long'")
    public long getLongParameter(String ruleName, String parameterName, long defaultValue) {
        return getParameter(ruleName, parameterName, LocalDate.now())
                .map(value -> {
                    try {
                        return Long.parseLong(value);
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse {} parameter {} as long: {}, using default: {}",
                                ruleName, parameterName, value, defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.debug("Parameter {} not found for rule {}, using default: {}",
                            parameterName, ruleName, defaultValue);
                    return defaultValue;
                });
    }

    /**
     * Получает BigDecimal параметр правила
     */
    @Cacheable(value = "underwritingConfig", key = "#ruleName + '_' + #parameterName + '_decimal'")
    public BigDecimal getBigDecimalParameter(String ruleName, String parameterName, BigDecimal defaultValue) {
        return getParameter(ruleName, parameterName, LocalDate.now())
                .map(value -> {
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        log.warn("Cannot parse {} parameter {} as BigDecimal: {}, using default: {}",
                                ruleName, parameterName, value, defaultValue);
                        return defaultValue;
                    }
                })
                .orElseGet(() -> {
                    log.debug("Parameter {} not found for rule {}, using default: {}",
                            parameterName, ruleName, defaultValue);
                    return defaultValue;
                });
    }

    /**
     * Получает string параметр правила
     */
    @Cacheable(value = "underwritingConfig", key = "#ruleName + '_' + #parameterName + '_string'")
    public String getStringParameter(String ruleName, String parameterName, String defaultValue) {
        return getParameter(ruleName, parameterName, LocalDate.now())
                .orElseGet(() -> {
                    log.debug("Parameter {} not found for rule {}, using default: {}",
                            parameterName, ruleName, defaultValue);
                    return defaultValue;
                });
    }

    /**
     * Получает параметр из БД
     */
    private java.util.Optional<String> getParameter(String ruleName, String parameterName, LocalDate date) {
        return configRepository.findActiveConfig(ruleName, parameterName, date)
                .map(config -> {
                    log.debug("Loaded config: {} {} = {}",
                            ruleName, parameterName, config.getParameterValue());
                    return config.getParameterValue();
                });
    }
}