package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.MedicalRiskLimitLevelEntity;
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
 * Интеграционные тесты для MedicalRiskLimitLevelRepository
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/test-data/medical-risk-limit-levels.sql")
class MedicalRiskLimitLevelRepositoryTest {

    @Autowired
    private MedicalRiskLimitLevelRepository repository;

    @Test
    void shouldFindLevelByCode() {
        // When
        boolean exists = repository.existsByCode("10000");

        // Then
        assertTrue(exists);
    }

    @Test
    void shouldNotFindNonExistentLevel() {
        // When
        boolean exists = repository.existsByCode("999999");

        // Then
        assertFalse(exists);
    }

    @Test
    void shouldFindActiveLevelByCode() {
        // When
        Optional<MedicalRiskLimitLevelEntity> level = repository.findActiveByCode("10000");

        // Then
        assertTrue(level.isPresent());
        assertEquals("10000", level.get().getCode());
        assertEquals(new BigDecimal("10000.00"), level.get().getCoverageAmount());
        assertEquals(new BigDecimal("2.00"), level.get().getDailyRate());
        assertEquals("EUR", level.get().getCurrency());
    }

    @Test
    void shouldFindActiveLevelOnSpecificDate() {
        // When
        Optional<MedicalRiskLimitLevelEntity> level = repository.findActiveByCode(
                "10000",
                LocalDate.of(2024, 6, 15)
        );

        // Then
        assertTrue(level.isPresent());
    }

    @Test
    void shouldNotFindLevelBeforeValidFrom() {
        // When
        Optional<MedicalRiskLimitLevelEntity> level = repository.findActiveByCode(
                "10000",
                LocalDate.of(2019, 12, 31)
        );

        // Then
        assertTrue(level.isEmpty());
    }

    @Test
    void shouldFindAllActiveLevels() {
        // When
        List<MedicalRiskLimitLevelEntity> levels = repository.findAllActive();

        // Then
        assertFalse(levels.isEmpty());
        assertTrue(levels.size() >= 7); // 7 уровней в test data

        // Проверяем сортировку по coverageAmount
        for (int i = 1; i < levels.size(); i++) {
            assertTrue(levels.get(i).getCoverageAmount()
                    .compareTo(levels.get(i - 1).getCoverageAmount()) >= 0);
        }
    }

    @Test
    void shouldSaveNewLevel() {
        // Given
        MedicalRiskLimitLevelEntity newLevel = new MedicalRiskLimitLevelEntity();
        newLevel.setCode("15000");  // Валидный код
        newLevel.setCoverageAmount(new BigDecimal("15000.00"));
        newLevel.setDailyRate(new BigDecimal("2.50"));
        newLevel.setCurrency("EUR");
        newLevel.setValidFrom(LocalDate.now());

        // When
        MedicalRiskLimitLevelEntity saved = repository.save(newLevel);

        // Then
        assertNotNull(saved.getId());
        assertEquals("15000", saved.getCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingLevel() {
        // Given
        Optional<MedicalRiskLimitLevelEntity> levelOpt = repository.findActiveByCode("10000");
        assertTrue(levelOpt.isPresent());

        MedicalRiskLimitLevelEntity level = levelOpt.get();
        BigDecimal newRate = new BigDecimal("2.50");

        // When
        level.setDailyRate(newRate);
        repository.save(level);

        // Then
        MedicalRiskLimitLevelEntity updated = repository.findById(level.getId()).orElseThrow();
        assertEquals(newRate, updated.getDailyRate());
    }

    @Test
    void shouldCheckIfLevelIsActive() {
        // Given
        Optional<MedicalRiskLimitLevelEntity> levelOpt = repository.findActiveByCode("10000");
        assertTrue(levelOpt.isPresent());

        // When
        MedicalRiskLimitLevelEntity level = levelOpt.get();

        // Then
        assertTrue(level.isActive());
        assertTrue(level.isActiveOn(LocalDate.now()));
    }

    @Test
    void shouldHandleExpiredLevel() {
        // Given: создаем уровень с истекшим сроком
        MedicalRiskLimitLevelEntity expiredLevel = new MedicalRiskLimitLevelEntity();
        expiredLevel.setCode("EXPIRED");
        expiredLevel.setCoverageAmount(new BigDecimal("1000.00"));
        expiredLevel.setDailyRate(new BigDecimal("1.00"));
        expiredLevel.setCurrency("EUR");
        expiredLevel.setValidFrom(LocalDate.of(2020, 1, 1));
        expiredLevel.setValidTo(LocalDate.of(2023, 12, 31));
        repository.save(expiredLevel);

        // When
        Optional<MedicalRiskLimitLevelEntity> found = repository.findActiveByCode("EXPIRED");

        // Then
        assertTrue(found.isEmpty());
    }

    @Test
    void shouldReturnLevelsInAscendingOrderByCoverage() {
        // When
        List<MedicalRiskLimitLevelEntity> levels = repository.findAllActive();

        // Then
        for (int i = 1; i < levels.size(); i++) {
            BigDecimal current = levels.get(i).getCoverageAmount();
            BigDecimal previous = levels.get(i - 1).getCoverageAmount();

            assertTrue(current.compareTo(previous) >= 0,
                    "Levels should be sorted by coverage amount ascending");
        }
    }

    @Test
    void shouldFindAllSevenStandardLevels() {
        // When
        List<MedicalRiskLimitLevelEntity> levels = repository.findAllActive();

        // Then
        // Проверяем наличие всех стандартных уровней
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("5000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("10000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("20000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("50000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("100000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("200000")));
        assertTrue(levels.stream().anyMatch(l -> l.getCode().equals("500000")));
    }
}