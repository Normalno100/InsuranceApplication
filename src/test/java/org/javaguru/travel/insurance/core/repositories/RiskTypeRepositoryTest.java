package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.RiskTypeEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для RiskTypeRepository
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/test-data/risk-types.sql")
class RiskTypeRepositoryTest {

    @Autowired
    private RiskTypeRepository repository;

    @Test
    void shouldFindRiskByCode() {
        // When
        boolean exists = repository.existsByCode("SPORT_ACTIVITIES");

        // Then
        assertTrue(exists);
    }

    @Test
    void shouldNotFindNonExistentRisk() {
        // When
        boolean exists = repository.existsByCode("INVALID_RISK");

        // Then
        assertFalse(exists);
    }

    @Test
    void shouldFindActiveRiskByCode() {
        // When
        Optional<RiskTypeEntity> risk = repository.findActiveByCode("SPORT_ACTIVITIES");

        // Then
        assertTrue(risk.isPresent());
        assertEquals("SPORT_ACTIVITIES", risk.get().getCode());
        assertEquals("Sport Activities", risk.get().getNameEn());
        assertEquals(new BigDecimal("0.30"), risk.get().getCoefficient());
        assertFalse(risk.get().getIsMandatory());
    }

    @Test
    void shouldFindMandatoryRisk() {
        // When
        Optional<RiskTypeEntity> risk = repository.findActiveByCode("TRAVEL_MEDICAL");

        // Then
        assertTrue(risk.isPresent());
        assertEquals("TRAVEL_MEDICAL", risk.get().getCode());
        assertTrue(risk.get().getIsMandatory());
        assertEquals(0, risk.get().getCoefficient().compareTo(BigDecimal.ZERO));
    }

    @Test
    void shouldFindActiveRiskOnSpecificDate() {
        // When
        Optional<RiskTypeEntity> risk = repository.findActiveByCode(
                "SPORT_ACTIVITIES",
                LocalDate.of(2024, 6, 15)
        );

        // Then
        assertTrue(risk.isPresent());
    }

    @Test
    void shouldNotFindRiskBeforeValidFrom() {
        // When
        Optional<RiskTypeEntity> risk = repository.findActiveByCode(
                "SPORT_ACTIVITIES",
                LocalDate.of(2019, 12, 31)
        );

        // Then
        assertTrue(risk.isEmpty());
    }

    @Test
    void shouldFindAllActiveRisks() {
        // When
        List<RiskTypeEntity> risks = repository.findAllActive();

        // Then
        assertFalse(risks.isEmpty());
        assertTrue(risks.size() >= 10); // 10 рисков в test data

        // Проверяем сортировку: сначала обязательные, потом по коду
        boolean foundOptional = false;
        for (RiskTypeEntity risk : risks) {
            if (!risk.getIsMandatory()) {
                foundOptional = true;
            } else {
                assertFalse(foundOptional,
                        "Mandatory risks should come before optional ones");
            }
        }
    }

    @Test
    void shouldFindAllMandatoryRisks() {
        // When
        List<RiskTypeEntity> mandatoryRisks = repository.findAllMandatory();

        // Then
        assertFalse(mandatoryRisks.isEmpty());
        assertEquals(1, mandatoryRisks.size()); // Только TRAVEL_MEDICAL
        assertTrue(mandatoryRisks.get(0).getIsMandatory());
        assertEquals("TRAVEL_MEDICAL", mandatoryRisks.get(0).getCode());
    }

    @Test
    void shouldFindAllOptionalRisks() {
        // When
        List<RiskTypeEntity> optionalRisks = repository.findAllOptional();

        // Then
        assertFalse(optionalRisks.isEmpty());
        assertTrue(optionalRisks.size() >= 9); // 9 опциональных рисков

        for (RiskTypeEntity risk : optionalRisks) {
            assertFalse(risk.getIsMandatory());
        }
    }

    @Test
    void shouldSaveNewRisk() {
        // Given
        RiskTypeEntity newRisk = new RiskTypeEntity();
        newRisk.setCode("NEW_RISK");
        newRisk.setNameEn("New Risk Type");
        newRisk.setNameRu("Новый тип риска");
        newRisk.setCoefficient(new BigDecimal("0.25"));
        newRisk.setIsMandatory(false);
        newRisk.setDescription("Test risk");
        newRisk.setValidFrom(LocalDate.now());

        // When
        RiskTypeEntity saved = repository.save(newRisk);

        // Then
        assertNotNull(saved.getId());
        assertEquals("NEW_RISK", saved.getCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingRisk() {
        // Given
        Optional<RiskTypeEntity> riskOpt = repository.findActiveByCode("SPORT_ACTIVITIES");
        assertTrue(riskOpt.isPresent());

        RiskTypeEntity risk = riskOpt.get();
        BigDecimal newCoefficient = new BigDecimal("0.35");

        // When
        risk.setCoefficient(newCoefficient);
        repository.save(risk);

        // Then
        RiskTypeEntity updated = repository.findById(risk.getId()).orElseThrow();
        assertEquals(newCoefficient, updated.getCoefficient());
    }

    @Test
    void shouldCheckIfRiskIsActive() {
        // Given
        Optional<RiskTypeEntity> riskOpt = repository.findActiveByCode("SPORT_ACTIVITIES");
        assertTrue(riskOpt.isPresent());

        // When
        RiskTypeEntity risk = riskOpt.get();

        // Then
        assertTrue(risk.isActive());
        assertTrue(risk.isActiveOn(LocalDate.now()));
    }

    @Test
    void shouldHandleExpiredRisk() {
        // Given: создаем риск с истекшим сроком
        RiskTypeEntity expiredRisk = new RiskTypeEntity();
        expiredRisk.setCode("EXPIRED_RISK");
        expiredRisk.setNameEn("Expired Risk");
        expiredRisk.setCoefficient(new BigDecimal("0.5"));
        expiredRisk.setIsMandatory(false);
        expiredRisk.setValidFrom(LocalDate.of(2020, 1, 1));
        expiredRisk.setValidTo(LocalDate.of(2023, 12, 31));
        repository.save(expiredRisk);

        // When
        Optional<RiskTypeEntity> found = repository.findActiveByCode("EXPIRED_RISK");

        // Then
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldVerifyAllStandardRisksExist() {
        // When
        List<RiskTypeEntity> allRisks = repository.findAllActive();

        // Then
        List<String> expectedCodes = List.of(
                "TRAVEL_MEDICAL",
                "SPORT_ACTIVITIES",
                "EXTREME_SPORT",
                "PREGNANCY",
                "CHRONIC_DISEASES",
                "ACCIDENT_COVERAGE",
                "TRIP_CANCELLATION",
                "LUGGAGE_LOSS",
                "FLIGHT_DELAY",
                "CIVIL_LIABILITY"
        );

        for (String code : expectedCodes) {
            assertTrue(allRisks.stream().anyMatch(r -> r.getCode().equals(code)),
                    "Risk " + code + " should exist");
        }
    }

    @Test
    void shouldVerifyRiskCoefficients() {
        // Given
        List<RiskTypeEntity> risks = repository.findAllActive();

        // Then
        for (RiskTypeEntity risk : risks) {
            assertNotNull(risk.getCoefficient());
            assertTrue(risk.getCoefficient().compareTo(BigDecimal.ZERO) >= 0,
                    "Coefficient should be >= 0");

            if (risk.getIsMandatory()) {
                assertEquals(0, risk.getCoefficient().compareTo(BigDecimal.ZERO),
                        "Mandatory risks should have 0 coefficient");
            }
        }
    }

    @Test
    void shouldSortOptionalRisksByCode() {
        // When
        List<RiskTypeEntity> optionalRisks = repository.findAllOptional();

        // Then
        for (int i = 1; i < optionalRisks.size(); i++) {
            String current = optionalRisks.get(i).getCode();
            String previous = optionalRisks.get(i - 1).getCode();

            assertTrue(current.compareTo(previous) >= 0,
                    "Optional risks should be sorted by code");
        }
    }
}