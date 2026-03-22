package org.javaguru.travel.insurance;

import org.javaguru.travel.insurance.application.dto.TravelCalculatePremiumRequest;

import java.util.List;

/**
 * Единый фабричный класс для создания тестовых запросов.
 *
 * ЭТАП 4 (рефакторинг): Устранение дублирования тестовых данных.
 *
 * КАК ПРИЛОЖЕНИЕ СЧИТАЕТ ДНИ:
 *   days = ChronoUnit.DAYS.between(agreementDateFrom, agreementDateTo)
 *   Т.е. конечный день НЕ включается:
 *     from=Apr17, to=Apr24 → days=7
 *     from=Apr17, to=May17 → days=30
 *   Формула для N дней: agreementDateTo = agreementDateFrom + N
 *
 * ПРЕСЕТЫ:
 *   adult35Spain()        — 35 лет, ES, LEVEL_50000, 14 дней
 *   young25Spain()        — 25 лет, ES, LEVEL_50000, 7 дней
 *   elderly77()           — 77 лет → REQUIRES_REVIEW
 *   tooOld85()            — 85 лет → VALIDATION_ERROR
 *   adult35Turkey()       — 35 лет, TR (MEDIUM risk)
 *   adult35Egypt()        — 35 лет, EG (HIGH risk) → REQUIRES_REVIEW
 *   adult35Afghanistan()  — 35 лет, AF (VERY_HIGH) → DECLINED
 *   adult35SpainLongTrip()— 35 лет, ES, 30 дней
 *   adult35SpainWithRisks(...)
 *   adult35SpainWithPromo(...)
 *   corporate35Spain()
 *   group10Spain()
 *
 * ПРОМО-КОДЫ (константы соответствуют test-data.sql):
 *   PROMO_10PCT, PROMO_15PCT, PROMO_FIXED50, PROMO_FAMILY_20PCT
 */
public final class TestRequestBuilder {

    // ── Опорная дата ──────────────────────────────────────────────────────────

    private static final java.time.LocalDate REF = TestConstants.TEST_DATE; // 2026-03-18

    // agreementDateFrom для всех пресетов
    private static final java.time.LocalDate DATE_FROM = REF.plusDays(30); // 2026-04-17

    // ── Константы промо-кодов ─────────────────────────────────────────────────

    /** 10% скидка, valid_to=2099-12-31 */
    public static final String PROMO_10PCT = "TEST_PROMO_10PCT";

    /** 15% скидка, valid_to=2099-12-31 */
    public static final String PROMO_15PCT = "TEST_PROMO_15PCT";

    /** Фиксированная скидка 50 EUR, valid_to=2099-12-31 */
    public static final String PROMO_FIXED50 = "TEST_PROMO_FIXED50";

    /** 20% семейная скидка, valid_to=2099-12-31 */
    public static final String PROMO_FAMILY_20PCT = "TEST_FAMILY_20PCT";

    // ── Конструктор ───────────────────────────────────────────────────────────

    private TestRequestBuilder() {
        throw new AssertionError("Utility class");
    }

    // ── Пресеты ───────────────────────────────────────────────────────────────

    /**
     * Взрослый 35 лет, Испания (LOW, coeff=1.0), уровень 50000, 14 дней.
     *
     * <pre>
     *   personBirthDate    = 1991-03-18  (REF - 35 лет)
     *   agreementDateFrom  = 2026-04-17  (REF + 30 дней)
     *   agreementDateTo    = 2026-05-01  (DATE_FROM + 14)  → days = 14
     *   ageCoefficient     = 1.1  (Adults: 31-40)
     *   durationCoefficient= 0.95 (8-14 дней)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35Spain() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Test")
                .personLastName("User")
                .personBirthDate(REF.minusYears(35))     // 1991-03-18
                .agreementDateFrom(DATE_FROM)             // 2026-04-17
                .agreementDateTo(DATE_FROM.plusDays(14))  // 2026-05-01  → days=14
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000");
    }

    /**
     * Молодой взрослый 25 лет, Испания, 7 дней.
     *
     * <pre>
     *   ageCoefficient     = 1.0  (Young adults: 18-30)
     *   durationCoefficient= 1.0  (1-7 дней)
     *   days = 7
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder young25Spain() {
        return TravelCalculatePremiumRequest.builder()
                .personFirstName("Test")
                .personLastName("User")
                .personBirthDate(REF.minusYears(25))     // 2001-03-18
                .agreementDateFrom(DATE_FROM)
                .agreementDateTo(DATE_FROM.plusDays(7))   // → days=7
                .countryIsoCode("ES")
                .medicalRiskLimitLevel("50000");
    }

    /**
     * Пожилой 77 лет → REQUIRES_REVIEW.
     *
     * <pre>
     *   age = 77 >= reviewThreshold 75
     *   ageCoefficient = 2.5 (Very elderly: 71-80)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder elderly77() {
        return adult35Spain()
                .personBirthDate(REF.minusYears(77));    // 1949-03-18
    }

    /**
     * Слишком пожилой 85 лет → VALIDATION_ERROR (age > 80).
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder tooOld85() {
        return adult35Spain()
                .personBirthDate(REF.minusYears(85));    // 1941-03-18
    }

    /**
     * 35 лет, Турция (MEDIUM risk, coeff=1.3), 14 дней.
     *
     * <pre>
     *   underwriting = APPROVED (WARNING для MEDIUM)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35Turkey() {
        return adult35Spain()
                .countryIsoCode("TR");
    }

    /**
     * 35 лет, Египет (HIGH risk, coeff=1.8), 14 дней.
     *
     * <pre>
     *   underwriting = REQUIRES_REVIEW (HIGH risk country)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35Egypt() {
        return adult35Spain()
                .countryIsoCode("EG");
    }

    /**
     * 35 лет, Афганистан (VERY_HIGH risk) → DECLINED.
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35Afghanistan() {
        return adult35Spain()
                .countryIsoCode("AF");
    }

    /**
     * 35 лет, Испания, 30 дней (durationCoefficient=0.90).
     *
     * <pre>
     *   agreementDateTo = DATE_FROM + 30  → days = 30
     *   durationCoefficient = 0.90 (15-30 дней)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35SpainLongTrip() {
        return adult35Spain()
                .agreementDateTo(DATE_FROM.plusDays(30)); // → days=30
    }

    /**
     * 35 лет, Испания с выбранными дополнительными рисками.
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35SpainWithRisks(
            String... riskCodes) {
        return adult35Spain()
                .selectedRisks(List.of(riskCodes));
    }

    /**
     * 35 лет, Испания с промо-кодом.
     *
     * <pre>
     *   Рекомендуется: adult35SpainWithPromo(TestRequestBuilder.PROMO_10PCT)
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder adult35SpainWithPromo(
            String promoCode) {
        return adult35Spain()
                .promoCode(promoCode);
    }

    /**
     * Корпоративный клиент, 35 лет, Испания.
     *
     * <pre>
     *   isCorporate = true → CORPORATE discount 20%
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder corporate35Spain() {
        return adult35Spain()
                .isCorporate(true);
    }

    /**
     * Групповой запрос (10 человек), 35 лет, Испания.
     *
     * <pre>
     *   personsCount = 10 → GROUP_10 discount 15%
     * </pre>
     */
    public static TravelCalculatePremiumRequest.TravelCalculatePremiumRequestBuilder group10Spain() {
        return adult35Spain()
                .personsCount(10);
    }
}