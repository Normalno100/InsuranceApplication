package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.domain.model.entity.Risk;
import org.javaguru.travel.insurance.domain.model.valueobject.RiskCode;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Проверяет что все выбранные риски существуют и активны
 */
public class RiskTypeExistenceValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final ReferenceDataPort referenceDataPort;

    public RiskTypeExistenceValidator(ReferenceDataPort referenceDataPort) {
        super("RiskTypeExistenceValidator", 230);
        this.referenceDataPort = referenceDataPort;
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
        List<Risk> foundRisks = new ArrayList<>();

        for (int i = 0; i < selectedRisks.size(); i++) {
            String riskTypeCode = selectedRisks.get(i);

            // Игнорируем null и пустые элементы
            if (riskTypeCode == null || riskTypeCode.trim().isEmpty()) {
                continue;
            }

            // Используем Domain Port вместо Repository
            RiskCode code = new RiskCode(riskTypeCode);
            var riskOpt = referenceDataPort.findRisk(code, agreementDateFrom);

            if (riskOpt.isEmpty()) {
                // Ошибка с индексом элемента
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks[" + i + "]",
                                        String.format("Risk type '%s' not found or not active on %s!",
                                                riskTypeCode, agreementDateFrom)
                                )
                                .withParameter("riskType", riskTypeCode)
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