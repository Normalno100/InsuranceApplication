package org.javaguru.travel.insurance.application.validation.rule.business;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Проверяет отсутствие дубликатов в списке выбранных рисков
 */
public class DuplicateRisksValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    public DuplicateRisksValidator() {
        super("DuplicateRisksValidator", 165); // Order = 165 (после MandatoryRisksValidator)
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        List<String> selectedRisks = request.getSelectedRisks();

        // Если список пустой или null - пропускаем
        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return success();
        }

        // Фильтруем null и пустые элементы (они игнорируются по требованию)
        List<String> validRisks = selectedRisks.stream()
                .filter(Objects::nonNull)
                .filter(risk -> !risk.trim().isEmpty())
                .collect(Collectors.toList());

        // Если после фильтрации список пустой - нечего проверять
        if (validRisks.isEmpty()) {
            return success();
        }

        // Ищем дубликаты
        ValidationResult.Builder resultBuilder = ValidationResult.builder();
        Set<String> seen = new HashSet<>();
        Map<String, List<Integer>> duplicates = new HashMap<>();

        for (int i = 0; i < validRisks.size(); i++) {
            String risk = validRisks.get(i);

            if (!seen.add(risk)) {
                // Это дубликат
                duplicates.computeIfAbsent(risk, k -> new ArrayList<>()).add(i);
            }
        }

        // Создаём ошибки для каждого дубликата
        for (Map.Entry<String, List<Integer>> entry : duplicates.entrySet()) {
            String riskCode = entry.getKey();
            List<Integer> indices = entry.getValue();

            // Добавляем ошибку для каждого повторного вхождения
            for (Integer index : indices) {
                resultBuilder.addError(
                        ValidationError.error(
                                        "selectedRisks[" + index + "]",
                                        String.format("Risk '%s' appears multiple times in the list!", riskCode)
                                )
                                .withParameter("riskCode", riskCode)
                                .withParameter("index", index)
                );
            }
        }

        return resultBuilder.build();
    }
}