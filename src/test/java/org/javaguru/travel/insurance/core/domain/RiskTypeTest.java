package org.javaguru.travel.insurance.core.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RiskType Tests")
class RiskTypeTest {

    @Nested
    @DisplayName("Enum Values - Basic Checks")
    class EnumValues {

        @Test
        @DisplayName("Should have at least 10 risk types")
        void shouldHaveAtLeast10RiskTypes() {
            RiskType[] types = RiskType.values();

            assertTrue(types.length >= 10,
                    "Should have at least 10 risk types");
        }

        @Test
        @DisplayName("All risk types should have non-null properties")
        void allRiskTypesShouldHaveNonNullProperties() {
            for (RiskType type : RiskType.values()) {
                assertAll("RiskType " + type.name() + " properties",
                        () -> assertNotNull(type.getCode(), "Code should not be null"),
                        () -> assertNotNull(type.getNameEn(), "English name should not be null"),
                        () -> assertNotNull(type.getNameRu(), "Russian name should not be null"),
                        () -> assertNotNull(type.getCoefficient(), "Coefficient should not be null")
                );
            }
        }

        @Test
        @DisplayName("Should have exactly one mandatory risk")
        void shouldHaveExactlyOneMandatoryRisk() {
            long mandatoryCount = Arrays.stream(RiskType.values())
                    .filter(RiskType::isMandatory)
                    .count();

            assertEquals(1, mandatoryCount,
                    "Should have exactly one mandatory risk (TRAVEL_MEDICAL)");
        }

        @Test
        @DisplayName("TRAVEL_MEDICAL should be the only mandatory risk")
        void travelMedicalShouldBeTheOnlyMandatoryRisk() {
            RiskType travelMedical = RiskType.TRAVEL_MEDICAL;

            assertTrue(travelMedical.isMandatory(),
                    "TRAVEL_MEDICAL should be mandatory");

            for (RiskType type : RiskType.values()) {
                if (type != RiskType.TRAVEL_MEDICAL) {
                    assertFalse(type.isMandatory(),
                            type.name() + " should not be mandatory");
                }
            }
        }

        @Test
        @DisplayName("All codes should be unique")
        void allCodesShouldBeUnique() {
            RiskType[] types = RiskType.values();
            long uniqueCodes = Arrays.stream(types)
                    .map(RiskType::getCode)
                    .distinct()
                    .count();

            assertEquals(types.length, uniqueCodes, "All codes should be unique");
        }
    }

    @Nested
    @DisplayName("Find By Code - Success Cases")
    class FindByCodeSuccess {

        @ParameterizedTest(name = "Code {0} should return {1}")
        @CsvSource({
                "TRAVEL_MEDICAL,        TRAVEL_MEDICAL",
                "SPORT_ACTIVITIES,      SPORT_ACTIVITIES",
                "EXTREME_SPORT,         EXTREME_SPORT",
                "PREGNANCY,             PREGNANCY",
                "CHRONIC_DISEASES,      CHRONIC_DISEASES",
                "ACCIDENT_COVERAGE,     ACCIDENT_COVERAGE",
                "TRIP_CANCELLATION,     TRIP_CANCELLATION",
                "LUGGAGE_LOSS,          LUGGAGE_LOSS",
                "FLIGHT_DELAY,          FLIGHT_DELAY",
                "CIVIL_LIABILITY,       CIVIL_LIABILITY"
        })
        @DisplayName("Should find risk type by valid code")
        void shouldFindRiskTypeByValidCode(String code, String expectedName) {
            RiskType type = RiskType.fromCode(code);

            assertNotNull(type);
            assertEquals(expectedName, type.name());
            assertEquals(code, type.getCode());
        }

        @Test
        @DisplayName("Should find TRAVEL_MEDICAL by code")
        void shouldFindTravelMedicalByCode() {
            RiskType type = RiskType.fromCode("TRAVEL_MEDICAL");

            assertAll(
                    () -> assertEquals(RiskType.TRAVEL_MEDICAL, type),
                    () -> assertEquals("TRAVEL_MEDICAL", type.getCode()),
                    () -> assertEquals("Medical Coverage", type.getNameEn()),
                    () -> assertTrue(type.isMandatory())
            );
        }

        @Test
        @DisplayName("Should find SPORT_ACTIVITIES by code")
        void shouldFindSportActivitiesByCode() {
            RiskType type = RiskType.fromCode("SPORT_ACTIVITIES");

            assertAll(
                    () -> assertEquals(RiskType.SPORT_ACTIVITIES, type),
                    () -> assertEquals("SPORT_ACTIVITIES", type.getCode()),
                    () -> assertEquals(new BigDecimal("0.3"), type.getCoefficient()),
                    () -> assertFalse(type.isMandatory())
            );
        }
    }

    @Nested
    @DisplayName("Find By Code - Error Cases")
    class FindByCodeError {

        @ParameterizedTest
        @ValueSource(strings = {"INVALID", "UNKNOWN", "medical", "sport"})
        @DisplayName("Should throw exception for invalid code")
        void shouldThrowExceptionForInvalidCode(String invalidCode) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> RiskType.fromCode(invalidCode)
            );

            assertTrue(exception.getMessage().contains(invalidCode),
                    "Exception message should contain the invalid code");
        }

        @Test
        @DisplayName("Should throw exception for null code")
        void shouldThrowExceptionForNullCode() {
            assertThrows(
                    Exception.class,
                    () -> RiskType.fromCode(null)
            );
        }

        @Test
        @DisplayName("Should throw exception for empty code")
        void shouldThrowExceptionForEmptyCode() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RiskType.fromCode("")
            );
        }
    }

    @Nested
    @DisplayName("Mandatory vs Optional Risks")
    class MandatoryVsOptional {

        @Test
        @DisplayName("Should get mandatory risks")
        void shouldGetMandatoryRisks() {
            RiskType[] mandatory = RiskType.getMandatoryRisks();

            assertEquals(1, mandatory.length);
            assertEquals(RiskType.TRAVEL_MEDICAL, mandatory[0]);
        }

        @Test
        @DisplayName("Should get optional risks")
        void shouldGetOptionalRisks() {
            RiskType[] optional = RiskType.getOptionalRisks();

            assertTrue(optional.length >= 9,
                    "Should have at least 9 optional risks");

            for (RiskType type : optional) {
                assertFalse(type.isMandatory(),
                        type.name() + " should be optional");
            }
        }

        @Test
        @DisplayName("Mandatory + Optional should equal total")
        void mandatoryPlusOptionalShouldEqualTotal() {
            int totalCount = RiskType.values().length;
            int mandatoryCount = RiskType.getMandatoryRisks().length;
            int optionalCount = RiskType.getOptionalRisks().length;

            assertEquals(totalCount, mandatoryCount + optionalCount,
                    "Mandatory + Optional should equal total count");
        }
    }

    @Nested
    @DisplayName("Coefficient Validation")
    class CoefficientValidation {

        @Test
        @DisplayName("Mandatory risk should have zero coefficient")
        void mandatoryRiskShouldHaveZeroCoefficient() {
            assertEquals(BigDecimal.ZERO, RiskType.TRAVEL_MEDICAL.getCoefficient(),
                    "TRAVEL_MEDICAL should have coefficient 0");
        }

        @Test
        @DisplayName("Optional risks should have positive coefficients")
        void optionalRisksShouldHavePositiveCoefficients() {
            for (RiskType type : RiskType.getOptionalRisks()) {
                assertTrue(type.getCoefficient().compareTo(BigDecimal.ZERO) > 0,
                        type.name() + " should have positive coefficient");
            }
        }

        @Test
        @DisplayName("Coefficients should be reasonable (between 0 and 1)")
        void coefficientsShouldBeReasonable() {
            BigDecimal min = BigDecimal.ZERO;
            BigDecimal max = BigDecimal.ONE;

            for (RiskType type : RiskType.values()) {
                assertTrue(
                        type.getCoefficient().compareTo(min) >= 0 &&
                                type.getCoefficient().compareTo(max) <= 0,
                        String.format("%s coefficient should be between 0 and 1", type.name())
                );
            }
        }

        @Test
        @DisplayName("EXTREME_SPORT should have highest coefficient")
        void extremeSportShouldHaveHighestCoefficient() {
            RiskType extremeSport = RiskType.EXTREME_SPORT;

            for (RiskType type : RiskType.values()) {
                assertTrue(
                        extremeSport.getCoefficient().compareTo(type.getCoefficient()) >= 0,
                        "EXTREME_SPORT should have highest or equal coefficient"
                );
            }
        }

        @Test
        @DisplayName("FLIGHT_DELAY should have lowest non-zero coefficient")
        void flightDelayShouldHaveLowestNonZeroCoefficient() {
            RiskType flightDelay = RiskType.FLIGHT_DELAY;

            for (RiskType type : RiskType.getOptionalRisks()) {
                assertTrue(
                        flightDelay.getCoefficient().compareTo(type.getCoefficient()) <= 0,
                        "FLIGHT_DELAY should have lowest coefficient among optional risks"
                );
            }
        }
    }

    @Nested
    @DisplayName("Specific Risk Type Properties")
    class SpecificRiskTypeProperties {

        @Test
        @DisplayName("TRAVEL_MEDICAL should have correct properties")
        void travelMedicalShouldHaveCorrectProperties() {
            RiskType type = RiskType.TRAVEL_MEDICAL;

            assertAll(
                    () -> assertEquals("TRAVEL_MEDICAL", type.getCode()),
                    () -> assertEquals("Medical Coverage", type.getNameEn()),
                    () -> assertEquals("Медицинское покрытие", type.getNameRu()),
                    () -> assertEquals(new BigDecimal("0"), type.getCoefficient()),
                    () -> assertTrue(type.isMandatory())
            );
        }

        @Test
        @DisplayName("SPORT_ACTIVITIES should have correct properties")
        void sportActivitiesShouldHaveCorrectProperties() {
            RiskType type = RiskType.SPORT_ACTIVITIES;

            assertAll(
                    () -> assertEquals("SPORT_ACTIVITIES", type.getCode()),
                    () -> assertEquals("Sport Activities", type.getNameEn()),
                    () -> assertEquals("Активный спорт", type.getNameRu()),
                    () -> assertEquals(new BigDecimal("0.3"), type.getCoefficient()),
                    () -> assertFalse(type.isMandatory())
            );
        }

        @Test
        @DisplayName("EXTREME_SPORT should have correct properties")
        void extremeSportShouldHaveCorrectProperties() {
            RiskType type = RiskType.EXTREME_SPORT;

            assertAll(
                    () -> assertEquals("EXTREME_SPORT", type.getCode()),
                    () -> assertEquals("Extreme Sport", type.getNameEn()),
                    () -> assertEquals("Экстремальный спорт", type.getNameRu()),
                    () -> assertEquals(new BigDecimal("0.6"), type.getCoefficient()),
                    () -> assertFalse(type.isMandatory())
            );
        }

        @Test
        @DisplayName("PREGNANCY should have correct properties")
        void pregnancyShouldHaveCorrectProperties() {
            RiskType type = RiskType.PREGNANCY;

            assertAll(
                    () -> assertEquals("PREGNANCY", type.getCode()),
                    () -> assertEquals("Pregnancy Coverage", type.getNameEn()),
                    () -> assertEquals(new BigDecimal("0.2"), type.getCoefficient()),
                    () -> assertFalse(type.isMandatory())
            );
        }

        @Test
        @DisplayName("CHRONIC_DISEASES should have correct properties")
        void chronicDiseasesShouldHaveCorrectProperties() {
            RiskType type = RiskType.CHRONIC_DISEASES;

            assertAll(
                    () -> assertEquals("CHRONIC_DISEASES", type.getCode()),
                    () -> assertEquals("Chronic Diseases", type.getNameEn()),
                    () -> assertEquals(new BigDecimal("0.4"), type.getCoefficient()),
                    () -> assertFalse(type.isMandatory())
            );
        }
    }

    @Nested
    @DisplayName("Business Logic Validation")
    class BusinessLogicValidation {

        @Test
        @DisplayName("Sport risks should be ordered by danger level")
        void sportRisksShouldBeOrderedByDangerLevel() {
            RiskType sport = RiskType.SPORT_ACTIVITIES;
            RiskType extremeSport = RiskType.EXTREME_SPORT;

            assertTrue(extremeSport.getCoefficient().compareTo(sport.getCoefficient()) > 0,
                    "EXTREME_SPORT should have higher coefficient than SPORT_ACTIVITIES");
        }

        @Test
        @DisplayName("High-risk activities should have higher coefficients")
        void highRiskActivitiesShouldHaveHigherCoefficients() {
            // Экстремальный спорт опаснее обычного спорта
            assertTrue(
                    RiskType.EXTREME_SPORT.getCoefficient()
                            .compareTo(RiskType.SPORT_ACTIVITIES.getCoefficient()) > 0
            );

            // Задержка рейса менее опасна чем потеря багажа
            assertTrue(
                    RiskType.LUGGAGE_LOSS.getCoefficient()
                            .compareTo(RiskType.FLIGHT_DELAY.getCoefficient()) >= 0
            );
        }

        @Test
        @DisplayName("Calculate combined coefficient for multiple risks")
        void calculateCombinedCoefficientForMultipleRisks() {
            // Сценарий: горнолыжный курорт + хронические заболевания
            BigDecimal combined = RiskType.SPORT_ACTIVITIES.getCoefficient()
                    .add(RiskType.CHRONIC_DISEASES.getCoefficient());

            assertEquals(new BigDecimal("0.7"), combined,
                    "Combined coefficient should be sum of individual coefficients");
        }

        @Test
        @DisplayName("Extreme scenario: all optional risks combined")
        void extremeScenarioAllOptionalRisksCombined() {
            BigDecimal total = BigDecimal.ZERO;

            for (RiskType type : RiskType.getOptionalRisks()) {
                total = total.add(type.getCoefficient());
            }

            assertTrue(total.compareTo(new BigDecimal("2.0")) > 0,
                    "All risks combined should significantly increase premium");
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenarios {

        @Test
        @DisplayName("Scenario: Standard vacation (medical only)")
        void scenarioStandardVacation() {
            // Только обязательное медицинское покрытие
            BigDecimal additionalCoefficient = RiskType.TRAVEL_MEDICAL.getCoefficient();

            assertEquals(BigDecimal.ZERO, additionalCoefficient,
                    "Standard vacation has no additional risks");
        }

        @Test
        @DisplayName("Scenario: Ski resort vacation")
        void scenarioSkiResortVacation() {
            // Медицинское + активный спорт
            BigDecimal additionalCoefficient = RiskType.SPORT_ACTIVITIES.getCoefficient();

            assertEquals(new BigDecimal("0.3"), additionalCoefficient,
                    "Ski resort adds 30% to premium");
        }

        @Test
        @DisplayName("Scenario: Extreme adventure trip")
        void scenarioExtremeAdventureTrip() {
            // Медицинское + экстремальный спорт
            BigDecimal additionalCoefficient = RiskType.EXTREME_SPORT.getCoefficient();

            assertEquals(new BigDecimal("0.6"), additionalCoefficient,
                    "Extreme adventure adds 60% to premium");
        }

        @Test
        @DisplayName("Scenario: Elderly traveler with health issues")
        void scenarioElderlyTravelerWithHealthIssues() {
            // Медицинское + хронические заболевания
            BigDecimal additionalCoefficient = RiskType.CHRONIC_DISEASES.getCoefficient();

            assertEquals(new BigDecimal("0.4"), additionalCoefficient,
                    "Chronic diseases add 40% to premium");
        }

        @Test
        @DisplayName("Scenario: Pregnant traveler")
        void scenarioPregnantTraveler() {
            // Медицинское + беременность
            BigDecimal additionalCoefficient = RiskType.PREGNANCY.getCoefficient();

            assertEquals(new BigDecimal("0.2"), additionalCoefficient,
                    "Pregnancy adds 20% to premium");
        }

        @Test
        @DisplayName("Scenario: Comprehensive coverage")
        void scenarioComprehensiveCoverage() {
            // Медицинское + несколько дополнительных рисков
            BigDecimal total = RiskType.SPORT_ACTIVITIES.getCoefficient()
                    .add(RiskType.TRIP_CANCELLATION.getCoefficient())
                    .add(RiskType.LUGGAGE_LOSS.getCoefficient());

            assertEquals(new BigDecimal("0.55"), total,
                    "Comprehensive coverage significantly increases premium");
        }

        @Test
        @DisplayName("Compare different risk combinations")
        void compareDifferentRiskCombinations() {
            // Сценарий A: Обычный спорт
            BigDecimal scenarioA = RiskType.SPORT_ACTIVITIES.getCoefficient();

            // Сценарий B: Экстремальный спорт
            BigDecimal scenarioB = RiskType.EXTREME_SPORT.getCoefficient();

            // Сценарий C: Спорт + хронические заболевания
            BigDecimal scenarioC = RiskType.SPORT_ACTIVITIES.getCoefficient()
                    .add(RiskType.CHRONIC_DISEASES.getCoefficient());

            assertAll(
                    () -> assertTrue(scenarioB.compareTo(scenarioA) > 0,
                            "Extreme sport should be more expensive than regular sport"),
                    () -> assertTrue(scenarioC.compareTo(scenarioA) > 0,
                            "Multiple risks should be more expensive than single risk"),
                    () -> assertTrue(scenarioC.compareTo(scenarioB) > 0,
                            "Sport + health should be most expensive")
            );
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCases {

        @Test
        @DisplayName("Code should be case sensitive")
        void codeShouldBeCaseSensitive() {
            assertDoesNotThrow(() -> RiskType.fromCode("SPORT_ACTIVITIES"));
            assertThrows(
                    IllegalArgumentException.class,
                    () -> RiskType.fromCode("sport_activities")
            );
        }

        @Test
        @DisplayName("Should handle all risks without duplicates")
        void shouldHandleAllRisksWithoutDuplicates() {
            RiskType[] all = RiskType.values();
            RiskType[] mandatory = RiskType.getMandatoryRisks();
            RiskType[] optional = RiskType.getOptionalRisks();

            // Проверка что нет пересечений
            for (RiskType m : mandatory) {
                for (RiskType o : optional) {
                    assertNotEquals(m, o, "Mandatory and optional should not overlap");
                }
            }
        }

        @Test
        @DisplayName("All non-mandatory risks should be in optional array")
        void allNonMandatoryRisksShouldBeInOptionalArray() {
            RiskType[] optional = RiskType.getOptionalRisks();

            for (RiskType type : RiskType.values()) {
                if (!type.isMandatory()) {
                    assertTrue(
                            Arrays.asList(optional).contains(type),
                            type.name() + " should be in optional risks"
                    );
                }
            }
        }
    }
}