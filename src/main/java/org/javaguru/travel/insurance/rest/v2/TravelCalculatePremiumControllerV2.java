package org.javaguru.travel.insurance.rest.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumServiceV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST контроллер для расчета страховых премий
 */
@Slf4j
@RestController
@RequestMapping("/insurance/travel")
@RequiredArgsConstructor
public class TravelCalculatePremiumControllerV2 {

    private final TravelCalculatePremiumServiceV2 calculatePremiumService;

    @PostMapping(
            path = {"/", "/calculate", "/v2/", "/v2/calculate"},
            consumes = "application/json",
            produces = "application/json"
    )
    public ResponseEntity<TravelCalculatePremiumResponseV2> calculatePremium(
            @RequestBody TravelCalculatePremiumRequestV2 request) {

        log.info("Premium calculation request: {} {}, country: {}",
                request.getPersonFirstName(),
                request.getPersonLastName(),
                request.getCountryIsoCode());

        var response = calculatePremiumService.calculatePremium(request);

        if (response.hasErrors()) {
            log.warn("Validation errors: {}", response.getErrors());
            return ResponseEntity.badRequest().body(response);
        }

        log.info("Premium calculated: {} {}", response.getAgreementPrice(), response.getCurrency());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/health", "/v2/health"})
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Travel Insurance Service is running");
    }
}