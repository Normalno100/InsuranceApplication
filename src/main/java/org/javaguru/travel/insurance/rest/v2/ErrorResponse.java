package org.javaguru.travel.insurance.rest.v2;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
