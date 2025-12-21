package org.javaguru.travel.insurance.dto.v2;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Запрос на расчет страховой премии (версия 2)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TravelCalculatePremiumRequestV2 {

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

    // Дополнительные риски
    private List<String> selectedRisks;

    // Валюта и промо-код
    private String currency;
    private String promoCode;

    // Групповое страхование
    private Integer personsCount;
    private Boolean isCorporate;
}