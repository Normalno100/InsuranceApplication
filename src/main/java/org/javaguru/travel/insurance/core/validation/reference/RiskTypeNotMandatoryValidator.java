package org.javaguru.travel.insurance.core.validation.reference;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.*;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Проверяет что выбранные риски НЕ являются обязательными
 * (обязательные риски добавляются автоматически)
 * ОБНОВЛЕНО: task_95 - теперь ИГНОРИРУЕМ обязательные риски вместо ошибки
 */
public class RiskTypeNotMandatoryValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private final RiskTypeRepository riskRepository;

    public RiskTypeNotMandatoryValidator(RiskTypeRepository riskRepository) {
        super("RiskTypeNotMandatoryValidator", 240);
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

        // ✅ ОБНОВЛЕНИЕ: ИГНОРИРУЕМ обязательные риски (не возвращаем ошибку)
        // Старая логика закомментирована для истории:

        /*
        ValidationResult.Builder resultBuilder = ValidationResult.builder();

        for (String riskType : selectedRisks) {
            if (riskType == null || riskType.trim().isEmpty()) {
                continue;
            }

            Optional<RiskTypeEntity> riskOpt =
                    riskRepository.findActiveByCode(riskType, agreementDateFrom);

            if (riskOpt.isPresent() && riskOpt.get().getIsMandatory()) {
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