package org.javaguru.travel.insurance.infrastructure.web.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * Form-объект для биндинга Thymeleaf — веб-форма расчёта страховой премии.
 *
 * Соответствует полям TravelCalculatePremiumRequest (V2 API — одна персона).
 * Поддержку нескольких персон через веб-интерфейс рекомендуется добавить
 * позднее как отдельную задачу, после стабилизации V3 API.
 */
@Getter
@Setter
@NoArgsConstructor
public class WebCalculateForm {

    // =========================================================
    // ПЕРСОНАЛЬНЫЕ ДАННЫЕ
    // =========================================================

    private String personFirstName;

    private String personLastName;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate personBirthDate;

    // =========================================================
    // ПАРАМЕТРЫ ПОЕЗДКИ
    // =========================================================

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate agreementDateTo;

    private String countryIsoCode;

    // =========================================================
    // ПОКРЫТИЕ И РИСКИ
    // =========================================================

    private String medicalRiskLimitLevel;

    private List<String> selectedRisks;

    // =========================================================
    // КОММЕРЧЕСКИЕ ПАРАМЕТРЫ
    // =========================================================

    private String currency = "EUR";

    private String promoCode;

    private Integer personsCount;

    private Boolean isCorporate = false;
}