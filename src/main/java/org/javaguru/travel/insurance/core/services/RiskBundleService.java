package org.javaguru.travel.insurance.core.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.domain.entities.RiskBundleEntity;
import org.javaguru.travel.insurance.core.repositories.RiskBundleRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Сервис для работы с пакетами рисков
 * Пакеты рисков — покупка нескольких связанных рисков со скидкой
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RiskBundleService {

    private final RiskBundleRepository bundleRepository;
    private final ObjectMapper objectMapper;

    /**
     * Находит все применимые пакеты для выбранных рисков
     *
     * @param selectedRisks выбранные коды рисков
     * @param date дата применения
     * @return список применимых пакетов, отсортированных по скидке (от большей к меньшей)
     */
    public List<ApplicableBundleResult> findApplicableBundles(
            List<String> selectedRisks,
            LocalDate date) {

        if (selectedRisks == null || selectedRisks.isEmpty()) {
            return Collections.emptyList();
        }

        log.debug("Finding applicable bundles for risks: {}", selectedRisks);

        // Получаем все активные пакеты
        List<RiskBundleEntity> allBundles = bundleRepository.findAllActive(date);

        // Фильтруем пакеты, требования которых полностью покрыты
        List<ApplicableBundleResult> applicableBundles = new ArrayList<>();

        for (RiskBundleEntity bundle : allBundles) {
            try {
                List<String> requiredRisks = parseRequiredRisks(bundle.getRequiredRisks());

                // Проверяем, содержит ли selectedRisks все требуемые риски
                if (selectedRisks.containsAll(requiredRisks)) {
                    applicableBundles.add(new ApplicableBundleResult(
                            bundle.getCode(),
                            bundle.getNameEn(),
                            bundle.getDiscountPercentage(),
                            requiredRisks
                    ));

                    log.debug("Bundle '{}' is applicable (discount: {}%)",
                            bundle.getCode(),
                            bundle.getDiscountPercentage());
                }
            } catch (Exception e) {
                log.error("Error parsing required risks for bundle {}: {}",
                        bundle.getCode(), e.getMessage());
            }
        }

        // Сортируем по размеру скидки (от большей к меньшей)
        applicableBundles.sort(
                Comparator.comparing(ApplicableBundleResult::discountPercentage).reversed()
        );

        log.info("Found {} applicable bundles", applicableBundles.size());
        return applicableBundles;
    }

    /**
     * Получает лучший (с максимальной скидкой) применимый пакет
     */
    public Optional<ApplicableBundleResult> getBestApplicableBundle(
            List<String> selectedRisks,
            LocalDate date) {

        var bundles = findApplicableBundles(selectedRisks, date);
        return bundles.isEmpty() ? Optional.empty() : Optional.of(bundles.get(0));
    }

    /**
     * Рассчитывает сумму скидки от пакета
     */
    public BigDecimal calculateBundleDiscount(
            BigDecimal premiumAmount,
            ApplicableBundleResult bundle) {

        return premiumAmount
                .multiply(bundle.discountPercentage())
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
    }

    /**
     * Парсит JSON с требуемыми рисками
     */
    private List<String> parseRequiredRisks(String requiredRisksJson) {
        try {
            return objectMapper.readValue(
                    requiredRisksJson,
                    new TypeReference<List<String>>() {}
            );
        } catch (Exception e) {
            log.error("Failed to parse required risks JSON: {}", requiredRisksJson, e);
            return Collections.emptyList();
        }
    }

    /**
     * Результат применимого пакета
     */
    public record ApplicableBundleResult(
            String code,
            String name,
            BigDecimal discountPercentage,
            List<String> requiredRisks
    ) {}
}