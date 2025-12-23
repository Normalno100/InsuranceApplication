package org.javaguru.travel.insurance.core;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.repositories.CountryRepository;
import org.javaguru.travel.insurance.core.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.repositories.RiskTypeRepository;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Валидатор запросов V2
 * Проверяет базовые поля и существование справочников в БД
 */
@Component
@RequiredArgsConstructor
public class TravelCalculatePremiumRequestValidator {

    private final CountryRepository countryRepository;
    private final MedicalRiskLimitLevelRepository medicalRiskLimitLevelRepository;
    private final RiskTypeRepository riskTypeRepository;

    public List<ValidationError> validate(TravelCalculatePremiumRequest request) {
        List<ValidationError> errors = new ArrayList<>();

        validatePersonalInfo(request, errors);
        validateDates(request, errors);
        validateCountry(request, errors);
        validateMedicalLevel(request, errors);
        validateRisks(request, errors);

        return errors;
    }

    private void validatePersonalInfo(TravelCalculatePremiumRequest request, List<ValidationError> errors) {
        if (isBlank(request.getPersonFirstName())) {
            errors.add(new ValidationError("personFirstName", "Must not be empty!"));
        }
        if (isBlank(request.getPersonLastName())) {
            errors.add(new ValidationError("personLastName", "Must not be empty!"));
        }
        if (request.getPersonBirthDate() == null) {
            errors.add(new ValidationError("personBirthDate", "Must not be empty!"));
        }
    }

    private void validateDates(TravelCalculatePremiumRequest request, List<ValidationError> errors) {
        LocalDate from = request.getAgreementDateFrom();
        LocalDate to = request.getAgreementDateTo();

        if (from == null) {
            errors.add(new ValidationError("agreementDateFrom", "Must not be empty!"));
        }
        if (to == null) {
            errors.add(new ValidationError("agreementDateTo", "Must not be empty!"));
        }
        if (from != null && to != null && to.isBefore(from)) {
            errors.add(new ValidationError("agreementDateTo", "Must be after agreementDateFrom!"));
        }
    }

    private void validateCountry(TravelCalculatePremiumRequest request, List<ValidationError> errors) {
        String isoCode = request.getCountryIsoCode();

        if (isBlank(isoCode)) {
            errors.add(new ValidationError("countryIsoCode", "Must not be empty!"));
            return;
        }

        if (countryRepository.findActiveByIsoCode(isoCode.trim(), request.getAgreementDateFrom()).isEmpty()) {
            errors.add(new ValidationError("countryIsoCode",
                    "Country '" + isoCode + "' not found or not active!"));
        }
    }

    private void validateMedicalLevel(TravelCalculatePremiumRequest request, List<ValidationError> errors) {
        String level = request.getMedicalRiskLimitLevel();

        if (isBlank(level)) {
            errors.add(new ValidationError("medicalRiskLimitLevel", "Must not be empty!"));
            return;
        }

        if (medicalRiskLimitLevelRepository.findActiveByCode(level.trim(), request.getAgreementDateFrom()).isEmpty()) {
            errors.add(new ValidationError("medicalRiskLimitLevel",
                    "Level '" + level + "' not found or not active!"));
        }
    }

    private void validateRisks(TravelCalculatePremiumRequest request, List<ValidationError> errors) {
        List<String> risks = request.getSelectedRisks();
        if (risks == null || risks.isEmpty()) return;

        LocalDate agreementDate = request.getAgreementDateFrom();

        for (String riskCode : risks) {
            if (isBlank(riskCode)) {
                errors.add(new ValidationError("selectedRisks", "Risk code cannot be empty!"));
                continue;
            }

            var risk = riskTypeRepository.findActiveByCode(riskCode.trim(), agreementDate);

            if (risk.isEmpty()) {
                errors.add(new ValidationError("selectedRisks",
                        "Risk '" + riskCode + "' not found or not active!"));
                continue;
            }

            if (Boolean.TRUE.equals(risk.get().getIsMandatory())) {
                errors.add(new ValidationError("selectedRisks",
                        "Risk '" + riskCode + "' is mandatory and cannot be selected manually!"));
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}