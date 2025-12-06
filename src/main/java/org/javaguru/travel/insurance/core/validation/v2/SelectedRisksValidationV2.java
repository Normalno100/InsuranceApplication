package org.javaguru.travel.insurance.core.validation.v2;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Валидация выбранных рисков для запросов V2
 * Проверяет:
 * 1. Что все выбранные риски существуют в базе данных
 * 2. Что все риски активны на дату начала поездки
 * 3. Что не выбраны обязательные риски (они добавляются автоматически)
 */
@Component
@RequiredArgsConstructor
public class SelectedRisksValidationV2 implements ValidationRuleV2 {

    private final RiskTypeRepository riskTypeRepository;

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {
        List<String> selectedRisks = request.getSelectedRisks();

        // Если риски не выбраны - это нормально
        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return Optional.empty();
        }

        // Проверяем каждый выбранный риск
        for (String riskCode : selectedRisks) {
            if (riskCode == null || riskCode.trim().isEmpty()) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk code cannot be empty!"
                ));
            }

            String trimmedCode = riskCode.trim();

            // Проверяем существование и активность риска
            var riskOpt = riskTypeRepository.findActiveByCode(
                    trimmedCode,
                    request.getAgreementDateFrom()
            );

            if (riskOpt.isEmpty()) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk type '" + trimmedCode + "' not found or not active!"
                ));
            }

            // Проверяем, что это не обязательный риск
            if (Boolean.TRUE.equals(riskOpt.get().getIsMandatory())) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk type '" + trimmedCode + "' is mandatory and should not be in selected risks!"
                ));
            }
        }

        return Optional.empty();
    }
}