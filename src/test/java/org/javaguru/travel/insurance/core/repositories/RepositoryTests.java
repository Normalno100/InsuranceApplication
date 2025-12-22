package org.javaguru.travel.insurance.core.repositories;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Упрощённые тесты репозиториев
 * Фокус: кастомные query методы, а не стандартный CRUD
 */
@DataJpaTest
@TestPropertySource(locations = "classpath:application-test.properties")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class RepositoryTests {

    // ========== COUNTRY REPOSITORY ==========

    @Nested
    @Sql("/test-data/countries.sql")
    @DisplayName("Country Repository")
    class CountryRepositoryTest {

        @Autowired
        private CountryRepository repository;

        @Test
        @DisplayName("finds active country on valid date")
        void shouldFindActiveCountry() {
            var country = repository.findActiveByIsoCode("ES", LocalDate.of(2024, 6, 15));

            assertTrue(country.isPresent());
            assertEquals("Spain", country.get().getNameEn());
        }

        @Test
        @DisplayName("rejects country before valid-from")
        void shouldRejectCountryBeforeValidFrom() {
            var country = repository.findActiveByIsoCode("ES", LocalDate.of(2019, 1, 1));

            assertTrue(country.isEmpty());
        }
    }

    // ========== MEDICAL LEVEL REPOSITORY ==========

    @Nested
    @Sql("/test-data/medical-risk-limit-levels.sql")
    @DisplayName("MedicalRiskLimitLevel Repository")
    class MedicalLevelRepositoryTest {

        @Autowired
        private MedicalRiskLimitLevelRepository repository;

        @Test
        @DisplayName("finds active level on valid date")
        void shouldFindActiveLevel() {
            var level = repository.findActiveByCode("10000", LocalDate.of(2024, 6, 15));

            assertTrue(level.isPresent());
            assertEquals("10000", level.get().getCode());
        }

        @Test
        @DisplayName("returns all active levels sorted by coverage")
        void shouldReturnActiveLevelsSorted() {
            var levels = repository.findAllActive();

            assertFalse(levels.isEmpty());

            // Check sorting
            for (int i = 1; i < levels.size(); i++) {
                assertTrue(
                        levels.get(i).getCoverageAmount()
                                .compareTo(levels.get(i-1).getCoverageAmount()) >= 0
                );
            }
        }
    }

    // ========== RISK TYPE REPOSITORY ==========

    @Nested
    @Sql("/test-data/risk-types.sql")
    @DisplayName("RiskType Repository")
    class RiskTypeRepositoryTest {

        @Autowired
        private RiskTypeRepository repository;

        @Test
        @DisplayName("finds active risk on valid date")
        void shouldFindActiveRisk() {
            var risk = repository.findActiveByCode("SPORT_ACTIVITIES", LocalDate.of(2024, 6, 15));

            assertTrue(risk.isPresent());
            assertFalse(risk.get().getIsMandatory());
        }

        @Test
        @DisplayName("separates mandatory from optional risks")
        void shouldSeparateMandatoryFromOptional() {
            var mandatory = repository.findAllMandatory();
            var optional = repository.findAllOptional();

            assertEquals(1, mandatory.size());
            assertTrue(mandatory.get(0).getIsMandatory());

            assertTrue(optional.size() >= 9);
            assertTrue(optional.stream().noneMatch(r -> r.getIsMandatory()));
        }
    }
}