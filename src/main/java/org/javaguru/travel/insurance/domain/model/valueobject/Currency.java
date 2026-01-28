package org.javaguru.travel.insurance.domain.model.valueobject;

/**
 * Value Object для валюты
 */
public enum Currency {
    EUR("€"),
    USD("$"),
    GBP("£"),
    CHF("CHF"),
    JPY("¥");
    
    private final String symbol;
    
    Currency(String symbol) {
        this.symbol = symbol;
    }
    
    public String getSymbol() {
        return symbol;
    }
    
    /**
     * Получает валюту из строки, с дефолтом EUR
     */
    public static Currency fromStringOrDefault(String currencyStr) {
        if (currencyStr == null || currencyStr.trim().isEmpty()) {
            return EUR;
        }
        try {
            return Currency.valueOf(currencyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            return EUR;
        }
    }
}
