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
 * ИЗМЕНЕНИЯ task_113:
 * - getAgeCoefficient(int age) теперь использует AgeCoefficientRepository для
 *   чтения коэффициентов из таблицы age_coefficients (вместо хардкода).
 * - При отсутствии данных в БД — fallback на захардкоженные значения с предупреждением в логах.
 * - calculateAgeAndCoefficient() добавлен overload с передачей referenceDate для
 *   выбора правильной версии коэффициента.
 *
 * ТАБЛИЦА КОЭФФИЦИЕНТОВ (хранится в age_coefficients):
 *   0–5 лет:   1.1 (Infants and toddlers)
 *   6–17 лет:  0.9 (Children and teenagers)
 *   18–30 лет: 1.0 (Young adults)
 *   31–40 лет: 1.1 (Adults)
 *   41–50 лет: 1.3 (Middle-aged)
 *   51–60 лет: 1.6 (Senior)
 *   61–70 лет: 2.0 (Elderly)
 *   71–80 лет: 2.5 (Very elderly)
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
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (age > 80) {
            throw new IllegalArgumentException("Insurance not available for persons over 80 years old");
        }

        // Сначала пробуем прочитать из БД
        var entityOpt = ageCoefficientRepository.findCoefficientForAge(age, date);

        if (entityOpt.isPresent()) {
            BigDecimal coefficient = entityOpt.get().getCoefficient();
            log.debug("Age coefficient for age {} on {}: {} (from DB)", age, date, coefficient);
            return coefficient;
        }

        // Fallback на хардкод (на случай если БД не заполнена или миграция не применена)
        log.warn("No age coefficient found in DB for age {} on {}. Using hardcoded fallback.", age, date);
        return getAgeCoefficientFallback(age);
    }

    /**
     * Получает описание возрастной группы.
     */
    public String getAgeGroupDescription(int age) {
        if (age <= 5) {
            return "Infants and toddlers";
        } else if (age <= 17) {
            return "Children and teenagers";
        } else if (age <= 30) {
            return "Young adults";
        } else if (age <= 40) {
            return "Adults";
        } else if (age <= 50) {
            return "Middle-aged";
        } else if (age <= 60) {
            return "Senior";
        } else if (age <= 70) {
            return "Elderly";
        } else {
            return "Very elderly";
        }
    }

    /**
     * Валидирует возраст для страхования.
     */
    public boolean isAgeValid(int age) {
        return age >= 0 && age <= 80;
    }

    /**
     * Рассчитывает возраст и коэффициент на дату agreementDateFrom.
     * Передаёт referenceDate в getAgeCoefficient для выбора правильной версии
     * коэффициента из таблицы age_coefficients.
     *
     * @param birthDate     дата рождения
     * @param referenceDate дата, на которую рассчитывается возраст (agreementDateFrom)
     * @return объект с возрастом, коэффициентом и описанием группы
     */
    public AgeCalculationResult calculateAgeAndCoefficient(LocalDate birthDate, LocalDate referenceDate) {
        int age = calculateAge(birthDate, referenceDate);
        // task_113: передаём referenceDate в getAgeCoefficient, чтобы использовать
        // актуальную версию коэффициента из БД на дату начала поездки
        BigDecimal coefficient = getAgeCoefficient(age, referenceDate);
        String description = getAgeGroupDescription(age);

        return new AgeCalculationResult(age, coefficient, description);
    }

    // ========================================
    // ПРИВАТНЫЕ МЕТОДЫ
    // ========================================

    /**
     * Fallback-метод: возвращает захардкоженный коэффициент.
     * Используется если в БД нет данных для запрошенного возраста/даты.
     *
     * ВАЖНО: поддерживать синхронизацию с данными в миграции V_113__create_age_coefficients.sql
     */
    private BigDecimal getAgeCoefficientFallback(int age) {
        if (age <= 5) {
            return new BigDecimal("1.1");
        } else if (age <= 17) {
            return new BigDecimal("0.9");
        } else if (age <= 30) {
            return new BigDecimal("1.0");
        } else if (age <= 40) {
            return new BigDecimal("1.1");
        } else if (age <= 50) {
            return new BigDecimal("1.3");
        } else if (age <= 60) {
            return new BigDecimal("1.6");
        } else if (age <= 70) {
            return new BigDecimal("2.0");
        } else {
            return new BigDecimal("2.5");
        }
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