package org.javaguru.travel.insurance.infrastructure.web.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumResponse;
import org.javaguru.travel.insurance.application.service.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.infrastructure.web.form.WebCalculateForm;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Контроллер веб-страниц для расчёта страховой премии.
 *
 * Создан как часть реализации Thymeleaf веб-интерфейса.
 *
 * URL-маппинг:
 *   GET  /                → index.html  — главная страница с формой расчёта
 *   POST /web/calculate   → result.html — страница с результатом расчёта
 *   GET  /web/calculate   → redirect:/  — защита от прямого GET-запроса
 *
 * Работает на базе V2 API (одна персона).
 * Поддержку нескольких персон через веб-интерфейс рекомендуется добавить
 * позднее как отдельную задачу, после стабилизации V3 API.
 */
@Slf4j
@Controller
@RequestMapping("/")
@RequiredArgsConstructor
public class TravelInsuranceWebController {

    private final TravelCalculatePremiumService calculatePremiumService;

    /**
     * GET / — главная страница с пустой формой расчёта.
     */
    @GetMapping
    public String index(Model model) {
        log.debug("GET / — index page");
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new WebCalculateForm());
        }
        addReferenceData(model);
        return "index";
    }

    /**
     * POST /web/calculate — расчёт премии и отображение результата.
     *
     * При ошибках валидации — возврат на форму с подсвеченными полями.
     * При успешном расчёте — отображение result.html.
     */
    @PostMapping("/web/calculate")
    public String calculate(
            @ModelAttribute("form") WebCalculateForm form,
            Model model,
            RedirectAttributes redirectAttributes) {

        log.info("POST /web/calculate — {} {}, country: {}",
                form.getPersonFirstName(), form.getPersonLastName(), form.getCountryIsoCode());

        TravelCalculatePremiumRequest request = toRequest(form);
        TravelCalculatePremiumResponse response = calculatePremiumService.calculatePremium(request, true);

        if (response.getStatus() == TravelCalculatePremiumResponse.ResponseStatus.VALIDATION_ERROR) {
            log.warn("Validation errors: {}", response.getErrors().size());
            model.addAttribute("form", form);
            model.addAttribute("validationErrors", response.getErrors());
            addReferenceData(model);
            return "index";
        }

        model.addAttribute("response", response);
        model.addAttribute("form", form);
        return "result";
    }

    /**
     * GET /web/calculate — защита от прямого GET-запроса (Post/Redirect/Get pattern).
     */
    @GetMapping("/web/calculate")
    public String calculateGet() {
        log.debug("GET /web/calculate — redirecting to /");
        return "redirect:/";
    }

    // ── Вспомогательные методы ─────────────────────────────────────────────

    /**
     * Конвертирует WebCalculateForm в TravelCalculatePremiumRequest (V2 API).
     */
    private TravelCalculatePremiumRequest toRequest(WebCalculateForm form) {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName(form.getPersonFirstName())
                .personLastName(form.getPersonLastName())
                .personBirthDate(form.getPersonBirthDate())
                .agreementDateFrom(form.getAgreementDateFrom())
                .agreementDateTo(form.getAgreementDateTo())
                .countryIsoCode(form.getCountryIsoCode())
                .medicalRiskLimitLevel(form.getMedicalRiskLimitLevel())
                .selectedRisks(form.getSelectedRisks())
                .currency(form.getCurrency() != null && !form.getCurrency().isBlank()
                        ? form.getCurrency() : "EUR")
                .promoCode(form.getPromoCode())
                .personsCount(form.getPersonsCount())
                .isCorporate(form.getIsCorporate())
                .build();
    }

    /**
     * Добавляет справочные данные в модель (для выпадающих списков формы).
     */
    private void addReferenceData(Model model) {
        // Страны для выпадающего списка
        model.addAttribute("countries", getCountries());
        // Уровни покрытия
        model.addAttribute("coverageLevels", getCoverageLevels());
        // Типы рисков
        model.addAttribute("riskTypes", getRiskTypes());
        // Валюты
        model.addAttribute("currencies", List.of("EUR", "USD", "GBP", "CHF", "JPY"));
    }

    private List<CountryOption> getCountries() {
        return List.of(
                new CountryOption("AT", "Austria"),
                new CountryOption("AU", "Australia"),
                new CountryOption("AE", "United Arab Emirates"),
                new CountryOption("AF", "Afghanistan"),
                new CountryOption("AR", "Argentina"),
                new CountryOption("BE", "Belgium"),
                new CountryOption("BR", "Brazil"),
                new CountryOption("CA", "Canada"),
                new CountryOption("CH", "Switzerland"),
                new CountryOption("CN", "China"),
                new CountryOption("CO", "Colombia"),
                new CountryOption("DE", "Germany"),
                new CountryOption("DK", "Denmark"),
                new CountryOption("EG", "Egypt"),
                new CountryOption("ES", "Spain"),
                new CountryOption("FR", "France"),
                new CountryOption("ID", "Indonesia"),
                new CountryOption("IN", "India"),
                new CountryOption("IQ", "Iraq"),
                new CountryOption("IT", "Italy"),
                new CountryOption("JP", "Japan"),
                new CountryOption("KE", "Kenya"),
                new CountryOption("KR", "South Korea"),
                new CountryOption("MA", "Morocco"),
                new CountryOption("MX", "Mexico"),
                new CountryOption("MY", "Malaysia"),
                new CountryOption("NL", "Netherlands"),
                new CountryOption("NO", "Norway"),
                new CountryOption("NZ", "New Zealand"),
                new CountryOption("PE", "Peru"),
                new CountryOption("PH", "Philippines"),
                new CountryOption("SE", "Sweden"),
                new CountryOption("SO", "Somalia"),
                new CountryOption("SY", "Syria"),
                new CountryOption("TH", "Thailand"),
                new CountryOption("TN", "Tunisia"),
                new CountryOption("TR", "Turkey"),
                new CountryOption("US", "United States"),
                new CountryOption("VN", "Vietnam"),
                new CountryOption("YE", "Yemen"),
                new CountryOption("ZA", "South Africa")
        );
    }

    private List<CoverageLevelOption> getCoverageLevels() {
        return List.of(
                new CoverageLevelOption("5000",   "5,000 EUR   — 1.50 EUR/day"),
                new CoverageLevelOption("10000",  "10,000 EUR  — 2.00 EUR/day"),
                new CoverageLevelOption("20000",  "20,000 EUR  — 3.00 EUR/day"),
                new CoverageLevelOption("50000",  "50,000 EUR  — 4.50 EUR/day"),
                new CoverageLevelOption("100000", "100,000 EUR — 7.00 EUR/day"),
                new CoverageLevelOption("200000", "200,000 EUR — 12.00 EUR/day"),
                new CoverageLevelOption("500000", "500,000 EUR — 20.00 EUR/day")
        );
    }

    private List<RiskTypeOption> getRiskTypes() {
        return List.of(
                new RiskTypeOption("SPORT_ACTIVITIES",  "Sport Activities (+30%)", false),
                new RiskTypeOption("EXTREME_SPORT",     "Extreme Sport (+60%)", false),
                new RiskTypeOption("PREGNANCY",         "Pregnancy Coverage (+20%)", false),
                new RiskTypeOption("CHRONIC_DISEASES",  "Chronic Diseases (+40%)", false),
                new RiskTypeOption("ACCIDENT_COVERAGE", "Accident Coverage (+20%)", false),
                new RiskTypeOption("TRIP_CANCELLATION", "Trip Cancellation (+15%)", false),
                new RiskTypeOption("LUGGAGE_LOSS",      "Luggage Loss (+10%)", false),
                new RiskTypeOption("FLIGHT_DELAY",      "Flight Delay (+5%)", false),
                new RiskTypeOption("CIVIL_LIABILITY",   "Civil Liability (+10%)", false)
        );
    }

    // ── Вспомогательные record-классы для справочников ────────────────────

    public record CountryOption(String code, String name) {}
    public record CoverageLevelOption(String code, String label) {}
    public record RiskTypeOption(String code, String label, boolean mandatory) {}
}