package org.javaguru.travel.insurance.core;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
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
import java.util.stream.Collectors;

/**
 * Сервис расчета страховой премии
 */
@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumServiceV2 {

    private final TravelCalculatePremiumRequestValidatorV2Impl validator;
    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final PromoCodeService promoCodeService;
    private final DiscountService discountService;
    private final CurrencyExchangeService currencyExchangeService;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    public TravelCalculatePremiumResponseV2 calculatePremium(TravelCalculatePremiumRequestV2 request) {
        // Валидация
        List<ValidationError> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            return new TravelCalculatePremiumResponseV2(errors);
        }

        try {
            // Расчет базовой премии
            var calculationResult = medicalRiskCalculator.calculatePremiumWithDetails(request);
            BigDecimal premium = calculationResult.premium();

            // Применение промо-кода и скидок
            BigDecimal totalDiscount = BigDecimal.ZERO;
            TravelCalculatePremiumResponseV2.PromoCodeInfo promoInfo = null;
            List<TravelCalculatePremiumResponseV2.DiscountInfo> discounts = new ArrayList<>();

            if (request.hasPromoCode()) {
                var promoResult = promoCodeService.applyPromoCode(
                        request.getPromoCode(),
                        request.getAgreementDateFrom(),
                        premium
                );

                if (promoResult.isValid()) {
                    totalDiscount = promoResult.actualDiscountAmount();
                    promoInfo = new TravelCalculatePremiumResponseV2.PromoCodeInfo(
                            promoResult.code(),
                            promoResult.description(),
                            promoResult.discountType().name(),
                            promoResult.discountValue(),
                            promoResult.actualDiscountAmount()
                    );
                }
            }

            // Дополнительные скидки
            var bestDiscount = discountService.calculateBestDiscount(
                    premium,
                    request.getPersonsCountOrDefault(),
                    request.isCorporateClient(),
                    request.getAgreementDateFrom()
            );

            if (bestDiscount.isPresent()) {
                var discount = bestDiscount.get();
                totalDiscount = totalDiscount.add(discount.amount());
                discounts.add(new TravelCalculatePremiumResponseV2.DiscountInfo(
                        discount.discountType().name(),
                        discount.name(),
                        discount.percentage(),
                        discount.amount()
                ));
            }

            // Итоговая премия
            BigDecimal finalPremium = applyMinimumPremium(premium.subtract(totalDiscount));

            // Конвертация валюты
            String currency = request.getCurrencyOrDefault();
            if (!"EUR".equals(currency)) {
                premium = currencyExchangeService.convert(premium, "EUR", currency);
                totalDiscount = currencyExchangeService.convert(totalDiscount, "EUR", currency);
                finalPremium = currencyExchangeService.convert(finalPremium, "EUR", currency);
            }

            return buildResponse(request, calculationResult, premium, totalDiscount,
                    finalPremium, currency, promoInfo, discounts);

        } catch (Exception e) {
            return new TravelCalculatePremiumResponseV2(List.of(
                    new ValidationError("system", "Calculation error: " + e.getMessage())
            ));
        }
    }

    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0 && premium.compareTo(BigDecimal.ZERO) > 0) {
            return MIN_PREMIUM;
        }
        return premium;
    }

    private TravelCalculatePremiumResponseV2 buildResponse(
            TravelCalculatePremiumRequestV2 request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult result,
            BigDecimal premiumBeforeDiscount,
            BigDecimal discountAmount,
            BigDecimal finalPremium,
            String currency,
            TravelCalculatePremiumResponseV2.PromoCodeInfo promoInfo,
            List<TravelCalculatePremiumResponseV2.DiscountInfo> discounts) {

        var riskPremiums = result.riskDetails().stream()
                .map(d -> new TravelCalculatePremiumResponseV2.RiskPremium(
                        d.riskCode(), d.riskName(), d.premium(), d.coefficient()
                ))
                .collect(Collectors.toList());

        var steps = result.calculationSteps().stream()
                .map(s -> new TravelCalculatePremiumResponseV2.CalculationStep(
                        s.description(), s.formula(), s.result()
                ))
                .collect(Collectors.toList());

        var calculationDetails = new TravelCalculatePremiumResponseV2.CalculationDetails(
                result.baseRate(),
                result.ageCoefficient(),
                result.countryCoefficient(),
                result.additionalRisksCoefficient(),
                result.totalCoefficient(),
                result.days(),
                buildFormula(result),
                steps
        );

        return TravelCalculatePremiumResponseV2.builder()
                .personFirstName(request.getPersonFirstName())
                .personLastName(request.getPersonLastName())
                .personBirthDate(request.getPersonBirthDate())
                .personAge(result.age())
                .agreementDateFrom(request.getAgreementDateFrom())
                .agreementDateTo(request.getAgreementDateTo())
                .agreementDays(result.days())
                .countryIsoCode(request.getCountryIsoCode())
                .countryName(result.countryName())
                .medicalRiskLimitLevel(request.getMedicalRiskLimitLevel())
                .coverageAmount(result.coverageAmount())
                .selectedRisks(request.getSelectedRisks())
                .riskPremiums(riskPremiums)
                .agreementPriceBeforeDiscount(premiumBeforeDiscount)
                .discountAmount(discountAmount)
                .agreementPrice(finalPremium)
                .currency(currency)
                .calculation(calculationDetails)
                .promoCodeInfo(promoInfo)
                .appliedDiscounts(discounts.isEmpty() ? null : discounts)
                .build();
    }

    private String buildFormula(MedicalRiskPremiumCalculator.PremiumCalculationResult result) {
        var formula = new StringBuilder("Premium = ")
                .append(String.format("%.2f", result.baseRate()))
                .append(" × ")
                .append(String.format("%.2f", result.ageCoefficient()))
                .append(" × ")
                .append(String.format("%.2f", result.countryCoefficient()));

        if (result.additionalRisksCoefficient().compareTo(BigDecimal.ZERO) > 0) {
            formula.append(" × (1 + ")
                    .append(String.format("%.2f", result.additionalRisksCoefficient()))
                    .append(")");
        }

        return formula.append(" × ").append(result.days()).append(" days").toString();
    }

    @Service
    static class CurrencyExchangeService {
        private static final java.util.Map<String, BigDecimal> RATES = java.util.Map.of(
                "EUR", new BigDecimal("1.0"),
                "USD", new BigDecimal("1.08"),
                "GBP", new BigDecimal("0.85"),
                "CHF", new BigDecimal("0.97"),
                "JPY", new BigDecimal("158.0"),
                "CNY", new BigDecimal("7.7"),
                "RUB", new BigDecimal("105.0")
        );

        public BigDecimal convert(BigDecimal amount, String from, String to) {
            if (from.equals(to)) return amount;

            BigDecimal fromRate = RATES.getOrDefault(from, BigDecimal.ONE);
            BigDecimal toRate = RATES.getOrDefault(to, BigDecimal.ONE);

            return amount.multiply(toRate.divide(fromRate, 6, RoundingMode.HALF_UP))
                    .setScale(2, RoundingMode.HALF_UP);
        }
    }
}