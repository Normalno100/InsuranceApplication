package org.javaguru.travel.insurance.core.calculators;

import org.javaguru.travel.insurance.dto.TravelCalculatePremiumRequest;
import org.javaguru.travel.insurance.dto.TravelCalculatePremiumResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для проверки всех трех новых идей:
 * - Прогрессивная шкала длительности
 * - Пакеты рисков
 * - Возрастные коэффициенты рисков
 */
@SpringBootTest
class AdvancedPricingIntegrationTest {

    @Autowired
    private MedicalRiskPremiumCalculator premiumCalculator;

    /**
     * Тест Прогрессивная скидка за длительность
     */
    @Test
    void testDurationDiscounts() {
        // 7 дней - без скидки (коэфф 1.0)
        var request7days = buildRequest(7, List.of());
        var result7 = premiumCalculator.calculatePremiumWithDetails(request7days);

        // 15 дней - скидка 10% (коэфф 0.90)
        var request15days = buildRequest(15, List.of());
        var result15 = premiumCalculator.calculatePremiumWithDetails(request15days);

        // 90 дней - скидка 15% (коэфф 0.85)
        var request90days = buildRequest(90, List.of());
        var result90 = premiumCalculator.calculatePremiumWithDetails(request90days);

        // Проверяем коэффициенты
        assertEquals(new BigDecimal("1.00"), result7.durationCoefficient());
        assertEquals(new BigDecimal("0.90"), result15.durationCoefficient());
        assertEquals(new BigDecimal("0.85"), result90.durationCoefficient());

        System.out.println("\n=== ИДЕЯ #3: Duration Discounts ===");
        System.out.println("7 days:  coeff=" + result7.durationCoefficient()
                + ", premium=" + result7.premium());
        System.out.println("15 days: coeff=" + result15.durationCoefficient()
                + ", premium=" + result15.premium());
        System.out.println("90 days: coeff=" + result90.durationCoefficient()
                + ", premium=" + result90.premium());
    }

    /**
     * Тест Пакеты рисков
     */
    @Test
    void testRiskBundles() {
        // Заявка БЕЗ пакета
        var requestNone = buildRequest(14, List.of("LUGGAGE_LOSS"));
        var resultNone = premiumCalculator.calculatePremiumWithDetails(requestNone);

        // Заявка С пакетом "Active Traveler" (SPORT_ACTIVITIES + ACCIDENT_COVERAGE)
        var requestActive = buildRequest(14,
                List.of("SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"));
        var resultActive = premiumCalculator.calculatePremiumWithDetails(requestActive);

        // Заявка С пакетом "Full Protection" (TRIP_CANCELLATION + LUGGAGE_LOSS + FLIGHT_DELAY)
        var requestFull = buildRequest(14,
                List.of("TRIP_CANCELLATION", "LUGGAGE_LOSS", "FLIGHT_DELAY"));
        var resultFull = premiumCalculator.calculatePremiumWithDetails(requestFull);

        // Проверяем что пакетная скидка применена
        assertNull(resultNone.bundleDiscount().bundle());
        assertNotNull(resultActive.bundleDiscount().bundle());
        assertEquals("ACTIVE_TRAVELER", resultActive.bundleDiscount().bundle().code());

        assertNotNull(resultFull.bundleDiscount().bundle());
        assertEquals("FULL_PROTECTION", resultFull.bundleDiscount().bundle().code());

        System.out.println("\n=== ИДЕЯ #2: Risk Bundles ===");
        System.out.println("No bundle: premium=" + resultNone.premium());
        System.out.println("Active Traveler: bundle_discount="
                + resultActive.bundleDiscount().discountAmount()
                + ", premium=" + resultActive.premium());
        System.out.println("Full Protection: bundle_discount="
                + resultFull.bundleDiscount().discountAmount()
                + ", premium=" + resultFull.premium());
    }

    /**
     * Тест Возрастные коэффициенты рисков
     */
    @Test
    void testAgeRiskCoefficients() {
        // Молодой человек (25 лет) с EXTREME_SPORT
        var requestYoung = buildRequestWithAge(
                LocalDate.now().minusYears(25),
                14,
                List.of("EXTREME_SPORT")
        );
        var resultYoung = premiumCalculator.calculatePremiumWithDetails(requestYoung);

        // Средний возраст (45 лет) с EXTREME_SPORT
        var requestMiddle = buildRequestWithAge(
                LocalDate.now().minusYears(45),
                14,
                List.of("EXTREME_SPORT")
        );
        var resultMiddle = premiumCalculator.calculatePremiumWithDetails(requestMiddle);

        // Пожилой (65 лет) с EXTREME_SPORT
        var requestSenior = buildRequestWithAge(
                LocalDate.now().minusYears(65),
                14,
                List.of("EXTREME_SPORT")
        );
        var resultSenior = premiumCalculator.calculatePremiumWithDetails(requestSenior);

        // Находим детали по EXTREME_SPORT
        var extremeYoung = findRiskDetail(resultYoung, "EXTREME_SPORT");
        var extremeMiddle = findRiskDetail(resultMiddle, "EXTREME_SPORT");
        var extremeSenior = findRiskDetail(resultSenior, "EXTREME_SPORT");

        // Проверяем что возрастные модификаторы разные
        assertTrue(extremeMiddle.ageModifier().compareTo(extremeYoung.ageModifier()) > 0);
        assertTrue(extremeSenior.ageModifier().compareTo(extremeMiddle.ageModifier()) > 0);

        System.out.println("\n=== ИДЕЯ #5: Age-Risk Coefficients ===");
        System.out.println("EXTREME_SPORT at age 25: modifier=" + extremeYoung.ageModifier()
                + ", premium=" + extremeYoung.premium());
        System.out.println("EXTREME_SPORT at age 45: modifier=" + extremeMiddle.ageModifier()
                + ", premium=" + extremeMiddle.premium());
        System.out.println("EXTREME_SPORT at age 65: modifier=" + extremeSenior.ageModifier()
                + ", premium=" + extremeSenior.premium());
    }

    /**
     * Комплексный тест: все три идеи вместе
     */
    @Test
    void testAllFeaturesTogeth() {
        // Пожилой человек (68 лет), долгая поездка (60 дней), пакет "Active Traveler"
        var request = TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Senior")
                .personBirthDate(LocalDate.now().minusYears(68))
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(70))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(List.of("SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"))
                .build();

        var result = premiumCalculator.calculatePremiumWithDetails(request);

        // Проверяем все три фичи
        // должна быть скидка за длительность
        assertTrue(result.durationCoefficient().compareTo(BigDecimal.ONE) < 0);

        // должен применяться пакет "ACTIVE_TRAVELER"
        assertNotNull(result.bundleDiscount().bundle());
        assertEquals("ACTIVE_TRAVELER", result.bundleDiscount().bundle().code());

        // возрастные модификаторы должны быть > 1.0 для пожилого
        var sportDetail = findRiskDetail(result, "SPORT_ACTIVITIES");
        assertTrue(sportDetail.ageModifier().compareTo(BigDecimal.ONE) > 0);

        System.out.println("\n=== КОМПЛЕКСНЫЙ ТЕСТ: Все фичи вместе ===");
        System.out.println("Age: 68, Duration: 60 days");
        System.out.println("Duration coefficient: " + result.durationCoefficient());
        System.out.println("Bundle: " + result.bundleDiscount().bundle().code()
                + " (-" + result.bundleDiscount().discountAmount() + " EUR)");
        System.out.println("SPORT age modifier: " + sportDetail.ageModifier());
        System.out.println("FINAL PREMIUM: " + result.premium() + " EUR");
    }

    // ========== HELPER METHODS ==========

    private TravelCalculatePremiumRequest buildRequest(int days, List<String> risks) {
        return buildRequestWithAge(
                LocalDate.now().minusYears(30),
                days,
                risks
        );
    }

    private TravelCalculatePremiumRequest buildRequestWithAge(
            LocalDate birthDate,
            int days,
            List<String> risks) {

        return TravelCalculatePremiumRequest.builder()
                .personFirstName("John")
                .personLastName("Doe")
                .personBirthDate(birthDate)
                .agreementDateFrom(LocalDate.now().plusDays(10))
                .agreementDateTo(LocalDate.now().plusDays(10 + days))
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("10000")
                .selectedRisks(risks)
                .build();
    }

    private MedicalRiskPremiumCalculator.RiskPremiumDetail findRiskDetail(
            MedicalRiskPremiumCalculator.PremiumCalculationResult result,
            String riskCode) {

        return result.riskDetails().stream()
                .filter(d -> d.riskCode().equals(riskCode))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Risk not found: " + riskCode));
    }
}