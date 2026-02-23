package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.domain.entities.CountryDefaultDayPremiumEntity;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.CountryDefaultDayPremiumRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Сервис для работы с дефолтными дневными премиями по странам.
 *
 * ЦЕЛЬ:
 * Предоставить альтернативный режим расчёта премии — вместо стандартного
 * подхода через medical_risk_limit_levels использовать базовую дневную ставку
 * конкретной страны (country_default_day_premiums).
 *
 * ФОРМУЛА (COUNTRY_DEFAULT режим):
 * ПРЕМИЯ = DEFAULT_DAY_PREMIUM × КОЭФФ_ВОЗРАСТА × КОЭФФ_ДЛИТЕЛЬНОСТИ
 *          × (1 + СУММА_РИСКОВ) × ДНИ - СКИДКА_ПАКЕТА
 *
 * ОТЛИЧИЕ от стандартного расчёта:
 * - Стандарт: базовая ставка берётся из medical_risk_limit_levels (зависит от уровня покрытия)
 * - COUNTRY_DEFAULT: базовая ставка берётся из country_default_day_premiums (зависит от страны)
 * - Коэффициент страны (risk_coefficient) НЕ применяется повторно, т.к. уже "запечён" в DEFAULT_DAY_PREMIUM
 *
 * FALLBACK логика:
 * Если для страны нет записи в country_default_day_premiums, сервис возвращает
 * Optional.empty() — вызывающий код должен откатиться на стандартный расчёт.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CountryDefaultDayPremiumService {

    private final CountryDefaultDayPremiumRepository repository;

    // ========================================
    // ОСНОВНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Находит дефолтную дневную премию для страны на указанную дату.
     *
     * @param countryIsoCode ISO код страны (например "ES", "US")
     * @param date           дата, на которую нужна премия
     * @return Optional с результатом, или empty если нет данных для страны
     */
    public Optional<DefaultPremiumResult> findDefaultDayPremium(
            String countryIsoCode,
            LocalDate date) {

        log.debug("Looking up default day premium for country: {}, date: {}", countryIsoCode, date);

        if (countryIsoCode == null || countryIsoCode.isBlank()) {
            log.warn("Country ISO code is null or blank — cannot find default premium");
            return Optional.empty();
        }

        Optional<CountryDefaultDayPremiumEntity> entityOpt =
                repository.findActiveByCountryAndDate(countryIsoCode.toUpperCase(), date);

        if (entityOpt.isEmpty()) {
            log.info("No default day premium found for country '{}' on date {}. " +
                    "Fallback to standard calculation.", countryIsoCode, date);
            return Optional.empty();
        }

        CountryDefaultDayPremiumEntity entity = entityOpt.get();

        log.debug("Found default day premium for country '{}': {} {}",
                countryIsoCode, entity.getDefaultDayPremium(), entity.getCurrency());

        return Optional.of(new DefaultPremiumResult(
                entity.getCountryIsoCode(),
                entity.getDefaultDayPremium(),
                entity.getCurrency(),
                entity.getDescription()
        ));
    }

    /**
     * Находит дефолтную дневную премию для страны на текущую дату.
     */
    public Optional<DefaultPremiumResult> findDefaultDayPremium(String countryIsoCode) {
        return findDefaultDayPremium(countryIsoCode, LocalDate.now());
    }

    /**
     * Проверяет наличие дефолтной дневной премии для страны.
     *
     * @param countryIsoCode ISO код страны
     * @param date           дата проверки
     * @return true если для страны есть активная запись
     */
    public boolean hasDefaultDayPremium(String countryIsoCode, LocalDate date) {
        return findDefaultDayPremium(countryIsoCode, date).isPresent();
    }

    /**
     * Рассчитывает итоговую базовую премию в режиме COUNTRY_DEFAULT.
     *
     * ФОРМУЛА:
     * basePremium = defaultDayPremium × ageCoefficient × durationCoefficient × days
     *
     * ВАЖНО: коэффициент страны (countryRiskCoefficient) НЕ применяется,
     * т.к. он уже учтён в defaultDayPremium при формировании справочника.
     *
     * @param defaultDayPremium   дефолтная дневная ставка страны
     * @param ageCoefficient      возрастной коэффициент
     * @param durationCoefficient коэффициент длительности поездки
     * @param days                количество дней поездки
     * @return рассчитанная базовая премия до применения доп. рисков и скидок
     */
    public BigDecimal calculateBasePremium(
            BigDecimal defaultDayPremium,
            BigDecimal ageCoefficient,
            BigDecimal durationCoefficient,
            int days) {

        if (defaultDayPremium == null || defaultDayPremium.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Default day premium must be positive, got: " + defaultDayPremium);
        }
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be positive, got: " + days);
        }

        BigDecimal result = defaultDayPremium
                .multiply(ageCoefficient)
                .multiply(durationCoefficient)
                .multiply(BigDecimal.valueOf(days))
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("COUNTRY_DEFAULT base premium: {} × {} × {} × {} days = {}",
                defaultDayPremium, ageCoefficient, durationCoefficient, days, result);

        return result;
    }

    /**
     * Строит строку формулы для отображения в ответе (поле calculationFormula).
     */
    public String buildFormula(
            BigDecimal defaultDayPremium,
            BigDecimal ageCoefficient,
            BigDecimal durationCoefficient,
            int days,
            BigDecimal additionalRisksCoefficient,
            BigDecimal bundleDiscount) {

        StringBuilder formula = new StringBuilder("Premium = ")
                .append(String.format("%.2f", defaultDayPremium))
                .append(" (country default rate)")
                .append(" × ")
                .append(String.format("%.4f", ageCoefficient))
                .append(" (age)")
                .append(" × ")
                .append(String.format("%.4f", durationCoefficient))
                .append(" (duration)");

        if (additionalRisksCoefficient != null
                && additionalRisksCoefficient.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.4f", additionalRisksCoefficient))
                    .append(") (risks)");
        }

        formula.append(" × ").append(days).append(" days");

        if (bundleDiscount != null && bundleDiscount.compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" - ")
                    .append(String.format("%.2f", bundleDiscount))
                    .append(" (bundle discount)");
        }

        return formula.toString();
    }

    // ========================================
    // РЕЗУЛЬТИРУЮЩИЕ ТИПЫ
    // ========================================

    /**
     * Результат поиска дефолтной дневной премии.
     */
    public record DefaultPremiumResult(
            String countryIsoCode,
            BigDecimal defaultDayPremium,
            String currency,
            String description
    ) {
        /**
         * Проверяет корректность данных.
         */
        public boolean isValid() {
            return countryIsoCode != null
                    && !countryIsoCode.isBlank()
                    && defaultDayPremium != null
                    && defaultDayPremium.compareTo(BigDecimal.ZERO) > 0;
        }
    }
}
