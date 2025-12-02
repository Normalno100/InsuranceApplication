package org.javaguru.travel.insurance.rest.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для расчета страховых премий (версия 2)
 *
 * Endpoints:
 * POST /insurance/travel/v2/calculate - расчет премии
 * GET  /insurance/travel/v2/health    - health check
 */
@Slf4j
@RestController
@RequestMapping("/insurance/travel/v2")
@RequiredArgsConstructor
public class TravelCalculatePremiumControllerV2 {

    private final TravelCalculatePremiumServiceV2 calculatePremiumService;

    /**
     * Рассчитывает страховую премию
     *
     * @param request запрос с параметрами страхования
     * @return ответ с рассчитанной премией и деталями
     */
    @PostMapping(
            path = {"/", "/calculate"},
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<TravelCalculatePremiumResponseV2> calculatePremium(
            @RequestBody TravelCalculatePremiumRequestV2 request) {

        log.info("Received premium calculation request for: {} {}",
                request.getPersonFirstName(),

                request.getPersonLastName());

        TravelCalculatePremiumResponseV2 response = calculatePremiumService.calculatePremium(request);

        if (response.hasErrors()) {
            log.warn("Validation errors for request: {}", response.getErrors());
            return ResponseEntity.badRequest().body(response);
        }

        log.info("Premium calculated successfully: {} {}",
                response.getAgreementPrice(),
                response.getCurrency());

        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     *
     * @return статус сервиса
     */
    @GetMapping("/health")
    public ResponseEntity<HealthResponse> healthCheck() {
        return ResponseEntity.ok(new HealthResponse(
                "Travel Insurance Service V2 is running",
                "2.0.0",
                System.currentTimeMillis()
        ));
    }

    /**
     * Получение информации о доступных странах
     *
     * @return список стран с коэффициентами
     */
    @GetMapping("/countries")
    public ResponseEntity<CountriesResponse> getCountries() {
        return ResponseEntity.ok(new CountriesResponse(
                org.javaguru.travel.insurance.core.domain.Country.values()
        ));
    }

    /**
     * Получение информации о доступных уровнях покрытия
     *
     * @return список уровней покрытия
     */
    @GetMapping("/coverage-levels")
    public ResponseEntity<CoverageLevelsResponse> getCoverageLevels() {
        return ResponseEntity.ok(new CoverageLevelsResponse(
                org.javaguru.travel.insurance.core.domain.MedicalRiskLimitLevel.values()
        ));
    }

    /**
     * Получение информации о доступных рисках
     *
     * @return список типов рисков
     */
    @GetMapping("/risk-types")
    public ResponseEntity<RiskTypesResponse> getRiskTypes() {
        return ResponseEntity.ok(new RiskTypesResponse(
                org.javaguru.travel.insurance.core.domain.RiskType.values()
        ));
    }

    /**
     * Валидация промо-кода
     *
     * @param code промо-код
     * @return информация о промо-коде
     */
    @GetMapping("/promo-codes/{code}")
    public ResponseEntity<PromoCodeValidationResponse> validatePromoCode(@PathVariable String code) {
        return ResponseEntity.ok(new PromoCodeValidationResponse(
                code,
                "Valid promo code",
                true
        ));
    }

    // ========== DTO для вспомогательных эндпоинтов ==========

    public record HealthResponse(
            String status,
            String version,
            long timestamp
    ) {}

    public record CountriesResponse(
            org.javaguru.travel.insurance.core.domain.Country[] countries
    ) {}

    public record CoverageLevelsResponse(
            org.javaguru.travel.insurance.core.domain.MedicalRiskLimitLevel[] levels
    ) {}

    public record RiskTypesResponse(
            org.javaguru.travel.insurance.core.domain.RiskType[] riskTypes
    ) {}

    public record PromoCodeValidationResponse(
            String code,
            String message,
            boolean isValid
    ) {}
}
