package org.javaguru.travel.insurance.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Country Tests")
class CountryTest {

    @Nested
    @DisplayName("Enum Values - Basic Checks")
    class EnumValues {

        @Test
        @DisplayName("Should have countries from all risk levels")
        void shouldHaveCountriesFromAllRiskLevels() {
            Country[] countries = Country.values();

            assertTrue(countries.length > 0, "Should have at least some countries");

            // Проверяем наличие всех уровней риска по коэффициентам
            Set<BigDecimal> coefficients = Arrays.stream(countries)
                    .map(Country::getRiskCoefficient)
                    .collect(Collectors.toSet());

            assertTrue(coefficients.contains(new BigDecimal("1.0")),
                    "Should have LOW risk countries (1.0)");
            assertTrue(coefficients.contains(new BigDecimal("1.3")),
                    "Should have MEDIUM risk countries (1.3)");
            assertTrue(coefficients.contains(new BigDecimal("1.8")),
                    "Should have HIGH risk countries (1.8)");
            assertTrue(coefficients.contains(new BigDecimal("2.5")),
                    "Should have VERY_HIGH risk countries (2.5)");
        }

        @Test
        @DisplayName("All countries should have non-null properties")
        void allCountriesShouldHaveNonNullProperties() {
            for (Country country : Country.values()) {
                assertAll("Country " + country.name() + " properties",
                        () -> assertNotNull(country.getIsoCode(), "ISO code should not be null"),
                        () -> assertNotNull(country.getNameEn(), "English name should not be null"),
                        () -> assertNotNull(country.getNameRu(), "Russian name should not be null"),
                        () -> assertNotNull(country.getRiskCoefficient(), "Risk coefficient should not be null")
                );
            }
        }

        @Test
        @DisplayName("ISO codes should be exactly 2 characters")
        void isoCodesShouldBe2Characters() {
            for (Country country : Country.values()) {
                assertEquals(2, country.getIsoCode().length(),
                        String.format("ISO code of %s should be 2 characters", country.name()));
            }
        }

        @Test
        @DisplayName("ISO codes should be uppercase")
        void isoCodesShouldBeUppercase() {
            for (Country country : Country.values()) {
                assertEquals(country.getIsoCode().toUpperCase(), country.getIsoCode(),
                        String.format("ISO code of %s should be uppercase", country.name()));
            }
        }

        @Test
        @DisplayName("All ISO codes should be unique")
        void allIsoCodesShouldBeUnique() {
            Country[] countries = Country.values();
            long uniqueIsoCodes = Arrays.stream(countries)
                    .map(Country::getIsoCode)
                    .distinct()
                    .count();

            assertEquals(countries.length, uniqueIsoCodes, "All ISO codes should be unique");
        }
    }

    @Nested
    @DisplayName("Find By ISO Code - Success Cases")
    class FindByIsoCodeSuccess {

        @ParameterizedTest(name = "ISO code {0} should return {1}")
        @CsvSource({
                "ES, SPAIN",
                "DE, GERMANY",
                "FR, FRANCE",
                "IT, ITALY",
                "AT, AUSTRIA",
                "TH, THAILAND",
                "VN, VIETNAM",
                "TR, TURKEY",
                "US, USA",
                "IN, INDIA",
                "EG, EGYPT"
        })
        @DisplayName("Should find country by valid ISO code")
        void shouldFindCountryByValidIsoCode(String isoCode, String expectedName) {
            Country country = Country.fromIsoCode(isoCode);

            assertNotNull(country);
            assertEquals(expectedName, country.name());
            assertEquals(isoCode, country.getIsoCode());
        }

        @Test
        @DisplayName("Should find Spain by ISO code")
        void shouldFindSpainByIsoCode() {
            Country country = Country.fromIsoCode("ES");

            assertAll(
                    () -> assertEquals(Country.SPAIN, country),
                    () -> assertEquals("ES", country.getIsoCode()),
                    () -> assertEquals("Spain", country.getNameEn()),
                    () -> assertEquals("Испания", country.getNameRu())
            );
        }

        @Test
        @DisplayName("Should handle case-insensitive ISO code")
        void shouldHandleCaseInsensitiveIsoCode() {
            assertAll(
                    () -> assertEquals(Country.SPAIN, Country.fromIsoCode("ES")),
                    () -> assertEquals(Country.SPAIN, Country.fromIsoCode("es")),
                    () -> assertEquals(Country.SPAIN, Country.fromIsoCode("Es"))
            );
        }
    }

    @Nested
    @DisplayName("Find By ISO Code - Error Cases")
    class FindByIsoCodeError {

        @ParameterizedTest
        @ValueSource(strings = {"XX", "ZZ", "ABC", "E", "ESP", "invalid"})
        @DisplayName("Should throw exception for invalid ISO code")
        void shouldThrowExceptionForInvalidIsoCode(String invalidCode) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> Country.fromIsoCode(invalidCode)
            );

            assertTrue(exception.getMessage().contains(invalidCode),
                    "Exception message should contain the invalid code");
        }

        @Test
        @DisplayName("Should throw exception for null ISO code")
        void shouldThrowExceptionForNullIsoCode() {
            assertThrows(
                    Exception.class,
                    () -> Country.fromIsoCode(null)
            );
        }

        @Test
        @DisplayName("Should throw exception for empty ISO code")
        void shouldThrowExceptionForEmptyIsoCode() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Country.fromIsoCode("")
            );
        }
    }

    @Nested
    @DisplayName("Risk Coefficients")
    class RiskCoefficients {

        @Test
        @DisplayName("LOW risk countries should have coefficient 1.0")
        void lowRiskCountriesShouldHaveCoefficient1() {
            Country[] lowRiskCountries = {
                    Country.SPAIN, Country.GERMANY, Country.FRANCE, Country.ITALY
            };

            for (Country country : lowRiskCountries) {
                assertEquals(new BigDecimal("1.0"), country.getRiskCoefficient(),
                        String.format("%s should have coefficient 1.0", country.name()));
            }
        }

        @Test
        @DisplayName("MEDIUM risk countries should have coefficient 1.3")
        void mediumRiskCountriesShouldHaveCoefficient1_3() {
            Country[] mediumRiskCountries = {
                    Country.THAILAND, Country.TURKEY, Country.USA, Country.CHINA
            };

            for (Country country : mediumRiskCountries) {
                assertEquals(new BigDecimal("1.3"), country.getRiskCoefficient(),
                        String.format("%s should have coefficient 1.3", country.name()));
            }
        }

        @Test
        @DisplayName("HIGH risk countries should have coefficient 1.8")
        void highRiskCountriesShouldHaveCoefficient1_8() {
            Country[] highRiskCountries = {
                    Country.INDIA, Country.EGYPT, Country.KENYA, Country.SOUTH_AFRICA
            };

            for (Country country : highRiskCountries) {
                assertEquals(new BigDecimal("1.8"), country.getRiskCoefficient(),
                        String.format("%s should have coefficient 1.8", country.name()));
            }
        }

        @Test
        @DisplayName("VERY_HIGH risk countries should have coefficient 2.5")
        void veryHighRiskCountriesShouldHaveCoefficient2_5() {
            Country[] veryHighRiskCountries = {
                    Country.AFGHANISTAN, Country.IRAQ, Country.SYRIA, Country.YEMEN
            };

            for (Country country : veryHighRiskCountries) {
                assertEquals(new BigDecimal("2.5"), country.getRiskCoefficient(),
                        String.format("%s should have coefficient 2.5", country.name()));
            }
        }

        @Test
        @DisplayName("Risk coefficients should be positive")
        void riskCoefficientsShouldBePositive() {
            for (Country country : Country.values()) {
                assertTrue(country.getRiskCoefficient().compareTo(BigDecimal.ZERO) > 0,
                        String.format("Risk coefficient of %s should be positive", country.name()));
            }
        }

        @Test
        @DisplayName("Risk coefficients should be reasonable (between 0.5 and 5.0)")
        void riskCoefficientsShouldBeReasonable() {
            BigDecimal min = new BigDecimal("0.5");
            BigDecimal max = new BigDecimal("5.0");

            for (Country country : Country.values()) {
                assertTrue(
                        country.getRiskCoefficient().compareTo(min) >= 0 &&
                                country.getRiskCoefficient().compareTo(max) <= 0,
                        String.format("Risk coefficient of %s should be between 0.5 and 5.0", country.name())
                );
            }
        }
    }

    @Nested
    @DisplayName("Specific Country Properties")
    class SpecificCountryProperties {

        @Test
        @DisplayName("Spain should have correct properties")
        void spainShouldHaveCorrectProperties() {
            Country spain = Country.SPAIN;

            assertAll(
                    () -> assertEquals("ES", spain.getIsoCode()),
                    () -> assertEquals("Spain", spain.getNameEn()),
                    () -> assertEquals("Испания", spain.getNameRu()),
                    () -> assertEquals(new BigDecimal("1.0"), spain.getRiskCoefficient())
            );
        }

        @Test
        @DisplayName("Thailand should have correct properties")
        void thailandShouldHaveCorrectProperties() {
            Country thailand = Country.THAILAND;

            assertAll(
                    () -> assertEquals("TH", thailand.getIsoCode()),
                    () -> assertEquals("Thailand", thailand.getNameEn()),
                    () -> assertEquals("Таиланд", thailand.getNameRu()),
                    () -> assertEquals(new BigDecimal("1.3"), thailand.getRiskCoefficient())
            );
        }

        @Test
        @DisplayName("USA should have correct properties")
        void usaShouldHaveCorrectProperties() {
            Country usa = Country.USA;

            assertAll(
                    () -> assertEquals("US", usa.getIsoCode()),
                    () -> assertEquals("United States", usa.getNameEn()),
                    () -> assertEquals("США", usa.getNameRu()),
                    () -> assertEquals(new BigDecimal("1.3"), usa.getRiskCoefficient())
            );
        }

        @Test
        @DisplayName("India should have correct properties")
        void indiaShouldHaveCorrectProperties() {
            Country india = Country.INDIA;

            assertAll(
                    () -> assertEquals("IN", india.getIsoCode()),
                    () -> assertEquals("India", india.getNameEn()),
                    () -> assertEquals("Индия", india.getNameRu()),
                    () -> assertEquals(new BigDecimal("1.8"), india.getRiskCoefficient())
            );
        }

        @Test
        @DisplayName("Afghanistan should have correct properties")
        void afghanistanShouldHaveCorrectProperties() {
            Country afghanistan = Country.AFGHANISTAN;

            assertAll(
                    () -> assertEquals("AF", afghanistan.getIsoCode()),
                    () -> assertEquals("Afghanistan", afghanistan.getNameEn()),
                    () -> assertEquals("Афганистан", afghanistan.getNameRu()),
                    () -> assertEquals(new BigDecimal("2.5"), afghanistan.getRiskCoefficient())
            );
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicValidation {

        @Test
        @DisplayName("European countries should have LOW coefficient")
        void europeanCountriesShouldHaveLowCoefficient() {
            Country[] europeanCountries = {
                    Country.SPAIN, Country.GERMANY, Country.FRANCE,
                    Country.ITALY, Country.AUSTRIA
            };

            for (Country country : europeanCountries) {
                assertEquals(new BigDecimal("1.0"), country.getRiskCoefficient(),
                        String.format("%s should have LOW risk coefficient", country.name()));
            }
        }

        @Test
        @DisplayName("War zone countries should have VERY_HIGH coefficient")
        void warZoneCountriesShouldHaveVeryHighCoefficient() {
            Country[] warZones = {
                    Country.AFGHANISTAN, Country.IRAQ, Country.SYRIA, Country.YEMEN
            };

            for (Country country : warZones) {
                assertEquals(new BigDecimal("2.5"), country.getRiskCoefficient(),
                        String.format("%s should have VERY_HIGH risk coefficient", country.name()));
            }
        }

        @Test
        @DisplayName("Should have popular tourist destinations")
        void shouldHavePopularTouristDestinations() {
            String[] popularDestinations = {
                    "SPAIN", "FRANCE", "ITALY", "THAILAND", "USA", "UAE"
            };

            for (String destination : popularDestinations) {
                assertDoesNotThrow(
                        () -> Country.valueOf(destination),
                        String.format("Should have popular destination %s", destination)
                );
            }
        }

        @Test
        @DisplayName("Higher risk should result in higher coefficient")
        void higherRiskShouldResultInHigherCoefficient() {
            Country lowRisk = Country.SPAIN;           // 1.0
            Country mediumRisk = Country.THAILAND;     // 1.3
            Country highRisk = Country.INDIA;          // 1.8
            Country veryHighRisk = Country.AFGHANISTAN; // 2.5

            assertTrue(
                    lowRisk.getRiskCoefficient().compareTo(mediumRisk.getRiskCoefficient()) < 0 &&
                            mediumRisk.getRiskCoefficient().compareTo(highRisk.getRiskCoefficient()) < 0 &&
                            highRisk.getRiskCoefficient().compareTo(veryHighRisk.getRiskCoefficient()) < 0,
                    "Risk coefficients should increase with risk level"
            );
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Scenario: Beach vacation in Spain")
        void scenarioBeachVacationInSpain() {
            Country spain = Country.fromIsoCode("ES");

            assertEquals(Country.SPAIN, spain);
            assertEquals(new BigDecimal("1.0"), spain.getRiskCoefficient());
            // Lowest risk = lowest additional cost
        }

        @Test
        @DisplayName("Scenario: Exotic trip to Thailand")
        void scenarioExoticTripToThailand() {
            Country thailand = Country.fromIsoCode("TH");

            assertEquals(Country.THAILAND, thailand);
            assertEquals(new BigDecimal("1.3"), thailand.getRiskCoefficient());
            // 30% higher cost than Europe
        }

        @Test
        @DisplayName("Scenario: Adventure in India")
        void scenarioAdventureInIndia() {
            Country india = Country.fromIsoCode("IN");

            assertEquals(Country.INDIA, india);
            assertEquals(new BigDecimal("1.8"), india.getRiskCoefficient());
            // 80% higher cost than Europe
        }

        @Test
        @DisplayName("Scenario: Business trip to USA")
        void scenarioBusinessTripToUSA() {
            Country usa = Country.fromIsoCode("US");

            assertEquals(Country.USA, usa);
            assertEquals(new BigDecimal("1.3"), usa.getRiskCoefficient());
            // MEDIUM risk due to expensive healthcare
        }

        @Test
        @DisplayName("Compare costs for different destinations")
        void compareCostsForDifferentDestinations() {
            BigDecimal baseCost = new BigDecimal("100");

            Country spain = Country.SPAIN;
            Country thailand = Country.THAILAND;
            Country india = Country.INDIA;

            BigDecimal spainCost = baseCost.multiply(spain.getRiskCoefficient());
            BigDecimal thailandCost = baseCost.multiply(thailand.getRiskCoefficient());
            BigDecimal indiaCost = baseCost.multiply(india.getRiskCoefficient());

            assertAll(
                    () -> assertEquals(new BigDecimal("100.0"), spainCost),
                    () -> assertEquals(new BigDecimal("130.0"), thailandCost),
                    () -> assertEquals(new BigDecimal("180.0"), indiaCost),
                    () -> assertTrue(spainCost.compareTo(thailandCost) < 0),
                    () -> assertTrue(thailandCost.compareTo(indiaCost) < 0)
            );
        }
    }
}