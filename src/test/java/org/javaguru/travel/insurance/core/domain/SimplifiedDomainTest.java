package org.javaguru.travel.insurance.core.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Упрощённые тесты domain enum'ов - проверяем только бизнес-логику
 */
class SimplifiedDomainTest {

    // ========== COUNTRY ==========

    @ParameterizedTest
    @CsvSource({
            "ES, SPAIN,  1.0",
            "TH, THAILAND, 1.3",
            "IN, INDIA,  1.8",
            "AF, AFGHANISTAN, 2.5"
    })
    void shouldFindCountryByCode(String code, String name, String coeff) {
        var country = Country.fromIsoCode(code);

        assertThat(country.name()).isEqualTo(name);
        assertThat(country.getRiskCoefficient())
                .isEqualByComparingTo(coeff);
    }

    @Test
    void shouldFindCountry_caseInsensitive() {
        assertThat(Country.fromIsoCode("es")).isEqualTo(Country.SPAIN);
        assertThat(Country.fromIsoCode("ES")).isEqualTo(Country.SPAIN);
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "ZZ", "INVALID"})
    void shouldFailForInvalidCountry(String code) {
        assertThatThrownBy(() -> Country.fromIsoCode(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== MEDICAL RISK LIMIT LEVEL ==========

    @ParameterizedTest
    @CsvSource({
            "5000,   LEVEL_5000,   1.50",
            "50000,  LEVEL_50000,  4.50",
            "100000, LEVEL_100000, 7.00",
            "500000, LEVEL_500000, 20.00"
    })
    void shouldFindMedicalLevelByCode(String code, String name, String rate) {
        var level = MedicalRiskLimitLevel.fromCode(code);

        assertThat(level.name()).isEqualTo(name);
        assertThat(level.getDailyRate()).isEqualByComparingTo(rate);
    }

    @ParameterizedTest
    @CsvSource({
            "1000,   LEVEL_5000",   // Ниже минимума
            "7000,   LEVEL_10000",  // Между уровнями
            "45000,  LEVEL_50000",  // Точно на границе
            "600000, LEVEL_500000"  // Выше максимума
    })
    void shouldFindLevelByAmount(String amount, String expected) {
        var level = MedicalRiskLimitLevel.findByAmount(new BigDecimal(amount));

        assertThat(level.name()).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "999999", "INVALID"})
    void shouldFailForInvalidLevel(String code) {
        assertThatThrownBy(() -> MedicalRiskLimitLevel.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ========== RISK TYPE ==========

    @Test
    void shouldHaveOneMandatoryRisk() {
        var mandatory = RiskType.TRAVEL_MEDICAL;

        assertThat(mandatory.isMandatory()).isTrue();
        assertThat(mandatory.getCoefficient()).isEqualByComparingTo("0");
    }

    @ParameterizedTest
    @CsvSource({
            "SPORT_ACTIVITIES, 0.3",
            "EXTREME_SPORT,    0.6",
            "PREGNANCY,        0.2",
            "CHRONIC_DISEASES, 0.4"
    })
    void shouldFindRiskByCode(String code, String coeff) {
        var risk = RiskType.fromCode(code);

        assertThat(risk.getCode()).isEqualTo(code);
        assertThat(risk.getCoefficient()).isEqualByComparingTo(coeff);
        assertThat(risk.isMandatory()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "UNKNOWN", "medical"})
    void shouldFailForInvalidRisk(String code) {
        assertThatThrownBy(() -> RiskType.fromCode(code))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldOrderRisksBySeverity() {
        assertThat(RiskType.EXTREME_SPORT.getCoefficient())
                .isGreaterThan(RiskType.SPORT_ACTIVITIES.getCoefficient());
        assertThat(RiskType.CHRONIC_DISEASES.getCoefficient())
                .isGreaterThan(RiskType.PREGNANCY.getCoefficient());
    }
}