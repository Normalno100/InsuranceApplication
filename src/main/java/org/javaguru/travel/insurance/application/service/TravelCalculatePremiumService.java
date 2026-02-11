package org.javaguru.travel.insurance.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.orchestrator.PremiumCalculationOrchestrator;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumResponse;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumService {

    private final PremiumCalculationOrchestrator orchestrator;

    public TravelCalculatePremiumResponse calculatePremium(
            TravelCalculatePremiumRequest request) {
        return calculatePremium(request, true);
    }

    public TravelCalculatePremiumResponse calculatePremium(
            TravelCalculatePremiumRequest request,
            boolean includeDetails) {
        return orchestrator.process(request, includeDetails);
    }
}