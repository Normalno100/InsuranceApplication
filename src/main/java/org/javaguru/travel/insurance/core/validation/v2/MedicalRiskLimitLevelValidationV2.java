package org.javaguru.travel.insurance.core.validation.v2;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.validation.ValidationRuleV2;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Валидация уровня медицинского покрытия для запросов V2
 * Проверяет:
 * 1. Что уровень покрытия не пустой
 * 2. Что уровень существует в базе данных
 * 3. Что уровень активен на дату начала поездки
 */
@Component
@RequiredArgsConstructor
public class MedicalRiskLimitLevelValidationV2 implements ValidationRuleV2 {

    private final MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository;

    @Override
    public Optional<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {
        // Проверка на null или пустую строку
        if (request.getMedicalRiskLimitLevel() == null
                || request.getMedicalRiskLimitLevel().trim().isEmpty()) {
            return Optional.of(new ValidationError(
                    "medicalRiskLimitLevel",
                    "Must not be empty!"
            ));
        }

        String levelCode = request.getMedicalRiskLimitLevel().trim();

        // Проверка существования уровня в БД
        var levelOpt = medicalRiskLimitLevelRepository.findActiveByCode(
                levelCode,
                request.getAgreementDateFrom()
        );

        if (levelOpt.isEmpty()) {
            return Optional.of(new ValidationError(
                    "medicalRiskLimitLevel",
                    "Medical risk limit level '" + levelCode + "' not found or not active!"
            ));
        }

        return Optional.empty();
    }
}