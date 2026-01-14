package org.javaguru.travel.insurance.core.validation;

/**
 * Константы для кодов ошибок валидации (для i18n)
 */
public final class ValidationErrorCodes {

    private ValidationErrorCodes() {
        throw new AssertionError("Cannot instantiate constants class");
    }

    // Structural validation
    public static final String NOT_NULL = "validation.not_null";
    public static final String NOT_BLANK = "validation.not_blank";
    public static final String INVALID_LENGTH = "validation.invalid_length";
    public static final String INVALID_FORMAT = "validation.invalid_format";
    public static final String INVALID_PATTERN = "validation.invalid_pattern";

    // Date validation
    public static final String INVALID_DATE = "validation.date.invalid";
    public static final String DATE_IN_PAST = "validation.date.in_past";
    public static final String DATE_IN_FUTURE = "validation.date.in_future";
    public static final String INVALID_DATE_RANGE = "validation.date.invalid_range";

    // Business validation
    public static final String AGE_TOO_LOW = "validation.age.too_low";
    public static final String AGE_TOO_HIGH = "validation.age.too_high";
    public static final String TRIP_TOO_LONG = "validation.trip.too_long";

    // Reference validation
    public static final String COUNTRY_NOT_FOUND = "validation.country.not_found";
    public static final String MEDICAL_LEVEL_NOT_FOUND = "validation.medical_level.not_found";
    public static final String RISK_TYPE_NOT_FOUND = "validation.risk_type.not_found";
    public static final String RISK_TYPE_MANDATORY = "validation.risk_type.mandatory";

    // Complex validation
    public static final String PROMO_CODE_INVALID = "validation.promo_code.invalid";
    public static final String PROMO_CODE_EXPIRED = "validation.promo_code.expired";
    public static final String PROMO_CODE_LIMIT_REACHED = "validation.promo_code.limit_reached";
}