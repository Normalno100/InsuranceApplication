package org.javaguru.travel.insurance.core.validation.v2;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Валидация выбранных рисков для запросов V2.
 * Проверяет:
 * 1. Код риска не null и не пустой
 * 2. Риск существует в базе и активен на дату начала договора
 * 3. Риск не является обязательным
 */
@Component
@RequiredArgsConstructor
public class SelectedRisksValidationV2 implements ValidationRuleV2 {

    private final RiskTypeRepository riskTypeRepository;

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {

        List<String> risks = request.getSelectedRisks();
        var agreementDate = request.getAgreementDateFrom();

        // Если рисков нет — ошибок нет
        if (risks == null || risks.isEmpty()) {
            return Optional.empty();
        }

        for (String riskCode : risks) {

            // === 1. Проверка на null ===
            if (riskCode == null) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk code cannot be empty!"
                ));
            }

            // === 2. Проверка на пустую строку ===
            String trimmed = riskCode.trim();
            if (trimmed.isEmpty()) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk code cannot be empty!"
                ));
            }

            // === 3. Проверка на существование и активность ===
            Optional<RiskTypeEntity> riskOpt =
                    riskTypeRepository.findActiveByCode(trimmed, agreementDate);

            if (riskOpt.isEmpty()) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk type '" + trimmed + "' not found or not active!"
                ));
            }

            // === 4. Проверка на обязательный риск ===
            RiskTypeEntity risk = riskOpt.get();
            if (Boolean.TRUE.equals(risk.getIsMandatory())) {
                return Optional.of(new ValidationError(
                        "selectedRisks",
                        "Risk '" + trimmed + "' is mandatory and cannot be selected manually!"
                ));
            }
        }

        // Всё хорошо — ошибок нет
        return Optional.empty();
    }
}
