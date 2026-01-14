package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Проверяет что все выбранные риски существуют и активны
 */
public class RiskTypeExistenceValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final RiskTypeRepository riskRepository;

    public RiskTypeExistenceValidator(RiskTypeRepository riskRepository) {
        super("RiskTypeExistenceValidator", 230);
        this.riskRepository = riskRepository;
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        List<String> selectedRisks = request.getSelectedRisks();
        LocalDate agreementDateFrom = request.getAgreementDateFrom();

        // Если нет выбранных рисков или нет даты, пропускаем
        if (selectedRisks == null || selectedRisks.isEmpty() || agreementDateFrom == null) {
            return success();
        }

        ValidationResult.Builder resultBuilder = ValidationResult.builder();
        List<RiskTypeEntity> foundRisks = new ArrayList<>();

        for (String riskType : selectedRisks) {
            if (riskType == null || riskType.trim().isEmpty()) {
                continue; // Пустые элементы проверяются другим валидатором
            }

            Optional<RiskTypeEntity> riskOpt =
                    riskRepository.findActiveByRiskType(riskType, agreementDateFrom);

            if (riskOpt.isEmpty()) {
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks",
                                        String.format("Risk type '%s' not found or not active on %s!",
                                                riskType, agreementDateFrom)
                                )
                                .withParameter("riskType", riskType)
                                .withParameter("agreementDateFrom", agreementDateFrom)
                );
            } else {
                foundRisks.add(riskOpt.get());
            }
        }

        // Сохраняем найденные риски в контекст
        if (!foundRisks.isEmpty()) {
            context.setAttribute("selectedRiskEntities", foundRisks);
        }

        return resultBuilder.build();
    }
}