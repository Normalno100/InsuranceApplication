package org.javaguru.travel.insurance.core.services;

import lombok.RequiredArgsConstructor;
import org.javaguru.travel.insurance.core.TravelCalculatePremiumRequestValidator;
import org.javaguru.travel.insurance.core.calculators.MedicalRiskPremiumCalculator;
import org.javaguru.travel.insurance.core.underwriting.UnderwritingService;
import org.javaguru.travel.insurance.core.underwriting.domain.UnderwritingResult;
import org.javaguru.travel.insurance.dto.ValidationError;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelCalculatePremiumService {

    private final TravelCalculatePremiumRequestValidator validator;
    private final MedicalRiskPremiumCalculator medicalRiskCalculator;
    private final PromoCodeService promoCodeService;
    private final DiscountService discountService;
    private final UnderwritingService underwritingService;

    private static final BigDecimal MIN_PREMIUM = new BigDecimal("10.00");

    public TravelCalculatePremiumResponse calculatePremium(TravelCalculatePremiumRequest request) {
        // 1. Валидация
        List<ValidationError> errors = validator.validate(request);
        if (!errors.isEmpty()) {
            TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
            response.setErrors(errors);  // ✅ Используем сеттер из родительского класса
            return response;
        }

        // 2. Андеррайтинг (оценка рисков)
        UnderwritingResult underwritingResult = underwritingService.evaluateApplication(request);

        // 2a. Если заявка отклонена
        if (underwritingResult.isDeclined()) {
            TravelCalculatePremiumResponse response = TravelCalculatePremiumResponse.builder()
                    .personFirstName(request.getPersonFirstName())
                    .personLastName(request.getPersonLastName())
                    .underwritingDecision(underwritingResult.getDecision().name())
                    .declineReason(underwritingResult.getDeclineReason())
                    .build();
            // ✅ Устанавливаем errors через сеттер, а не через билдер
            response.setErrors(List.of(new ValidationError(
                    "underwriting",
                    "Application declined: " + underwritingResult.getDeclineReason()
            )));
            return response;
        }

        // 2b. Если требуется ручная проверка
        if (underwritingResult.requiresManualReview()) {
            TravelCalculatePremiumResponse response = TravelCalculatePremiumResponse.builder()
                    .personFirstName(request.getPersonFirstName())
                    .personLastName(request.getPersonLastName())
                    .underwritingDecision(underwritingResult.getDecision().name())
                    .reviewReason(underwritingResult.getDeclineReason())
                    .build();
            // ✅ Устанавливаем errors через сеттер
            response.setErrors(List.of(new ValidationError(
                    "underwriting",
                    "Manual review required: " + underwritingResult.getDeclineReason()
            )));
            return response;
        }

        try {
            // 3. Расчет базовой премии (только если одобрено)
            var calculationResult = medicalRiskCalculator.calculatePremiumWithDetails(request);
            BigDecimal premium = calculationResult.premium();

            // 4. Применение промо-кода и скидок
            BigDecimal totalDiscount = BigDecimal.ZERO;
            TravelCalculatePremiumResponse.PromoCodeInfo promoInfo = null;
            List<TravelCalculatePremiumResponse.DiscountInfo> discounts = new ArrayList<>();

            if (request.getPromoCode() != null && !request.getPromoCode().trim().isEmpty()) {
                var promoResult = promoCodeService.applyPromoCode(
                        request.getPromoCode(),
                        request.getAgreementDateFrom(),
                        premium
                );

                if (promoResult.isValid()) {
                    totalDiscount = promoResult.actualDiscountAmount();
                    promoInfo = new TravelCalculatePremiumResponse.PromoCodeInfo(
                            promoResult.code(),
                            promoResult.description(),
                            promoResult.discountType().name(),
                            promoResult.discountValue(),
                            promoResult.actualDiscountAmount()
                    );
                }
            }

            int personsCount = request.getPersonsCount() != null && request.getPersonsCount() > 0
                    ? request.getPersonsCount() : 1;
            boolean isCorporate = Boolean.TRUE.equals(request.getIsCorporate());

            var bestDiscount = discountService.calculateBestDiscount(
                    premium,
                    personsCount,
                    isCorporate,
                    request.getAgreementDateFrom()
            );

            if (bestDiscount.isPresent()) {
                var discount = bestDiscount.get();
                totalDiscount = totalDiscount.add(discount.amount());
                discounts.add(new TravelCalculatePremiumResponse.DiscountInfo(
                        discount.discountType().name(),
                        discount.name(),
                        discount.percentage(),
                        discount.amount()
                ));
            }

            BigDecimal finalPremium = applyMinimumPremium(premium.subtract(totalDiscount));
            String currency = request.getCurrency() != null && !request.getCurrency().trim().isEmpty()
                    ? request.getCurrency() : "EUR";

            var response = buildResponse(request, calculationResult, premium, totalDiscount,
                    finalPremium, currency, promoInfo, discounts);

            // 5. Добавляем информацию об андеррайтинге
            response.setUnderwritingDecision(underwritingResult.getDecision().name());

            return response;

        } catch (Exception e) {
            TravelCalculatePremiumResponse response = new TravelCalculatePremiumResponse();
            // ✅ Устанавливаем errors через сеттер
            response.setErrors(List.of(
                    new ValidationError("system", "Calculation error: " + e.getMessage())
            ));
            return response;
        }
    }

    private BigDecimal applyMinimumPremium(BigDecimal premium) {
        if (premium.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        if (premium.compareTo(MIN_PREMIUM) < 0) {
            return MIN_PREMIUM;
        }
        return premium;
    }

    private TravelCalculatePremiumResponse buildResponse(
            TravelCalculatePremiumRequest request,
            MedicalRiskPremiumCalculator.PremiumCalculationResult result,
            BigDecimal premiumBeforeDiscount,
            BigDecimal discountAmount,
            BigDecimal finalPremium,
            String currency,
            TravelCalculatePremiumResponse.PromoCodeInfo promoInfo,
            List<TravelCalculatePremiumResponse.DiscountInfo> discounts) {

        var riskPremiums = result.riskDetails().stream()
                .map(d -> new TravelCalculatePremiumResponse.RiskPremium(
                        d.riskCode(), d.riskName(), d.premium(), d.coefficient()
                ))
                .toList();

        var steps = result.calculationSteps().stream()
                .map(s -> new TravelCalculatePremiumResponse.CalculationStep(
                        s.description(), s.formula(), s.result()
                ))
                .toList();

        var calculationDetails = new TravelCalculatePremiumResponse.CalculationDetails(
                result.baseRate(),
                result.ageCoefficient(),
                result.countryCoefficient(),
                result.additionalRisksCoefficient(),
                result.totalCoefficient(),
                result.days(),
                buildFormula(result),
                steps
        );

        return TravelCalculatePremiumResponse.builder()
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
}