package org.javaguru.travel.insurance.core.underwriting;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.javaguru.travel.insurance.core.underwriting.domain.RuleResult;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.core.underwriting.rule.UnderwritingRule;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Движок андеррайтинга - выполняет все правила и принимает решение
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UnderwritingEngine {

    private final List<UnderwritingRule> rules;

    /**
     * Выполняет все правила андеррайтинга и возвращает итоговое решение
     *
     * @param request заявка на страхование
     * @return результат андеррайтинга
     */
    public UnderwritingResult evaluate(TravelCalculatePremiumRequest request) {
        log.info("Starting underwriting evaluation for {} {}",
                request.getPersonFirstName(), request.getPersonLastName());

        List<RuleResult> ruleResults = new ArrayList<>();

        // Сортируем правила по приоритету (Order)
        List<UnderwritingRule> sortedRules = rules.stream()
                .sorted(Comparator.comparingInt(UnderwritingRule::getOrder))
                .collect(Collectors.toList());

        // Выполняем все правила
        for (UnderwritingRule rule : sortedRules) {
            log.debug("Evaluating rule: {}", rule.getRuleName());

            try {
                RuleResult result = rule.evaluate(request);
                ruleResults.add(result);

                log.debug("Rule {} result: {}", rule.getRuleName(), result.getSeverity());

            } catch (Exception e) {
                log.error("Error evaluating rule {}: {}", rule.getRuleName(), e.getMessage(), e);

                // Если правило упало с ошибкой, считаем это блокирующей проблемой
                ruleResults.add(RuleResult.blocking(
                        rule.getRuleName(),
                        "Error evaluating rule: " + e.getMessage()
                ));
            }
        }

        // Принимаем итоговое решение
        return makeDecision(ruleResults);
    }

    /**
     * Принимает итоговое решение на основе результатов всех правил
     */
    private UnderwritingResult makeDecision(List<RuleResult> ruleResults) {

        // Проверяем наличие блокирующих нарушений
        List<RuleResult> blockingResults = ruleResults.stream()
                .filter(RuleResult::isBlocking)
                .collect(Collectors.toList());

        if (!blockingResults.isEmpty()) {
            String reason = blockingResults.stream()
                    .map(RuleResult::getMessage)
                    .collect(Collectors.joining("; "));

            log.info("Application DECLINED: {}", reason);
            return UnderwritingResult.declined(ruleResults, reason);
        }

        // Проверяем наличие правил, требующих проверки
        List<RuleResult> reviewResults = ruleResults.stream()
                .filter(RuleResult::requiresReview)
                .collect(Collectors.toList());

        if (!reviewResults.isEmpty()) {
            String reason = reviewResults.stream()
                    .map(RuleResult::getMessage)
                    .collect(Collectors.joining("; "));

            log.info("Application REQUIRES MANUAL REVIEW: {}", reason);
            return UnderwritingResult.requiresReview(ruleResults, reason);
        }

        // Все правила пройдены
        log.info("Application APPROVED");
        return UnderwritingResult.approved(ruleResults);
    }
}