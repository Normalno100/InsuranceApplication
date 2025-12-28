package org.javaguru.travel.insurance.core.underwriting;

import org.javaguru.travel.insurance.core.domain.entities.UnderwritingDecisionEntity;
import org.javaguru.travel.insurance.core.repositories.UnderwritingDecisionRepository;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(locations = "classpath:application-test.properties")
@Sql(scripts = {
        "/test-data/countries.sql",
        "/test-data/medical-risk-limit-levels.sql",
        "/test-data/risk-types.sql"
})
@DisplayName("Underwriting Integration Tests")
class UnderwritingIntegrationTest {

    @Autowired
    private UnderwritingService underwritingService;

    @Autowired
    private UnderwritingDecisionRepository decisionRepository;

    @Test
    @DisplayName("should save decision to database")
    void shouldSaveDecisionToDatabase() {
        // Given
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(LocalDate.of(1990, 1, 1))
                .agreementDateFrom(LocalDate.now())
                .agreementDateTo(LocalDate.now().plusDays(10))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000")
                .build();

        // When
        var result = underwritingService.evaluateApplication(request);

        // Then
        assertThat(result).isNotNull();

        // Check database
        var decisions = decisionRepository.findByPersonFirstNameAndPersonLastNameAndPersonBirthDate(
                "John", "Doe", LocalDate.of(1990, 1, 1)
        );

        assertThat(decisions).isNotEmpty();
        UnderwritingDecisionEntity savedDecision = decisions.get(0);
        assertThat(savedDecision.getDecision()).isEqualTo(result.getDecision().name());
        assertThat(savedDecision.getCountryIsoCode()).isEqualTo("ES");
    }
}