package org.javaguru.travel.insurance.core.validation.business;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.util.List;
import java.util.Set;

/**
 * Проверяет что selectedRisks не содержат mandatory рисков
 * (TRAVEL_MEDICAL - обязательный и добавляется автоматически)
 */
public class MandatoryRisksValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final Set<String> MANDATORY_RISKS = Set.of("TRAVEL_MEDICAL");

    public MandatoryRisksValidator() {
        super("MandatoryRisksValidator", 160); // Order = 160
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        List<String> selectedRisks = request.getSelectedRisks();

        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return success();
        }

        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (String risk : selectedRisks) {
            if (MANDATORY_RISKS.contains(risk)) {
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks",
                                        String.format("Risk '%s' is mandatory and cannot be in selectedRisks!", risk)
                                )
                                .withParameter("risk", risk)
                );
            }
        }

        return resultBuilder.build();
    }
}