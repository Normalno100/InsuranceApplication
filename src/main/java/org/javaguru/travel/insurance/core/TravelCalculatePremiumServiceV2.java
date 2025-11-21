package org.javaguru.travel.insurance.core;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.domain.Country;
import org.javaguru.travel.insurance.core.services.DiscountService;
import org.javaguru.travel.insurance.core.services.PromoCodeService;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumRequestV2;
import org.javaguru.travel.insurance.dto.v2.TravelCalculatePremiumResponseV2;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Главный сервис расчета страховой премии (версия 2)
 *
 * Координирует работу всех компонентов:
 * - Валидация запроса
 * - Расчет базовой премии
 * - Применение промо-кодов
 * - Применение скидок
 * - Формирование детального ответа
 */
@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumServiceV2 {

    private final TravelCalculatePremiumRequestValidatorV2 requestValidator;
    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final PromoCodeService promoCodeService;
    private final DiscountService discountService;
    private final CurrencyExchangeService currencyExchangeService;

    /**
     * Главный метод расчета премии
     *
     * @param request запрос на расчет
     * @return ответ с рассчитанной премией и деталями
     */
    public TravelCalculatePremiumResponseV2 calculatePremium(TravelCalculatePremiumRequestV2 request) {
        // 1. Валидация запроса
        List<ValidationError> validationErrors = requestValidator.validate(request);
        if (!validationErrors.isEmpty()) {
            return new TravelCalculatePremiumResponseV2(validationErrors);
        }

        try {
            // 2. Расчет базовой премии с деталями
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult =
                    medicalRiskCalculator.calculatePremiumWithDetails(request);

            // 3. Применение промо-кода (если есть)
            BigDecimal premiumBeforeDiscount = calculationResult.premium();
            BigDecimal discountAmount = BigDecimal.ZERO;
            TravelCalculatePremiumResponseV2.PromoCodeInfo promoCodeInfo = null;
            List<TravelCalculatePremiumResponseV2.DiscountInfo> appliedDiscounts = new ArrayList<>();

            if (request.hasPromoCode()) {
                PromoCodeService.PromoCodeResult promoResult = promoCodeService.applyPromoCode(
                        request.getPromoCode(),
                        request.getAgreementDateFrom(),
                        premiumBeforeDiscount
                );

                if (promoResult.isValid()) {
                    discountAmount = discountAmount.add(promoResult.actualDiscountAmount());
                    promoCodeInfo = new TravelCalculatePremiumResponseV2.PromoCodeInfo(
                            promoResult.code(),
                            promoResult.description(),
                            promoResult.discountType().name(),
                            promoResult.discountValue(),
                            promoResult.actualDiscountAmount()
                    );
                }
            }

            // 4. Применение дополнительных скидок (групповые, корпоративные)
            Optional<DiscountService.DiscountResult> bestDiscount = discountService.calculateBestDiscount(
                    premiumBeforeDiscount,
                    request.getPersonsCountOrDefault(),
                    request.isCorporateClient(),
                    request.getAgreementDateFrom()
            );

            if (bestDiscount.isPresent()) {
                DiscountService.DiscountResult discount = bestDiscount.get();
                discountAmount = discountAmount.add(discount.amount());
                appliedDiscounts.add(new TravelCalculatePremiumResponseV2.DiscountInfo(
                        discount.discountType().name(),
                        discount.name(),
                        discount.percentage(),
                        discount.amount()
                ));
            }

            // 5. Итоговая премия после скидок
            BigDecimal finalPremium = premiumBeforeDiscount.subtract(discountAmount);
            if (finalPremium.compareTo(BigDecimal.ZERO) < 0) {
                finalPremium = BigDecimal.ZERO;
            }

            // Применение минимальной премии (10 EUR)
            BigDecimal minPremium = new BigDecimal("10.00");
            if (finalPremium.compareTo(minPremium) < 0 && finalPremium.compareTo(BigDecimal.ZERO) > 0) {
                finalPremium = minPremium;
            }

            // 6. Конвертация валюты (если нужно)
            String requestedCurrency = request.getCurrencyOrDefault();
            if (!requestedCurrency.equals("EUR")) {
                premiumBeforeDiscount = currencyExchangeService.convert(
                        premiumBeforeDiscount, "EUR", requestedCurrency
                );
                discountAmount = currencyExchangeService.convert(
                        discountAmount, "EUR", requestedCurrency
                );
                finalPremium = currencyExchangeService.convert(
                        finalPremium, "EUR", requestedCurrency
                );
            }

            // 7. Формирование детального ответа
            return buildResponse(
                    request,
                    calculationResult,
                    premiumBeforeDiscount,
                    discountAmount,
                    finalPremium,
                    requestedCurrency,
                    promoCodeInfo,
                    appliedDiscounts
            );

        } catch (Exception e) {
            // Обработка ошибок
            List<ValidationError> errors = new ArrayList<>();
            errors.add(new ValidationError("system", "Calculation error: " + e.getMessage()));
            return new TravelCalculatePremiumResponseV2(errors);
        }
    }

    /**
     * Формирует детальный ответ
     */
    private TravelCalculatePremiumResponseV2 buildResponse(
            TravelCalculatePremiumRequestV2 request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult calculationResult,
            BigDecimal premiumBeforeDiscount,
            BigDecimal discountAmount,
            BigDecimal finalPremium,
            String currency,
            TravelCalculatePremiumResponseV2.PromoCodeInfo promoCodeInfo,
            List<TravelCalculatePremiumResponseV2.DiscountInfo> appliedDiscounts) {

        // Конвертация рисков в формат ответа
        List<TravelCalculatePremiumResponseV2.RiskPremium> riskPremiums = calculationResult.riskDetails().stream()
                .map(detail -> new TravelCalculatePremiumResponseV2.RiskPremium(
                        detail.riskCode(),
                        detail.riskName(),
                        detail.premium(),
                        detail.coefficient()
                ))
                .collect(Collectors.toList());

        // Конвертация шагов расчета
        List<TravelCalculatePremiumResponseV2.CalculationStep> steps = calculationResult.calculationSteps().stream()
                .map(step -> new TravelCalculatePremiumResponseV2.CalculationStep(
                        step.description(),
                        step.formula(),
                        step.result()
                ))
                .collect(Collectors.toList());

        // Детали расчета
        TravelCalculatePremiumResponseV2.CalculationDetails calculationDetails =
                new TravelCalculatePremiumResponseV2.CalculationDetails(
                        calculationResult.baseRate(),
                        calculationResult.ageCoefficient(),
                        calculationResult.countryCoefficient(),
                        calculationResult.additionalRisksCoefficient(),
                        calculationResult.totalCoefficient(),
                        calculationResult.days(),
                        buildFormula(calculationResult),
                        steps
                );

        // Формирование ответа через builder
        return TravelCalculatePremiumResponseV2.builder()
                .personFirstName(request.getPersonFirstName())
                .personLastName(request.getPersonLastName())
                .personBirthDate(request.getPersonBirthDate())
                .personAge(calculationResult.age())
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                .agreementDays(calculationResult.days())
                .countryIsoCode(request.getCountryIsoCode())
                .countryName(calculationResult.countryName())
                .medicalRiskLimitLevel(request.getMedicalRiskLimitLevel())
                .coverageAmount(calculationResult.coverageAmount())
                .selectedRisks(request.getSelectedRisks())
                .riskPremiums(riskPremiums)
                .agreementPriceBeforeDiscount(premiumBeforeDiscount)
                .discountAmount(discountAmount)
                .agreementPrice(finalPremium)
                .currency(currency)
                .calculation(calculationDetails)
                .promoCodeInfo(promoCodeInfo)
                .appliedDiscounts(appliedDiscounts.isEmpty() ? null : appliedDiscounts)
                .build();
    }

    /**
     * Формирует текстовое представление формулы
     */
    private String buildFormula(MedicalRiskPremiumCalculator.PremiumCalculationResult result) {
        StringBuilder formula = new StringBuilder();
        formula.append("Premium = ");
        formula.append(String.format("%.2f", result.baseRate()));
        formula.append(" × ");
        formula.append(String.format("%.2f", result.ageCoefficient()));
        formula.append(" × ");
        formula.append(String.format("%.2f", result.countryCoefficient()));

        if (result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ");
            formula.append(String.format("%.2f", result.additionalRisksCoefficient()));
            formula.append(")");
        }

        formula.append(" × ");
        formula.append(result.days());
        formula.append(" days");

        return formula.toString();
    }

    // ========== ВСПОМОГАТЕЛЬНЫЙ СЕРВИС ==========

    /**
     * Сервис конвертации валют (заглушка)
     * В реальном приложении будет работать с внешним API или БД
     */
    @Service
    static class CurrencyExchangeService {

        public BigDecimal convert(BigDecimal amount, String fromCurrency, String toCurrency) {
            if (fromCurrency.equals(toCurrency)) {
                return amount;
            }

            // Упрощенные курсы (в реальности из БД или API)
            BigDecimal rate = getExchangeRate(fromCurrency, toCurrency);
            return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
        }

        private BigDecimal getExchangeRate(String from, String to) {
            // Все курсы относительно EUR
            Map<String, BigDecimal> rates = Map.of(
                    "EUR", new BigDecimal("1.0"),
                    "USD", new BigDecimal("1.08"),
                    "GBP", new BigDecimal("0.85"),
                    "CHF", new BigDecimal("0.97"),
                    "JPY", new BigDecimal("158.0"),
                    "CNY", new BigDecimal("7.7"),
                    "RUB", new BigDecimal("105.0")
            );

            BigDecimal fromRate = rates.getOrDefault(from, BigDecimal.ONE);
            BigDecimal toRate = rates.getOrDefault(to, BigDecimal.ONE);

            return toRate.divide(fromRate, 6, RoundingMode.HALF_UP);
        }
    }

    /**
     * Валидатор запросов версии 2 (заглушка)
     * В реальном приложении будет полноценная валидация
     */
    @Service
    static class TravelCalculatePremiumRequestValidatorV2 {

        public List<ValidationError> validate(TravelCalculatePremiumRequestV2 request) {
            List<ValidationError> errors = new ArrayList<>();

            // Валидация имени
            if (request.getPersonFirstName() == null || request.getPersonFirstName().isEmpty()) {
                errors.add(new ValidationError("personFirstName", "Must not be empty!"));
            }

            // Валидация фамилии
            if (request.getPersonLastName() == null || request.getPersonLastName().isEmpty()) {
                errors.add(new ValidationError("personLastName", "Must not be empty!"));
            }

            // Валидация даты рождения
            if (request.getPersonBirthDate() == null) {
                errors.add(new ValidationError("personBirthDate", "Must not be empty!"));
            }

            // Валидация дат поездки
            if (request.getAgreementDateFrom() == null) {
                errors.add(new ValidationError("agreementDateFrom", "Must not be empty!"));
            }
            if (request.getAgreementDateTo() == null) {
                errors.add(new ValidationError("agreementDateTo", "Must not be empty!"));
            }
            if (request.getAgreementDateFrom() != null && request.getAgreementDateTo() != null) {
                if (request.getAgreementDateTo().isBefore(request.getAgreementDateFrom())) {
                    errors.add(new ValidationError("agreementDateTo", "Must be after agreementDateFrom!"));
                }
            }

            // Валидация страны
            if (request.getCountryIsoCode() == null || request.getCountryIsoCode().isEmpty()) {
                errors.add(new ValidationError("countryIsoCode", "Must not be empty!"));
            } else {
                try {
                    Country.fromIsoCode(request.getCountryIsoCode());
                } catch (IllegalArgumentException e) {
                    errors.add(new ValidationError("countryIsoCode", "Unknown country: " + request.getCountryIsoCode()));
                }
            }

            // Валидация уровня покрытия
            if (request.getMedicalRiskLimitLevel() == null || request.getMedicalRiskLimitLevel().isEmpty()) {
                errors.add(new ValidationError("medicalRiskLimitLevel", "Must not be empty!"));
            }

            return errors;
        }
    }
}