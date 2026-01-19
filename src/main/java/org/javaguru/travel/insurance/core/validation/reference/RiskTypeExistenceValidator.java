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
 * ОБНОВЛЕНО: task_95 - игнорируем null/пустые элементы, ошибки с индексами
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

        for (int i = 0; i < selectedRisks.size(); i++) {
            String riskType = selectedRisks.get(i);

            // ✅ ОБНОВЛЕНИЕ: игнорируем null и пустые элементы
            if (riskType == null || riskType.trim().isEmpty()) {
                continue;
            }

            Optional<RiskTypeEntity> riskOpt =
                    riskRepository.findActiveByCode(riskType, agreementDateFrom);

            if (riskOpt.isEmpty()) {
                // ✅ ОБНОВЛЕНИЕ: ошибка с индексом элемента
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks[" + i + "]",
                                        String.format("Risk type '%s' not found or not active on %s!",
                                                riskType, agreementDateFrom)
                                )
                                .withParameter("riskType", riskType)
                                .withParameter("agreementDateFrom", agreementDateFrom)
                                .withParameter("index", i)
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