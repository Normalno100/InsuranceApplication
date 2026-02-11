package org.javaguru.travel.insurance.application.validation.rule.reference;

import org.javaguru.travel.insurance.application.validation.AbstractValidationRule;
import org.javaguru.travel.insurance.application.validation.ValidationContext;
import org.javaguru.travel.insurance.application.validation.ValidationError;
import org.javaguru.travel.insurance.application.validation.ValidationResult;
import org.javaguru.travel.insurance.application.validation.*;
import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.util.Set;

/**
 * Проверяет что валюта поддерживается системой
 * (если указана в запросе)
 */
public class CurrencySupportValidator extends AbstractValidationRule<TravelCalculatePremiumRequest> {

    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "EUR", "USD", "GBP", "CHF", "JPY"
    );

    public CurrencySupportValidator() {
        super("CurrencySupportValidator", 250);
    }

    @Override
    protected ValidationResult doValidate(TravelCalculatePremiumRequest request,
                                          ValidationContext context) {
        String currency = request.getCurrency();

        // Если валюта не указана, используем дефолтную (EUR)
        if (currency == null || currency.trim().isEmpty()) {
            return success();
        }

        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            return ValidationResult.failure(
                    ValidationError.error(
                                    "currency",
                                    String.format(
                                            "Currency '%s' is not supported! Supported currencies: %s",
                                            currency, SUPPORTED_CURRENCIES
                                    )
                            )
                            .withParameter("currency", currency)
                            .withParameter("supportedCurrencies", SUPPORTED_CURRENCIES)
            );
        }

        return success();
    }
}