package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.domain.model.entity.MedicalRiskLimitLevel;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Проверяет что уровень медицинского покрытия существует и активен
 */
public class MedicalRiskLimitLevelExistenceValidator
        extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final ReferenceDataPort referenceDataPort;

    public MedicalRiskLimitLevelExistenceValidator(ReferenceDataPort referenceDataPort) {
        super("MedicalRiskLimitLevelExistenceValidator", 220);
        this.referenceDataPort = referenceDataPort;
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        String medicalRiskLimitLevel = request.getMedicalRiskLimitLevel();
        LocalDate agreementDateFrom = request.getAgreementDateFrom();

        // Если поля null, пропускаем
        if (medicalRiskLimitLevel == null || agreementDateFrom == null) {
            return success();
        }

        // Используем Domain Port вместо Repository
        Optional<MedicalRiskLimitLevel> levelOpt =
                referenceDataPort.findMedicalLevel(medicalRiskLimitLevel, agreementDateFrom);

        if (levelOpt.isEmpty()) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    "medicalRiskLimitLevel",
                                    String.format(
                                            "Medical risk limit level '%s' not found or not active on %s!",
                                            medicalRiskLimitLevel, agreementDateFrom
                                    )
                            )
                            .withParameter("medicalRiskLimitLevel", medicalRiskLimitLevel)
                            .withParameter("agreementDateFrom", agreementDateFrom)
            );
        }

        // Сохраняем в контекст
        context.setAttribute("medicalRiskLimitLevel", levelOpt.get());

        return success();
    }
}