package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.application.orchestrator.PremiumCalculationOrchestrator;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
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