package org.javaguru.travel.insurance.core.underwriting.rule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.calculators.AgeCalculator;
import org.javaguru.travel.insurance.infrastructure.persistence.repositories.MedicalRiskLimitLevelRepository;
import org.javaguru.travel.insurance.core.underwriting.config.UnderwritingConfigService;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Правило андеррайтинга: проверка суммы медицинского покрытия по возрасту.
 *
 * ИСПРАВЛЕНИЕ (п. 2.2 плана рефакторинга):
 *
 * ПРОБЛЕМА (было):
 *   evaluate() сразу вызывал
 *   medicalRepository.findActiveByCode(request.getMedicalRiskLimitLevel(), ...)
 *   без проверки режима расчёта. При useCountryDefaultPremium=true поле
 *   medicalRiskLimitLevel равно null → NullPointerException /
 *   IllegalArgumentException внутри репозитория.
 *
 * РЕШЕНИЕ (стало):
 *   В начале evaluate() добавлены два guard-а:
 *
 *   1. useCountryDefaultPremium == true → немедленный PASS.
 *      В этом режиме medicalRiskLimitLevel не задаётся и не нужен;
 *      уровень покрытия отсутствует, проверять нечего.
 *
 *   2. medicalRiskLimitLevel == null → немедленный PASS.
 *      Защита от NullPointerException в случаях, когда валидация
 *      пропустила запрос при stopOnCriticalError=false.
 *
 * ПОВЕДЕНИЕ ПО РЕЖИМАМ:
 *   COUNTRY_DEFAULT (useCountryDefaultPremium=true):
 *     → PASS (без обращения к репозиторию)
 *
 *   MEDICAL_LEVEL (useCountryDefaultPremium=false/null):
 *     age > blockingAge  && coverage > blockingThreshold  → BLOCKING
 *     age >= reviewAge   && coverage > reviewThreshold    → REVIEW_REQUIRED
 *     иначе                                              → PASS
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MedicalCoverageRule implements UnderwritingRule {

    private final AgeCalculator ageCalculator;
    private final MedicalRiskLimitLevelRepository medicalRepository;
    private final UnderwritingConfigService configService;

    @Override
    public RuleResult evaluate(TravelCalculatePremiumRequest request) {

        // ──────────────────────────────────────────────────────────────────
        // GUARD 1: режим COUNTRY_DEFAULT
        // Уровень медицинского покрытия в этом режиме не задаётся,
        // поэтому правило неприменимо — пропускаем.
        // ──────────────────────────────────────────────────────────────────
        if (Boolean.TRUE.equals(request.getUseCountryDefaultPremium())) {
            log.debug("MedicalCoverageRule: skipped — COUNTRY_DEFAULT mode");
            return RuleResult.pass(getRuleName());
        }

        // ──────────────────────────────────────────────────────────────────
        // GUARD 2: medicalRiskLimitLevel == null
        // Защита от NPE на случай, если структурная валидация
        // не остановила запрос (stopOnCriticalError=false).
        // ──────────────────────────────────────────────────────────────────
        if (request.getMedicalRiskLimitLevel() == null) {
            log.debug("MedicalCoverageRule: skipped — medicalRiskLimitLevel is null");
            return RuleResult.pass(getRuleName());
        }

        // ──────────────────────────────────────────────────────────────────
        // Штатная логика режима MEDICAL_LEVEL (код не изменился)
        // ──────────────────────────────────────────────────────────────────

        // Загружаем параметры из БД
        int reviewAge = configService.getIntParameter("MedicalCoverageRule", "REVIEW_AGE", 70);
        int blockingAge = configService.getIntParameter("MedicalCoverageRule", "BLOCKING_AGE", 75);
        BigDecimal reviewThreshold = configService.getBigDecimalParameter(
                "MedicalCoverageRule", "REVIEW_COVERAGE_THRESHOLD", new BigDecimal("100000"));
        BigDecimal blockingThreshold = configService.getBigDecimalParameter(
                "MedicalCoverageRule", "BLOCKING_COVERAGE_THRESHOLD", new BigDecimal("200000"));

        int age = ageCalculator.calculateAge(
                request.getPersonBirthDate(),
                request.getAgreementDateFrom()
        );

        var medicalLevel = medicalRepository.findActiveByCode(
                request.getMedicalRiskLimitLevel(),
                request.getAgreementDateFrom()
        ).orElseThrow(() -> new IllegalArgumentException("Medical level not found"));

        BigDecimal coverage = medicalLevel.getCoverageAmount();

        log.debug("Evaluating medical coverage rule: age={}, coverage={}, reviewAge={}, blockingAge={}",
                age, coverage, reviewAge, blockingAge);

        // Блокирующее условие
        if (age > blockingAge && coverage.compareTo(blockingThreshold) > 0) {
            return RuleResult.blocking(
                    getRuleName(),
                    String.format("Coverage of %s EUR is too high for age %d (max %s EUR)",
                            coverage, age, blockingThreshold)
            );
        }

        // Требует проверки
        if (age >= reviewAge && coverage.compareTo(reviewThreshold) > 0) {
            return RuleResult.reviewRequired(
                    getRuleName(),
                    String.format("High coverage (%s EUR) for age %d requires manual review",
                            coverage, age)
            );
        }

        return RuleResult.pass(getRuleName());
    }

    @Override
    public String getRuleName() {
        return "MedicalCoverageRule";
    }

    @Override
    public int getOrder() {
        return 30;
    }
}