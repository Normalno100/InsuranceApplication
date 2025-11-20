package org.javaguru.travel.insurance.core.calculators;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

/**
 * Калькулятор возраста и возрастных коэффициентов
 */
@Component
@RequiredArgsConstructor
public class AgeCalculator {

    /**
     * Рассчитывает возраст на указанную дату
     *
     * @param birthDate дата рождения
     * @param referenceDate дата, на которую рассчитывается возраст (обычно дата начала поездки)
     * @return возраст в годах
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
     * Получает коэффициент для указанного возраста
     *
     * Таблица коэффициентов:
     * 0-5 лет:   1.1 (младенцы и дети)
     * 6-17 лет:  0.9 (школьники)
     * 18-30 лет: 1.0 (молодые взрослые)
     * 31-40 лет: 1.1 (взрослые)
     * 41-50 лет: 1.3 (средний возраст)
     * 51-60 лет: 1.6 (старший возраст)
     * 61-70 лет: 2.0 (пожилые)
     * 71-80 лет: 2.5 (очень пожилые)
     *
     * @param age возраст в годах
     * @return коэффициент
     */
    public BigDecimal getAgeCoefficient(int age) {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
        if (age > 80) {
            throw new IllegalArgumentException("Insurance not available for persons over 80 years old");
        }

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

    /**
     * Получает описание возрастной группы
     *
     * @param age возраст
     * @return название группы
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
     * Валидирует возраст для страхования
     *
     * @param age возраст
     * @return true если возраст подходит для страхования
     */
    public boolean isAgeValid(int age) {
        return age >= 0 && age <= 80;
    }

    /**
     * Рассчитывает возраст и коэффициент одновременно
     *
     * @param birthDate дата рождения
     * @param referenceDate дата на которую рассчитывается возраст
     * @return объект с возрастом и коэффициентом
     */
    public AgeCalculationResult calculateAgeAndCoefficient(LocalDate birthDate, LocalDate referenceDate) {
        int age = calculateAge(birthDate, referenceDate);
        BigDecimal coefficient = getAgeCoefficient(age);
        String description = getAgeGroupDescription(age);

        return new AgeCalculationResult(age, coefficient, description);
    }

    /**
     * Результат расчета возраста
     */
    public record AgeCalculationResult(
            int age,
            BigDecimal coefficient,
            String description
    ) {}
}
