package org.javaguru.travel.insurance.rest;

public record ErrorResponse(
        String error,
        String message,
        long timestamp
) {}
