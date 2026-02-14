-- ============================================
-- TEST DATA FOR E2E API INTEGRATION TESTS
-- ============================================
-- H2-compatible version with 2026 dates

-- ============================================
-- 1. AGE COEFFICIENTS
-- ============================================
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (0, 5, 1.10, 'Infants and toddlers', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (6, 17, 0.90, 'Children and teenagers', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (18, 30, 1.00, 'Young adults', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (31, 40, 1.10, 'Adults', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (41, 50, 1.30, 'Middle-aged', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (51, 60, 1.60, 'Senior', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (61, 70, 2.00, 'Elderly', '2026-01-01');
MERGE INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES (71, 80, 2.50, 'Very elderly', '2026-01-01');

-- ============================================
-- 2. MEDICAL RISK LIMIT LEVELS
-- ============================================
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('5000', 5000.00, 1.50, 'EUR', '2026-01-01');
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('10000', 10000.00, 2.00, 'EUR', '2026-01-01');
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('20000', 20000.00, 3.00, 'EUR', '2026-01-01');
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('50000', 50000.00, 4.50, 'EUR', '2026-01-01');
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('100000', 100000.00, 7.00, 'EUR', '2026-01-01');
MERGE INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from) VALUES ('200000', 200000.00, 12.00, 'EUR', '2026-01-01');

-- ============================================
-- 3. COUNTRIES
-- ============================================
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('ES', 'Spain', 'Испания', 'LOW', 1.0, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('FR', 'France', 'Франция', 'LOW', 1.0, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('DE', 'Germany', 'Германия', 'LOW', 1.0, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('IT', 'Italy', 'Италия', 'LOW', 1.0, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('AT', 'Austria', 'Австрия', 'LOW', 1.0, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('TR', 'Turkey', 'Турция', 'MEDIUM', 1.3, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('US', 'United States', 'США', 'MEDIUM', 1.3, '2026-01-01');
MERGE INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES ('EG', 'Egypt', 'Египет', 'HIGH', 1.8, '2026-01-01');

-- ============================================
-- 4. RISK TYPES
-- ============================================
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('TRAVEL_MEDICAL', 'Medical Coverage', 'Медицинское покрытие', 0.00, true, 'Base medical coverage', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('SPORT_ACTIVITIES', 'Sport Activities', 'Активный спорт', 0.30, false, 'Skiing, snowboarding, diving', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('EXTREME_SPORT', 'Extreme Sport', 'Экстремальный спорт', 0.60, false, 'Mountaineering, parachuting', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('PREGNANCY', 'Pregnancy Coverage', 'Покрытие беременности', 0.20, false, 'Up to 31 weeks', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('CHRONIC_DISEASES', 'Chronic Diseases', 'Хронические заболевания', 0.40, false, 'Diabetes, asthma, etc.', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('ACCIDENT_COVERAGE', 'Accident Coverage', 'От несчастных случаев', 0.20, false, 'Extended accident coverage', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('TRIP_CANCELLATION', 'Trip Cancellation', 'Отмена поездки', 0.15, false, 'Trip cancellation insurance', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('LUGGAGE_LOSS', 'Luggage Loss', 'Потеря багажа', 0.10, false, 'Lost luggage coverage', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('FLIGHT_DELAY', 'Flight Delay', 'Задержка рейса', 0.05, false, 'Flight delay compensation', '2026-01-01');
MERGE INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES ('CIVIL_LIABILITY', 'Civil Liability', 'Гражданская ответственность', 0.10, false, 'Third party liability', '2026-01-01');

-- ============================================
-- 5. PROMO CODES
-- ============================================
MERGE INTO promo_codes (code, description, discount_type, discount_value, min_premium_amount, max_discount_amount, valid_from, valid_to, max_usage_count, current_usage_count, is_active) VALUES ('SUMMER2026', 'Summer discount 10%', 'PERCENTAGE', 10, 50, 100, '2026-06-01', '2026-08-31', 1000, 0, true);
MERGE INTO promo_codes (code, description, discount_type, discount_value, min_premium_amount, max_discount_amount, valid_from, valid_to, max_usage_count, current_usage_count, is_active) VALUES ('WINTER2026', 'Winter discount 15%', 'PERCENTAGE', 15, 100, 200, '2025-12-01', '2026-02-28', 500, 0, true);
MERGE INTO promo_codes (code, description, discount_type, discount_value, min_premium_amount, max_discount_amount, valid_from, valid_to, max_usage_count, current_usage_count, is_active) VALUES ('WELCOME50', 'Welcome bonus 50 EUR', 'FIXED_AMOUNT', 50, 200, NULL, '2026-01-01', '2026-12-31', 100, 0, true);
MERGE INTO promo_codes (code, description, discount_type, discount_value, min_premium_amount, max_discount_amount, valid_from, valid_to, max_usage_count, current_usage_count, is_active) VALUES ('FAMILY20', 'Family discount 20%', 'PERCENTAGE', 20, 150, 300, '2026-01-01', '2026-12-31', NULL, 0, true);

-- ============================================
-- 6. TRIP DURATION COEFFICIENTS
-- ============================================
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (1, 7, 1.00, 'Short trip (1 week)', '2026-01-01');
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (8, 14, 0.95, 'Medium trip (2 weeks) - 5% discount', '2026-01-01');
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (15, 30, 0.90, 'Long trip (1 month) - 10% discount', '2026-01-01');
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (31, 60, 0.88, 'Extended trip (2 months) - 12% discount', '2026-01-01');
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (61, 90, 0.85, 'Very long trip (3 months) - 15% discount', '2026-01-01');
MERGE INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from) VALUES (91, 365, 0.82, 'Ultra long trip (3+ months) - 18% discount', '2026-01-01');

-- ============================================
-- 7. RISK BUNDLES
-- ============================================
MERGE INTO risk_bundles (code, name_en, name_ru, description, discount_percentage, required_risks, valid_from, is_active) VALUES ('ACTIVE_TRAVELER', 'Active Traveler Package', 'Пакет Активный путешественник', 'For sports enthusiasts', 15.00, '["SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"]', '2026-01-01', true);
MERGE INTO risk_bundles (code, name_en, name_ru, description, discount_percentage, required_risks, valid_from, is_active) VALUES ('FULL_PROTECTION', 'Full Protection Package', 'Пакет Полная защита', 'Complete coverage', 20.00, '["TRIP_CANCELLATION", "LUGGAGE_LOSS", "FLIGHT_DELAY"]', '2026-01-01', true);
MERGE INTO risk_bundles (code, name_en, name_ru, description, discount_percentage, required_risks, valid_from, is_active) VALUES ('EXTREME_ADVENTURE', 'Extreme Adventure', 'Экстремальное приключение', 'For adrenaline junkies', 18.00, '["EXTREME_SPORT", "ACCIDENT_COVERAGE", "CHRONIC_DISEASES"]', '2026-01-01', true);

-- ============================================
-- 8. AGE RISK COEFFICIENTS
-- ============================================
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('EXTREME_SPORT', 18, 35, 1.00, 'Standard rate for young adults', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('EXTREME_SPORT', 36, 50, 1.30, '+30% for middle-aged', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('EXTREME_SPORT', 51, 65, 1.80, '+80% for seniors', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('EXTREME_SPORT', 66, 80, 2.50, '+150% for elderly', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('SPORT_ACTIVITIES', 18, 50, 1.00, 'Standard rate', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('SPORT_ACTIVITIES', 51, 65, 1.20, '+20% for seniors', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('SPORT_ACTIVITIES', 66, 80, 1.50, '+50% for elderly', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('CHRONIC_DISEASES', 18, 45, 1.00, 'Standard rate', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('CHRONIC_DISEASES', 46, 60, 1.40, '+40% for middle-aged', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('CHRONIC_DISEASES', 61, 70, 1.80, '+80% for seniors', '2026-01-01');
MERGE INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from) VALUES ('CHRONIC_DISEASES', 71, 80, 2.50, '+150% for elderly', '2026-01-01');

-- ============================================
-- 9. UNDERWRITING RULES CONFIG
-- ============================================
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('AgeRule', 'MAX_AGE', '80', 'Maximum allowed age', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('AgeRule', 'REVIEW_AGE_THRESHOLD', '75', 'Age threshold for review', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('AdditionalRisksRule', 'MAX_AGE_FOR_EXTREME_SPORT', '70', 'Max age for extreme sport', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('AdditionalRisksRule', 'REVIEW_AGE_FOR_EXTREME_SPORT', '60', 'Review age for extreme sport', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('TripDurationRule', 'MAX_DAYS', '180', 'Maximum trip duration', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('TripDurationRule', 'REVIEW_DAYS_THRESHOLD', '90', 'Days requiring review', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('MedicalCoverageRule', 'REVIEW_AGE', '70', 'Age for medical review', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('MedicalCoverageRule', 'BLOCKING_AGE', '75', 'Age blocking high coverage', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('MedicalCoverageRule', 'REVIEW_COVERAGE_THRESHOLD', '100000', 'Coverage requiring review', '2026-01-01', true);
MERGE INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active) VALUES ('MedicalCoverageRule', 'BLOCKING_COVERAGE_THRESHOLD', '200000', 'Max coverage for elderly', '2026-01-01', true);