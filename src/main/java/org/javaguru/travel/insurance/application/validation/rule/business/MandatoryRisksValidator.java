package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

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