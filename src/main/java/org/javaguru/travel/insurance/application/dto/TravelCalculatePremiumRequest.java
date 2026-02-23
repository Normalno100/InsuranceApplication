package org.javaguru.travel.insurance.application.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Запрос на расчет страховой премии
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCalculatePremiumRequest {

    // Персональные данные
    private String personFirstName;
    private String personLastName;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    private String personEmail;
    private String personPhone;

    // Даты поездки
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;

    // Страна и покрытие
    private String countryIsoCode;
    private String medicalRiskLimitLevel;

    /**
     * Опциональное поле: использовать дефолтную дневную премию страны
     * вместо уровня медицинского покрытия (medicalRiskLimitLevel).
     *
     * Если true — расчёт идёт через country_default_day_premiums.
     * Если false или null — используется стандартная логика через medical_risk_limit_levels.
     */
    private Boolean useCountryDefaultPremium;

    // Дополнительные риски
    private List<String> selectedRisks;

    // Валюта и промо-код
    private String currency;
    private String promoCode;

    // Групповое страхование
    private Integer personsCount;
    private Boolean isCorporate;
}