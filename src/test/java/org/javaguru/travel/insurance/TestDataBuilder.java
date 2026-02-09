package org.javaguru.travel.insurance;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import java.time.LocalDate;
import java.util.List;

public class TestDataBuilder {

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder validRequest() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1985, 5, 15))
                .personEmail("john.doe@example.com")
                .personPhone("+1234567890")
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(17))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .currency("EUR");
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder requestWithRisks(
            String... riskCodes) {
        return validRequest()
                .selectedRisks(List.of(riskCodes));
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder elderlyRequest() {
        return validRequest()
                .personBirthDate(LocalDate.of(1945, 3, 10)); // 80 years old
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder seniorRequest() {
        return validRequest()
                .personBirthDate(LocalDate.of(1950, 6, 20)); // 75 years old
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder youngAdultRequest() {
        return validRequest()
                .personBirthDate(LocalDate.of(2000, 1, 1)); // 25 years old
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder highRiskCountryRequest() {
        return validRequest()
                .countryIsoCode("IN"); // India - HIGH risk
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder veryHighRiskCountryRequest() {
        return validRequest()
                .countryIsoCode("AF"); // Afghanistan - VERY_HIGH risk
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder longTripRequest() {
        return validRequest()
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(97)); // 90+ days
    }

    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder veryLongTripRequest() {
        return validRequest()
                .agreementDateFrom(LocalDate.now().plusDays(7))
                .agreementDateTo(LocalDate.now().plusDays(190)); // 180+ days
    }
}