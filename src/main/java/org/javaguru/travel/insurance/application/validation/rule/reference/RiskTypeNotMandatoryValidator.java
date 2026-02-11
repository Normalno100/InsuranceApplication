package org.javaguru.travel.insurance.application.validation.rule.reference;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.domain.port.ReferenceDataPort;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * Проверяет что выбранные риски НЕ являются обязательными
 * (обязательные риски добавляются автоматически)
 */
public class RiskTypeNotMandatoryValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final ReferenceDataPort referenceDataPort;

    public RiskTypeNotMandatoryValidator(ReferenceDataPort referenceDataPort) {
        super("RiskTypeNotMandatoryValidator", 240);
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

        // ✅ ОБНОВЛЕНИЕ (task_95): ИГНОРИРУЕМ обязательные риски (не возвращаем ошибку)
        // Старая логика закомментирована для истории:

        /*
        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (String riskType : selectedRisks) {
            if (riskType == null || riskType.trim().isEmpty()) {
                continue;
            }

            RiskCode code = new RiskCode(riskType);
            var riskOpt = referenceDataPort.findRisk(code, agreementDateFrom);

            if (riskOpt.isPresent() && riskOpt.get().isMandatory()) {
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks",
                                        String.format(
                                                "Risk type '%s' is mandatory and cannot be included in selectedRisks!",
                                                riskType
                                        )
                                )
                                .withParameter("riskType", riskType)
                                .withParameter("mandatory", true)
                );
            }
        }

        return resultBuilder.build();
        */

        // Теперь всегда успех - обязательные риски просто игнорируются
        return success();
    }
}