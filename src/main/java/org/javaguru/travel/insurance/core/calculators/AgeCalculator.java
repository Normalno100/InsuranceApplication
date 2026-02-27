package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.AgeCoefficientRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Калькулятор возраста и возрастных коэффициентов.
 *
 * ТАБЛИЦА КОЭФФИЦИЕНТОВ (хранится в age_coefficients):
 *   0–5 лет:   1.1  (Infants and toddlers)
 *   6–17 лет:  0.9  (Children and teenagers)
 *   18–30 лет: 1.0  (Young adults)
 *   31–40 лет: 1.1  (Adults)
 *   41–50 лет: 1.3  (Middle-aged)
 *   51–60 лет: 1.6  (Senior)
 *   61–70 лет: 2.0  (Elderly)
 *   71–80 лет: 2.5  (Very elderly)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgeCalculator {

    private final AgeCoefficientRepository ageCoefficientRepository;

    // ========================================
    // ПУБЛИЧНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Рассчитывает возраст на указанную дату.
     */
    public int calculateAge(LocalDate birthDate, LocalDate referenceDate) {
        if (birthDate == null) {
            throw new IllegalArgumentException("Birth date cannot be null");
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        if (birthDate.isAfter(referenceDate)) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
        return Period.between(birthDate, referenceDate).getYears();
    }

    /**
     * Получает коэффициент для указанного возраста на текущую дату.
     * Читает из БД (age_coefficients), при отсутствии данных — fallback на хардкод.
     *
     * @param age возраст в годах
     * @return коэффициент
     */
    public BigDecimal getAgeCoefficient(int age) {
        return getAgeCoefficient(age, LocalDate.now());
    }

    /**
     * Получает коэффициент для указанного возраста на указанную дату.
     * Читает из таблицы age_coefficients.
     * При отсутствии данных — fallback на захардкоженные значения.
     *
     * @param age  возраст в годах
     * @param date дата, на которую нужен коэффициент (обычно agreementDateFrom)
     * @return коэффициент
     */
    public BigDecimal getAgeCoefficient(int age, LocalDate date) {
        return getAgeCoefficient(age, date, true);
    }

    /**
     * Получает коэффициент для указанного возраста.
     *
     * task_116: если enabled=false, возвращает BigDecimal.ONE (коэффициент отключён).
     *
     * @param age     возраст в годах
     * @param date    дата, на которую нужен коэффициент
     * @param enabled true = применять коэффициент, false = вернуть 1.0
     * @return коэффициент (или 1.0 если отключён)
     */
    public BigDecimal getAgeCoefficient(int age, LocalDate date, boolean enabled) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (age > 80) {
            throw new IllegalArgumentException("Insurance not available for persons over 80 years old");
        }

        if (!enabled) {
            log.debug("Age coefficient is DISABLED (enabled=false). Returning 1.0 for age {}", age);
            return BigDecimal.ONE;
        }

        // Сначала пробуем прочитать из БД
        var entityOpt = ageCoefficientRepository.findCoefficientForAge(age, date);

        if (entityOpt.isPresent()) {
            BigDecimal coefficient = entityOpt.get().getCoefficient();
            log.debug("Age coefficient for age {} on {}: {} (from DB)", age, date, coefficient);
            return coefficient;
        }

        // Fallback на хардкод
        log.warn("No age coefficient found in DB for age {} on {}. Using hardcoded fallback.", age, date);
        return getAgeCoefficientFallback(age);
    }

    /**
     * Получает описание возрастной группы.
     */
    public String getAgeGroupDescription(int age) {
        if (age <= 5)  return "Infants and toddlers";
        if (age <= 17) return "Children and teenagers";
        if (age <= 30) return "Young adults";
        if (age <= 40) return "Adults";
        if (age <= 50) return "Middle-aged";
        if (age <= 60) return "Senior";
        if (age <= 70) return "Elderly";
        return "Very elderly";
    }

    /**
     * Валидирует возраст для страхования.
     */
    public boolean isAgeValid(int age) {
        return age >= 0 && age <= 80;
    }

    /**
     * Рассчитывает возраст и коэффициент на дату agreementDateFrom.
     * Коэффициент всегда активен (стандартный вызов).
     *
     * @param birthDate     дата рождения
     * @param referenceDate дата начала поездки
     * @return объект с возрастом, коэффициентом и описанием группы
     */
    public AgeCalculationResult calculateAgeAndCoefficient(LocalDate birthDate, LocalDate referenceDate) {
        return calculateAgeAndCoefficient(birthDate, referenceDate, true);
    }

    /**
     * Рассчитывает возраст и коэффициент на дату agreementDateFrom.
     *
     * @param birthDate     дата рождения
     * @param referenceDate дата начала поездки
     * @param enabled       применять ли коэффициент
     * @return объект с возрастом, коэффициентом и описанием группы
     */
    public AgeCalculationResult calculateAgeAndCoefficient(
            LocalDate birthDate,
            LocalDate referenceDate,
            boolean enabled) {

        int age = calculateAge(birthDate, referenceDate);
        BigDecimal coefficient = getAgeCoefficient(age, referenceDate, enabled);
        String description = getAgeGroupDescription(age);

        return new AgeCalculationResult(age, coefficient, description);
    }

    // ========================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Fallback-метод: возвращает захардкоженный коэффициент.
     * Используется если в БД нет данных.
     *
     * ВАЖНО: поддерживать синхронизацию с данными миграции V_113__create_age_coefficients.sql
     */
    private BigDecimal getAgeCoefficientFallback(int age) {
        if (age <= 5)  return new BigDecimal("1.1");
        if (age <= 17) return new BigDecimal("0.9");
        if (age <= 30) return new BigDecimal("1.0");
        if (age <= 40) return new BigDecimal("1.1");
        if (age <= 50) return new BigDecimal("1.3");
        if (age <= 60) return new BigDecimal("1.6");
        if (age <= 70) return new BigDecimal("2.0");
        return new BigDecimal("2.5");
    }

    // ========================================
    // РЕЗУЛЬТИРУЮЩИЙ ТИП
    // ========================================

    /**
     * Результат расчёта возраста.
     */
    public record AgeCalculationResult(
            int age,
            BigDecimal coefficient,
            String description
    ) {}
}