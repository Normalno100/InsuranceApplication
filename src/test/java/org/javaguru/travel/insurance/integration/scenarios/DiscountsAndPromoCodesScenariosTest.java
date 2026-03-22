package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.TestRequestBuilder;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты для сценариев со скидками и промо-кодами.
 *
 * ПРАВИЛО JSONPath:
 *   [?(@.field == 'value')] → возвращает JSONArray.
 *   Допустимо только с: .exists(), .doesNotExist(), .value(hasItem(...))
 *   Для числовых сравнений (greaterThan, value(X)) — прямой путь: $.array[N].field
 *
 * ПРОМО-КОДЫ: используем константы TestRequestBuilder.PROMO_*
 *   Промо-коды в test-data.sql не привязаны к году, valid_to=2099-12-31.
 *
 * ВАЖНО — min_premium_amount проверяется ПОСЛЕ bundle discount:
 *   TEST_PROMO_10PCT   — нет ограничений
 *   TEST_PROMO_15PCT   — нет ограничений
 *   TEST_PROMO_FIXED50 — min_premium = 200 EUR
 *       → нужна длинная поездка (30 дней) + уровень 200000
 *         12.0 × 1.1 × 1.0 × 0.9 × 30 = 356.40 EUR (без рисков → нет bundle discount)
 *   TEST_FAMILY_20PCT  — min_premium = 150 EUR
 *       → уровень 200000 + 14 дней = 175.56 EUR
 */
@DisplayName("E2E: Discounts and Promo Codes Scenarios")
class DiscountsAndPromoCodesScenariosTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Промо-код TEST_PROMO_15PCT — 15% скидка применена")
    void shouldApplyPromoCode_percentageDiscount() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithPromo(TestRequestBuilder.PROMO_15PCT)
                .medicalRiskLimitLevel("100000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath(
                        "$.appliedDiscounts[?(@.code == '" + TestRequestBuilder.PROMO_15PCT + "')]").exists())
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = new ObjectMapper().readTree(json);

        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();
        assertThat(total).isLessThan(base);

        JsonNode promoDiscount = findDiscountByCode(root.path("appliedDiscounts"), TestRequestBuilder.PROMO_15PCT);
        assertThat(promoDiscount).isNotNull();
        assertThat(promoDiscount.path("type").asText()).isEqualTo("PROMO_CODE");
        assertThat(promoDiscount.path("percentage").asDouble()).isEqualTo(15.0);
        assertThat(promoDiscount.path("amount").asDouble()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Промо-код TEST_PROMO_FIXED50 — фиксированная скидка применена")
    void shouldApplyPromoCode_fixedAmount() throws Exception {
        // TEST_PROMO_FIXED50 проверяет min_premium = 200 EUR ПОСЛЕ bundle discount.
        // Используем 30-дневную поездку без дополнительных рисков → нет bundle discount.
        // baseAmount = 12.0 × 1.1 × 1.0 × 0.9 × 30 = 356.40 EUR > 200 EUR → промо применится.
        var request = TestRequestBuilder.adult35SpainLongTrip()
                .medicalRiskLimitLevel("200000")
                .promoCode(TestRequestBuilder.PROMO_FIXED50)
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath(
                        "$.appliedDiscounts[?(@.code == '" + TestRequestBuilder.PROMO_FIXED50 + "')]").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        JsonNode promoDiscount = findDiscountByCode(root.path("appliedDiscounts"), TestRequestBuilder.PROMO_FIXED50);
        assertThat(promoDiscount).isNotNull();
        assertThat(promoDiscount.path("type").asText()).isEqualTo("PROMO_CODE");
        assertThat(promoDiscount.path("amount").asDouble()).isGreaterThan(0.0);
    }

    @Test
    @DisplayName("Промо-код TEST_PROMO_FIXED50 не применяется — базовая премия ниже минимума (200 EUR)")
    void shouldNotApplyPromoCode_whenPremiumBelowMinimum() throws Exception {
        // Уровень 50000, 14 дней → ~65.84 EUR < 200 EUR → промо-код отклоняется
        var request = TestRequestBuilder.adult35SpainWithPromo(TestRequestBuilder.PROMO_FIXED50)
                .medicalRiskLimitLevel("50000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath(
                        "$.appliedDiscounts[?(@.code == '" + TestRequestBuilder.PROMO_FIXED50 + "')]").doesNotExist())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Невалидный промо-код — игнорируется")
    void shouldIgnoreInvalidPromoCode() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithPromo("INVALID_CODE_XYZ").build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath(
                        "$.appliedDiscounts[?(@.code == 'INVALID_CODE_XYZ')]").doesNotExist())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Автоматическая скидка для группы из 5 человек")
    void shouldApplyGroupDiscount_5persons() throws Exception {
        var request = TestRequestBuilder.adult35Spain()
                .personsCount(5)
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts").isNotEmpty())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Групповая скидка 10 человек — GROUP_10 (15%)")
    void shouldApplyGroupDiscount_10persons() throws Exception {
        var request = TestRequestBuilder.group10Spain()
                .medicalRiskLimitLevel("100000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts[?(@.type == 'GROUP')]").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        JsonNode groupDiscount = findDiscountByType(root.path("appliedDiscounts"), "GROUP");
        assertThat(groupDiscount).isNotNull();
        assertThat(groupDiscount.path("percentage").asDouble()).isEqualTo(15.0);
    }

    @Test
    @DisplayName("Корпоративная скидка — CORPORATE 20%")
    void shouldApplyCorporateDiscount() throws Exception {
        var request = TestRequestBuilder.corporate35Spain()
                .medicalRiskLimitLevel("200000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts[?(@.type == 'CORPORATE')]").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        JsonNode corporateDiscount = findDiscountByType(root.path("appliedDiscounts"), "CORPORATE");
        assertThat(corporateDiscount).isNotNull();
        assertThat(corporateDiscount.path("percentage").asDouble()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("Пакетная скидка — покупка набора рисков снижает итоговую сумму")
    void shouldApplyBundleDiscount() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks(
                        "TRIP_CANCELLATION", "LUGGAGE_LOSS", "FLIGHT_DELAY")
                .medicalRiskLimitLevel("100000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.baseAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();
        assertThat(total).isLessThan(base);
    }

    @Test
    @DisplayName("Промо-код + корпоративный — оба применяются")
    void shouldApplyBothDiscounts_promoAndCorporate() throws Exception {
        var request = TestRequestBuilder.corporate35Spain()
                .promoCode(TestRequestBuilder.PROMO_10PCT)
                .medicalRiskLimitLevel("100000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.appliedDiscounts", hasSize(greaterThanOrEqualTo(2))))
                .andExpect(jsonPath("$.appliedDiscounts[?(@.code == '" + TestRequestBuilder.PROMO_10PCT + "')]").exists())
                .andExpect(jsonPath("$.appliedDiscounts[?(@.type == 'CORPORATE')]").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();
        assertThat(total).isLessThan(base);
    }

    @Test
    @DisplayName("Прогрессивная скидка за длительность — 30 дней, durationCoefficient=0.90")
    void shouldApplyDurationDiscount_30days() throws Exception {
        var request = TestRequestBuilder.adult35SpainLongTrip()
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.trip.days").value(30))
                .andExpect(jsonPath("$.pricingDetails.durationCoefficient").value(lessThan(1.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Промо-код TEST_FAMILY_20PCT — семейная скидка 20%")
    void shouldApplyFamilyPromoCode() throws Exception {
        // TEST_FAMILY_20PCT требует min_premium = 150 EUR.
        // Уровень 200000 + 14 дней: 12.0 × 1.1 × 1.0 × 0.95 × 14 = 175.56 EUR > 150 EUR.
        var request = TestRequestBuilder.adult35SpainWithPromo(TestRequestBuilder.PROMO_FAMILY_20PCT)
                .medicalRiskLimitLevel("200000")
                .personsCount(4)
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.totalDiscount").value(greaterThan(0.0)))
                .andExpect(jsonPath(
                        "$.appliedDiscounts[?(@.code == '" + TestRequestBuilder.PROMO_FAMILY_20PCT + "')]").exists())
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        JsonNode root = new ObjectMapper().readTree(result.getResponse().getContentAsString());
        JsonNode promoDiscount = findDiscountByCode(root.path("appliedDiscounts"), TestRequestBuilder.PROMO_FAMILY_20PCT);
        assertThat(promoDiscount).isNotNull();
        assertThat(promoDiscount.path("percentage").asDouble()).isEqualTo(20.0);
    }

    // ── Вспомогательные методы ────────────────────────────────────────────────

    private JsonNode findDiscountByCode(JsonNode discountsArray, String code) {
        for (JsonNode discount : discountsArray) {
            if (code.equals(discount.path("code").asText())) {
                return discount;
            }
        }
        return null;
    }

    private JsonNode findDiscountByType(JsonNode discountsArray, String type) {
        for (JsonNode discount : discountsArray) {
            if (type.equals(discount.path("type").asText())) {
                return discount;
            }
        }
        return null;
    }
}