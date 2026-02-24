package org.javaguru.travel.insurance.application.validation.rule.structural;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;

/**
 * Условный валидатор для medicalRiskLimitLevel.
 *
 * БИЗНЕС-ПРАВИЛО:
 * - Если useCountryDefaultPremium = true  → поле НЕ обязательно (игнорируется)
 * - Если useCountryDefaultPremium = false/null → поле ОБЯЗАТЕЛЬНО
 */
public class ConditionalMedicalRiskLimitLevelValidator
        extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    public ConditionalMedicalRiskLimitLevelValidator() {
        super("ConditionalMedicalRiskLimitLevelValidator", 15); // Order 15 — structural level
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        // В режиме COUNTRY_DEFAULT медицинский уровень не нужен
        if (Boolean.TRUE.equals(request.getUseCountryDefaultPremium())) {
            return success();
        }

        // В режиме MEDICAL_LEVEL (useCountryDefaultPremium = false / null) — поле обязательно
        String level = request.getMedicalRiskLimitLevel();

        if (level == null) {
            return ValidationResult.failure(
                    ValidationError.critical(
                            "medicalRiskLimitLevel",
                            "Field medicalRiskLimitLevel must not be null!"
                    ).withParameter("field", "medicalRiskLimitLevel")
            );
        }

        if (level.trim().isEmpty()) {
            return ValidationResult.failure(
                    ValidationError.error(
                            "medicalRiskLimitLevel",
                            "Field medicalRiskLimitLevel must not be empty!"
                    ).withParameter("field", "medicalRiskLimitLevel")
            );
        }

        return success();
    }
}
