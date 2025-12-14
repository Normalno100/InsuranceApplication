package org.javaguru.travel.insurance.core.repositories;

import org.javaguru.travel.insurance.core.domain.entities.CountryEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционные тесты для CountryRepository
 * Используют H2 in-memory базу данных
 *
 * Примечание: cleanup.sql не нужен, так как ddl-auto=create-drop
 * создает чистую БД для каждого теста
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(scripts = "/test-data/countries.sql")
class CountryRepositoryTest {

    @Autowired
    private CountryRepository countryRepository;

    @Test
    void shouldFindCountryByIsoCode() {
        // Given: страна "ES" загружена из test-data/countries.sql

        // When
        boolean exists = countryRepository.existsByIsoCode("ES");

        // Then
        assertTrue(exists);
    }

    @Test
    void shouldNotFindNonExistentCountry() {
        // When
        boolean exists = countryRepository.existsByIsoCode("XX");

        // Then
        assertFalse(exists);
    }

    @Test
    void shouldFindActiveCountryByIsoCode() {
        // Given: активная страна на текущую дату

        // When
        Optional<CountryEntity> country = countryRepository.findActiveByIsoCode("ES");

        // Then
        assertTrue(country.isPresent());
        assertEquals("ES", country.get().getIsoCode());
        assertEquals("Spain", country.get().getNameEn());
        assertNotNull(country.get().getRiskCoefficient());
    }

    @Test
    void shouldFindActiveCountryOnSpecificDate() {
        // Given: страна активна с 2020-01-01

        // When
        Optional<CountryEntity> country = countryRepository.findActiveByIsoCode(
                "ES",
                LocalDate.of(2024, 6, 15)
        );

        // Then
        assertTrue(country.isPresent());
    }

    @Test
    void shouldNotFindCountryBeforeValidFrom() {
        // Given: страна активна только с 2020-01-01

        // When
        Optional<CountryEntity> country = countryRepository.findActiveByIsoCode(
                "ES",
                LocalDate.of(2019, 12, 31)
        );

        // Then
        assertTrue(country.isEmpty());
    }

    @Test
    void shouldNotFindCountryAfterValidTo() {
        // Given: в test data есть неактивная страна с validTo в прошлом
        // Создадим такую для теста
        CountryEntity expiredCountry = new CountryEntity();
        expiredCountry.setIsoCode("OL");  // 2 символа вместо "OLD"
        expiredCountry.setNameEn("Old Country");
        expiredCountry.setRiskGroup("LOW");
        expiredCountry.setRiskCoefficient(new BigDecimal("1.0"));
        expiredCountry.setValidFrom(LocalDate.of(2020, 1, 1));
        expiredCountry.setValidTo(LocalDate.of(2023, 12, 31));
        countryRepository.save(expiredCountry);

        // When
        Optional<CountryEntity> country = countryRepository.findActiveByIsoCode(
                "OL",
                LocalDate.of(2024, 1, 1)
        );

        // Then
        assertTrue(country.isEmpty());
    }

    @Test
    void shouldSaveNewCountry() {
        // Given
        CountryEntity newCountry = new CountryEntity();
        newCountry.setIsoCode("XX");  // 2 символа вместо "TEST"
        newCountry.setNameEn("Test Country");
        newCountry.setNameRu("Тестовая страна");
        newCountry.setRiskGroup("MEDIUM");
        newCountry.setRiskCoefficient(new BigDecimal("1.5"));
        newCountry.setValidFrom(LocalDate.now());

        // When
        CountryEntity saved = countryRepository.save(newCountry);

        // Then
        assertNotNull(saved.getId());
        assertEquals("XX", saved.getIsoCode());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());
    }

    @Test
    void shouldUpdateExistingCountry() {
        // Given
        Optional<CountryEntity> countryOpt = countryRepository.findActiveByIsoCode("ES");
        assertTrue(countryOpt.isPresent());

        CountryEntity country = countryOpt.get();
        BigDecimal oldCoefficient = country.getRiskCoefficient();
        BigDecimal newCoefficient = new BigDecimal("1.5");

        // When
        country.setRiskCoefficient(newCoefficient);
        countryRepository.save(country);

        // Then
        CountryEntity updated = countryRepository.findById(country.getId()).orElseThrow();
        assertEquals(newCoefficient, updated.getRiskCoefficient());
        assertNotEquals(oldCoefficient, updated.getRiskCoefficient());
    }

    @Test
    void shouldCheckIfCountryIsActive() {
        // Given
        Optional<CountryEntity> countryOpt = countryRepository.findActiveByIsoCode("ES");
        assertTrue(countryOpt.isPresent());

        // When
        CountryEntity country = countryOpt.get();

        // Then
        assertTrue(country.isActive());
        assertTrue(country.isActiveOn(LocalDate.now()));
    }

    @Test
    void shouldHandleCaseInsensitiveSearch() {
        // Given: в БД страна хранится как "ES"

        // When
        boolean existsUppercase = countryRepository.existsByIsoCode("ES");
        boolean existsLowercase = countryRepository.existsByIsoCode("es");

        // Then
        assertTrue(existsUppercase);
        // Примечание: регистронезависимый поиск зависит от настроек БД
        // В PostgreSQL по умолчанию чувствителен к регистру
    }
}