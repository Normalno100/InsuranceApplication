package org.javaguru.travel.insurance.infrastructure.web.error;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
