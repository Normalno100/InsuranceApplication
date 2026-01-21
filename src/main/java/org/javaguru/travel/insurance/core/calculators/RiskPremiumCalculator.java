package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.math.BigDecimal;

/**
 * Интерфейс для расчета стоимости отдельного риска
 *
 * Каждая реализация отвечает за расчет премии конкретного типа риска
 * согласно бизнес-правилам страховой компании
 */
public interface RiskPremiumCalculator {

    /**
     * Код типа риска, который обрабатывает данный калькулятор
     * Например: "TRAVEL_MEDICAL", "SPORT_ACTIVITIES", "EXTREME_SPORT"
     *
     * @return уникальный код риска
     */
    String getRiskCode();

    /**
     * Рассчитывает стоимость премии для данного риска
     *
     * Формула расчета зависит от типа риска и может учитывать:
     * - Базовый тариф (daily rate)
     * - Возраст застрахованного
     * - Страну путешествия
     * - Длительность поездки
     * - Дополнительные риски
     * - Специфичные параметры риска
     *
     * @param request данные заявки на страхование
     * @return стоимость премии для данного риска
     */
    BigDecimal calculatePremium(TravelCalculatePremiumRequest request);

    /**
     * Проверяет, применим ли данный риск для указанной заявки
     *
     * Некоторые риски могут быть недоступны при определенных условиях:
     * - Возрастные ограничения
     * - Географические ограничения
     * - Ограничения по длительности поездки
     * - Конфликты с другими рисками
     *
     * @param request данные заявки на страхование
     * @return true если риск применим, false иначе
     */
    default boolean isApplicable(TravelCalculatePremiumRequest request) {
        return true; // По умолчанию все риски применимы
    }
}