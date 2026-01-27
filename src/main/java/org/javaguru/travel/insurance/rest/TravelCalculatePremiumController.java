package org.javaguru.travel.insurance.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.services.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST контроллер для расчета страховых премий, формат ответа с метаданными и разделением summary/details
 */
@Slf4j
@RestController
@RequestMapping("/insurance/travel")
@RequiredArgsConstructor
public class TravelCalculatePremiumController {

    private final TravelCalculatePremiumService calculatePremiumService;

    /**
     * Расчет страховой премии
     *
     * @param request тело запроса с параметрами страхования
     * @param includeDetails включать ли детальную разбивку расчета (по умолчанию true)
     * @return ответ с результатом расчета или ошибками
     */
    @PostMapping(
            path = {"/", "/calculate"},
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<TravelCalculatePremiumResponse> calculatePremium(
            @RequestBody TravelCalculatePremiumRequest request,
            @RequestParam(name = "includeDetails", defaultValue = "true") boolean includeDetails) {

        log.info("Premium calculation request: {} {}, country: {}, includeDetails: {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode(),
                includeDetails);

        var response = calculatePremiumService.calculatePremium(request, includeDetails);

        // Логируем результат
        logResponse(response);

        // Определяем HTTP статус на основе статуса ответа
        return ResponseEntity
                .status(determineHttpStatus(response))
                .body(response);
    }

    /**
     * Определяет HTTP статус на основе статуса ответа
     */
    private int determineHttpStatus(TravelCalculatePremiumResponse response) {
        return switch (response.getStatus()) {
            case SUCCESS -> 200;  // OK
            case VALIDATION_ERROR -> 400;  // Bad Request
            case DECLINED -> 422;  // Unprocessable Entity
            case REQUIRES_REVIEW -> 202;  // Accepted (требует дополнительной обработки)
        };
    }

    /**
     * Логирует ответ
     */
    private void logResponse(TravelCalculatePremiumResponse response) {
        switch (response.getStatus()) {
            case SUCCESS -> {
                var premium = response.getPricing() != null
                        ? response.getPricing().getTotalPremium()
                        : null;
                var currency = response.getPricing() != null
                        ? response.getPricing().getCurrency()
                        : "EUR";
                log.info("Premium calculated successfully: {} {} (requestId: {})",
                        premium, currency, response.getRequestId());
            }
            case VALIDATION_ERROR ->
                    log.warn("Validation errors (requestId: {}): {}",
                            response.getRequestId(),
                            response.getErrors().size());
            case DECLINED ->
                    log.warn("Application declined (requestId: {}): {}",
                            response.getRequestId(),
                            response.getUnderwriting() != null
                                    ? response.getUnderwriting().getReason()
                                    : "Unknown reason");
            case REQUIRES_REVIEW ->
                    log.info("Application requires manual review (requestId: {}): {}",
                            response.getRequestId(),
                            response.getUnderwriting() != null
                                    ? response.getUnderwriting().getReason()
                                    : "Unknown reason");
        }
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<HealthCheckResponse> healthCheck() {
        return ResponseEntity.ok(new HealthCheckResponse(
                "Travel Insurance Service is running",
                "2.0",
                "OK"
        ));
    }

    /**
     * Информация об API
     */
    @GetMapping("/info")
    public ResponseEntity<ApiInfo> apiInfo() {
        return ResponseEntity.ok(new ApiInfo(
                "Travel Insurance API",
                "2.0",
                "Premium calculation with underwriting",
                List.of(
                        new Endpoint("POST", "/insurance/travel/calculate", "Calculate insurance premium")
                )
        ));
    }

    // ========================================
    // Вспомогательные классы для ответов
    // ========================================

    private record HealthCheckResponse(
            String message,
            String version,
            String status
    ) {}

    private record ApiInfo(
            String name,
            String version,
            String description,
            List<Endpoint> endpoints
    ) {}

    private record Endpoint(
            String method,
            String path,
            String description
    ) {}
}