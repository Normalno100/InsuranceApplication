package org.javaguru.travel.insurance.integration.scenarios;

import org.javaguru.travel.insurance.TestConstants;
import org.javaguru.travel.insurance.TestRequestBuilder;
import org.javaguru.travel.insurance.integration.BaseIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.JsonNode;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * E2E тесты для сценариев с дополнительными рисками.
 *
 * ПРАВИЛО JSONPath:
 *   [?(@.field == 'value')] → возвращает JSONArray.
 *   Допустимо только с: .exists(), .doesNotExist(), .value(hasItem(...))
 *   Для числовых сравнений (greaterThan, value(X)) — прямой путь: $.array[N].field
 */
@DisplayName("E2E: Additional Risks Scenarios")
class AdditionalRisksScenariosTest extends BaseIntegrationTest {

    private static final java.time.LocalDate REF = TestConstants.TEST_DATE;

    @Test
    @DisplayName("Один дополнительный риск - Активный спорт")
    void shouldCalculatePremium_withSportActivities() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks("SPORT_ACTIVITIES")
                .countryIsoCode("AT")
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(1)))
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("SPORT_ACTIVITIES"))
                // riskBreakdown[0] = TRAVEL_MEDICAL (обязательный), [1] = SPORT_ACTIVITIES
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(2)))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].premium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Несколько дополнительных рисков")
    void shouldCalculatePremium_withMultipleRisks() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks(
                        "SPORT_ACTIVITIES", "LUGGAGE_LOSS", "FLIGHT_DELAY", "TRIP_CANCELLATION")
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(4)))
                // [0] = TRAVEL_MEDICAL + 4 дополнительных = 5 итого
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(5)))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[0].riskCode").value("TRAVEL_MEDICAL"))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(50.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Экстремальный спорт - молодой возраст (одобрено)")
    void shouldCalculatePremium_withExtremeSport_youngAge() throws Exception {
        var request = TestRequestBuilder.young25Spain()
                .countryIsoCode("CH")
                .medicalRiskLimitLevel("200000")
                .selectedRisks(List.of("EXTREME_SPORT"))
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks[0]").value("EXTREME_SPORT"))
                // [0] = TRAVEL_MEDICAL, [1] = EXTREME_SPORT
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("EXTREME_SPORT"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].premium").value(greaterThan(20.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Экстремальный спорт - 65 лет (требует проверки)")
    void shouldRequireReview_withExtremeSport_middleAge() throws Exception {
        // 65 лет >= reviewAge 60 → REQUIRES_REVIEW
        var request = TestRequestBuilder.adult35SpainWithRisks("EXTREME_SPORT")
                .personBirthDate(REF.minusYears(65))
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("REQUIRES_REVIEW"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("REQUIRES_MANUAL_REVIEW"))
                .andExpect(jsonPath("$.underwriting.reason", containsString("Extreme sport")));
    }

    @Test
    @DisplayName("Экстремальный спорт - старше 70 лет (отклонено)")
    void shouldDecline_withExtremeSport_oldAge() throws Exception {
        // 72 года > maxAge 70 для EXTREME_SPORT → DECLINED
        var request = TestRequestBuilder.adult35SpainWithRisks("EXTREME_SPORT")
                .personBirthDate(REF.minusYears(72))
                .medicalRiskLimitLevel("50000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value("DECLINED"))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.underwriting.decision").value("DECLINED"))
                .andExpect(jsonPath("$.underwriting.reason", containsString("Extreme sport")))
                .andExpect(jsonPath("$.errors", hasSize(1)));
    }

    @Test
    @DisplayName("Хронические заболевания - baseCoefficient=0.4")
    void shouldCalculatePremium_withChronicDiseases() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks("CHRONIC_DISEASES")
                .countryIsoCode("DE")
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // [0] = TRAVEL_MEDICAL, [1] = CHRONIC_DISEASES
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("CHRONIC_DISEASES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].baseCoefficient").value(0.4))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].premium").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Пакет рисков ACTIVE_TRAVELER — итоговая премия меньше базовой")
    void shouldApplyBundleDiscount_forRiskPackage() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks("SPORT_ACTIVITIES", "ACCIDENT_COVERAGE")
                .medicalRiskLimitLevel("100000")
                .build();

        MvcResult result = performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.baseAmount").value(greaterThan(0.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"))
                .andReturn();

        String json = result.getResponse().getContentAsString();
        JsonNode root = new ObjectMapper().readTree(json);

        double base = root.path("pricing").path("baseAmount").asDouble();
        double total = root.path("pricing").path("totalPremium").asDouble();

        assertThat(total).isLessThan(base);
    }

    @Test
    @DisplayName("Все доступные риски — 7 элементов в riskBreakdown")
    void shouldCalculatePremium_withAllRisks() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks(
                        "SPORT_ACTIVITIES", "ACCIDENT_COVERAGE", "TRIP_CANCELLATION",
                        "LUGGAGE_LOSS", "FLIGHT_DELAY", "CIVIL_LIABILITY")
                .medicalRiskLimitLevel("200000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.pricing.includedRisks", hasSize(6)))
                // [0] TRAVEL_MEDICAL + 6 дополнительных = 7
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown", hasSize(7)))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[0].riskCode").value("TRAVEL_MEDICAL"))
                .andExpect(jsonPath("$.pricing.totalPremium").value(greaterThan(100.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Возрастной модификатор — спорт для 66+ лет, ageModifier > 1.0")
    void shouldApplyAgeModifier_forSportRisk() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks("SPORT_ACTIVITIES")
                .personBirthDate(REF.minusYears(67))
                .medicalRiskLimitLevel("100000")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                // [0] = TRAVEL_MEDICAL, [1] = SPORT_ACTIVITIES
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].riskCode").value("SPORT_ACTIVITIES"))
                .andExpect(jsonPath("$.pricingDetails.riskBreakdown[1].ageModifier").value(greaterThan(1.0)))
                .andExpect(jsonPath("$.underwriting.decision").value("APPROVED"));
    }

    @Test
    @DisplayName("Обязательный риск TRAVEL_MEDICAL в selectedRisks — ошибка валидации")
    void shouldReject_mandatoryRiskInSelectedRisks() throws Exception {
        var request = TestRequestBuilder.adult35SpainWithRisks("TRAVEL_MEDICAL", "LUGGAGE_LOSS")
                .build();

        performCalculatePremium(request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("selectedRisks"))
                .andExpect(jsonPath("$.errors[0].message", containsString("mandatory")));
    }
}