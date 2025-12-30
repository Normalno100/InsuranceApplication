package org.javaguru.travel.insurance.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.services.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для расчета страховых премий
 */
@Slf4j
@RestController
@RequestMapping("/insurance/travel")
@RequiredArgsConstructor
public class TravelCalculatePremiumController {

    private final TravelCalculatePremiumService calculatePremiumService;

    @PostMapping(
            path = {"/", "/calculate"},
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<TravelCalculatePremiumResponse> calculatePremium(
            @RequestBody TravelCalculatePremiumRequest request) {

        log.info("Premium calculation request: {} {}, country: {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode());

        var response = calculatePremiumService.calculatePremium(request);

        // Проверяем наличие ошибок и возвращаем соответствующий HTTP статус
        if (response.hasErrors()) {
            log.warn("Validation errors: {}", response.getErrors());
            return ResponseEntity.badRequest().body(response); // HTTP 400
        }

        log.info("Premium calculated: {} {}", response.getAgreementPrice(), response.getCurrency());
        return ResponseEntity.ok(response); // HTTP 200
    }

    @GetMapping({"/health"})
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Travel Insurance Service is running");
    }
}