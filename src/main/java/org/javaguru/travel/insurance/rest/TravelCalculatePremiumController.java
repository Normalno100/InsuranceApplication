package org.javaguru.travel.insurance.rest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumService;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

		log.info("Received premium calculation request for: {} {}",
				request.getPersonFirstName(), request.getPersonLastName());

		TravelCalculatePremiumResponse response = calculatePremiumService.calculatePremium(request);

		if (response.hasErrors()) {
			log.warn("Validation errors occurred: {}", response.getErrors());
			return ResponseEntity.badRequest().body(response);
		}

		log.info("Premium calculated successfully: {}", response.getAgreementPrice());
		return ResponseEntity.ok(response);
	}

	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		return ResponseEntity.ok("Travel Insurance Service is running");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception e) {
		log.error("Unexpected error occurred", e);
		ErrorResponse errorResponse = new ErrorResponse(
				"Internal server error",
				e.getMessage()
		);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
	}

	public record ErrorResponse(String error, String message) {}
}