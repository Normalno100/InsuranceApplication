package org.javaguru.travel.insurance.core.domain.entities;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для Entity классов
 * Проверяют маппинг, каскадные операции, и JPA lifecycle callbacks
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class EntityIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldPersistCountryEntity() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("XX");  // 2 символа
        country.setNameEn("Test Country");
        country.setNameRu("Тестовая страна");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setValidFrom(LocalDate.now());

        // When
        CountryEntity saved = entityManager.persistAndFlush(country);

        // Then
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("XX", saved.getIsoCode());
    }

    @Test
    void shouldUpdateCountryEntityTimestamp() throws InterruptedException {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("XY");
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setValidFrom(LocalDate.now());

        entityManager.persistAndFlush(country);
        entityManager.clear();

        CountryEntity initial = entityManager.find(CountryEntity.class, country.getId());
        LocalDateTime createdAt = initial.getCreatedAt();
        LocalDateTime updatedAt1 = initial.getUpdatedAt();

        Thread.sleep(10);

        // When
        initial.setNameEn("Updated Country");
        entityManager.persistAndFlush(initial);
        entityManager.clear();

        CountryEntity updated = entityManager.find(CountryEntity.class, initial.getId());

        // Then
        assertNotNull(updated.getCreatedAt());
        assertEquals(createdAt, updated.getCreatedAt());
        assertTrue(updated.getUpdatedAt().isAfter(updatedAt1));
    }

    @Test
    void shouldPersistMedicalRiskLimitLevelEntity() {
        // Given
        MedicalRiskLimitLevelEntity level = new MedicalRiskLimitLevelEntity();
        level.setCode("15000");  // Валидный код
        level.setCoverageAmount(new BigDecimal("15000.00"));
        level.setDailyRate(new BigDecimal("2.50"));
        level.setCurrency("EUR");
        level.setValidFrom(LocalDate.now());

        // When
        MedicalRiskLimitLevelEntity saved = entityManager.persistAndFlush(level);

        // Then
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("15000", saved.getCode());
        assertEquals("EUR", saved.getCurrency());
    }

    @Test
    void shouldPersistRiskTypeEntity() {
        // Given
        RiskTypeEntity risk = new RiskTypeEntity();
        risk.setCode("TEST_RISK");
        risk.setNameEn("Test Risk");
        risk.setNameRu("Тестовый риск");
        risk.setCoefficient(new BigDecimal("0.25"));
        risk.setIsMandatory(false);
        risk.setDescription("Test description");
        risk.setValidFrom(LocalDate.now());

        // When
        RiskTypeEntity saved = entityManager.persistAndFlush(risk);

        // Then
        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
        assertEquals("TEST_RISK", saved.getCode());
        assertFalse(saved.getIsMandatory());
    }

    @Test
    void shouldSetDefaultValuesOnPersist() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("YY");  // 2 символа
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        // Не устанавливаем validFrom

        // When
        CountryEntity saved = entityManager.persistAndFlush(country);

        // Then
        assertNotNull(saved.getValidFrom());
        assertEquals(LocalDate.now(), saved.getValidFrom());
    }

    @Test
    void shouldCheckCountryIsActive() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("ZZ");  // 2 символа
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setValidFrom(LocalDate.now().minusDays(10));
        country.setValidTo(LocalDate.now().plusDays(10));

        CountryEntity saved = entityManager.persistAndFlush(country);

        // Then
        assertTrue(saved.isActive());
        assertTrue(saved.isActiveOn(LocalDate.now()));
        assertTrue(saved.isActiveOn(LocalDate.now().plusDays(5)));
        assertFalse(saved.isActiveOn(LocalDate.now().minusDays(11)));
        assertFalse(saved.isActiveOn(LocalDate.now().plusDays(11)));
    }

    @Test
    void shouldCheckMedicalLevelIsActive() {
        // Given
        MedicalRiskLimitLevelEntity level = new MedicalRiskLimitLevelEntity();
        level.setCode("12000");  // Валидный код
        level.setCoverageAmount(new BigDecimal("10000"));
        level.setDailyRate(new BigDecimal("2.00"));
        level.setCurrency("EUR");
        level.setValidFrom(LocalDate.now().minusDays(10));
        level.setValidTo(LocalDate.now().plusDays(10));

        MedicalRiskLimitLevelEntity saved = entityManager.persistAndFlush(level);

        // Then
        assertTrue(saved.isActive());
        assertTrue(saved.isActiveOn(LocalDate.now()));
    }

    @Test
    void shouldCheckRiskIsActive() {
        // Given
        RiskTypeEntity risk = new RiskTypeEntity();
        risk.setCode("TEST_RISK_001");  // Уникальный валидный код
        risk.setNameEn("Test");
        risk.setCoefficient(new BigDecimal("0.1"));
        risk.setIsMandatory(false);
        risk.setValidFrom(LocalDate.now().minusDays(10));
        risk.setValidTo(LocalDate.now().plusDays(10));

        RiskTypeEntity saved = entityManager.persistAndFlush(risk);

        // Then
        assertTrue(saved.isActive());
        assertTrue(saved.isActiveOn(LocalDate.now()));
    }

    @Test
    void shouldHandleNullValidTo() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("QQ");  // 2 символа
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));
        country.setValidFrom(LocalDate.now());
        country.setValidTo(null); // Без даты окончания

        CountryEntity saved = entityManager.persistAndFlush(country);

        // Then
        assertTrue(saved.isActive());
        assertTrue(saved.isActiveOn(LocalDate.now().plusYears(100)));
    }

    @Test
    void shouldSetDefaultIsMandatoryForRisk() {
        // Given
        RiskTypeEntity risk = new RiskTypeEntity();
        risk.setCode("TEST_RISK_002");  // Уникальный код
        risk.setNameEn("Test");
        risk.setCoefficient(new BigDecimal("0.1"));
        // Не устанавливаем isMandatory

        // When
        RiskTypeEntity saved = entityManager.persistAndFlush(risk);

        // Then
        assertNotNull(saved.getIsMandatory());
        assertFalse(saved.getIsMandatory());
    }

    @Test
    void shouldFindEntityById() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("WW");  // 2 символа
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));

        CountryEntity saved = entityManager.persistAndFlush(country);
        Long id = saved.getId();
        entityManager.clear();

        // When
        CountryEntity found = entityManager.find(CountryEntity.class, id);

        // Then
        assertNotNull(found);
        assertEquals("WW", found.getIsoCode());
    }

    @Test
    void shouldDeleteEntity() {
        // Given
        CountryEntity country = new CountryEntity();
        country.setIsoCode("VV");  // 2 символа
        country.setNameEn("Test Country");
        country.setRiskGroup("LOW");
        country.setRiskCoefficient(new BigDecimal("1.0"));

        CountryEntity saved = entityManager.persistAndFlush(country);
        Long id = saved.getId();

        // When
        entityManager.remove(saved);
        entityManager.flush();
        entityManager.clear();

        // Then
        CountryEntity found = entityManager.find(CountryEntity.class, id);
        assertNull(found);
    }
}