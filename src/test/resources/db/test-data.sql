-- ============================================
-- TEST DATA FOR E2E API INTEGRATION TESTS
-- ============================================
-- H2-compatible version with INSERT statements
-- Uses exact table names from JPA entities

-- Note: This script runs AFTER JPA creates tables via ddl-auto=create-drop
-- All valid_from dates are 2020-01-01 so data is active for tests in 2026
-- CLEAN ALL TABLES (order matters because of FK)

DELETE FROM age_risk_coefficients;
DELETE FROM underwriting_rules_config;
DELETE FROM risk_bundles;
DELETE FROM trip_duration_coefficients;
DELETE FROM risk_types;
DELETE FROM medical_risk_limit_levels;
DELETE FROM promo_codes;
DELETE FROM countries;

-- ============================================
-- 1. COUNTRIES
-- ============================================
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from, created_at) VALUES
('ES', 'Spain', 'Испания', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('FR', 'France', 'Франция', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('DE', 'Germany', 'Германия', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('IT', 'Italy', 'Италия', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('AT', 'Austria', 'Австрия', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('CH', 'Switzerland', 'Швейцария', 'LOW', 1.0, '2020-01-01', CURRENT_TIMESTAMP),
('TR', 'Turkey', 'Турция', 'MEDIUM', 1.3, '2020-01-01', CURRENT_TIMESTAMP),
('US', 'United States', 'США', 'MEDIUM', 1.3, '2020-01-01', CURRENT_TIMESTAMP),
('EG', 'Egypt', 'Египет', 'HIGH', 1.8, '2020-01-01', CURRENT_TIMESTAMP),
('IN', 'India', 'Индия', 'HIGH', 1.8, '2020-01-01', CURRENT_TIMESTAMP),
('AF', 'Afghanistan', 'Афганистан', 'VERY_HIGH', 3.0, '2020-01-01', CURRENT_TIMESTAMP);

-- ============================================
-- 2. MEDICAL RISK LIMIT LEVELS
-- ============================================
INSERT INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from, created_at) VALUES
('5000', 5000.00, 1.50, 'EUR', '2020-01-01', CURRENT_TIMESTAMP),
('10000', 10000.00, 2.00, 'EUR', '2020-01-01', CURRENT_TIMESTAMP),
('20000', 20000.00, 3.00, 'EUR', '2020-01-01', CURRENT_TIMESTAMP),
('50000', 50000.00, 4.50, 'EUR', '2020-01-01', CURRENT_TIMESTAMP),
('100000', 100000.00, 7.00, 'EUR', '2020-01-01', CURRENT_TIMESTAMP),
('200000', 200000.00, 12.00, 'EUR', '2020-01-01', CURRENT_TIMESTAMP);

-- ============================================
-- 3. RISK TYPES
-- ============================================
INSERT INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from, created_at) VALUES
('TRAVEL_MEDICAL', 'Medical Coverage', 'Медицинское покрытие', 0.00, true, 'Base medical coverage', '2020-01-01', CURRENT_TIMESTAMP),
('SPORT_ACTIVITIES', 'Sport Activities', 'Активный спорт', 0.30, false, 'Skiing, snowboarding, diving', '2020-01-01', CURRENT_TIMESTAMP),
('EXTREME_SPORT', 'Extreme Sport', 'Экстремальный спорт', 0.60, false, 'Mountaineering, parachuting', '2020-01-01', CURRENT_TIMESTAMP),
('PREGNANCY', 'Pregnancy Coverage', 'Покрытие беременности', 0.20, false, 'Up to 31 weeks', '2020-01-01', CURRENT_TIMESTAMP),
('CHRONIC_DISEASES', 'Chronic Diseases', 'Хронические заболевания', 0.40, false, 'Diabetes, asthma, etc.', '2020-01-01', CURRENT_TIMESTAMP),
('ACCIDENT_COVERAGE', 'Accident Coverage', 'От несчастных случаев', 0.20, false, 'Extended accident coverage', '2020-01-01', CURRENT_TIMESTAMP),
('TRIP_CANCELLATION', 'Trip Cancellation', 'Отмена поездки', 0.15, false, 'Trip cancellation insurance', '2020-01-01', CURRENT_TIMESTAMP),
('LUGGAGE_LOSS', 'Luggage Loss', 'Потеря багажа', 0.10, false, 'Lost luggage coverage', '2020-01-01', CURRENT_TIMESTAMP),
('FLIGHT_DELAY', 'Flight Delay', 'Задержка рейса', 0.05, false, 'Flight delay compensation', '2020-01-01', CURRENT_TIMESTAMP),
('CIVIL_LIABILITY', 'Civil Liability', 'Гражданская ответственность', 0.10, false, 'Third party liability', '2020-01-01', CURRENT_TIMESTAMP);

-- ============================================
-- 4. PROMO CODES
-- ============================================
INSERT INTO promo_codes (code, description, discount_type, discount_value, min_premium_amount, max_discount_amount, valid_from, valid_to, max_usage_count, current_usage_count, is_active, created_at) VALUES
('SUMMER2025', 'Summer discount 10%', 'PERCENTAGE', 10, 50, 100, '2025-06-01', '2025-08-31', 1000, 0, true, CURRENT_TIMESTAMP),
('WINTER2025', 'Winter discount 15%', 'PERCENTAGE', 15, 100, 200, '2025-12-01', '2026-12-31', 500, 0, true, CURRENT_TIMESTAMP),
('WELCOME50', 'Welcome bonus 50 EUR', 'FIXED_AMOUNT', 50, 200, NULL, '2025-01-01', '2026-12-31', 100, 0, true, CURRENT_TIMESTAMP),
('FAMILY20', 'Family discount 20%', 'PERCENTAGE', 20, 150, 300, '2025-01-01', '2026-12-31', NULL, 0, true, CURRENT_TIMESTAMP);

-- ============================================
-- 5. TRIP DURATION COEFFICIENTS
-- ============================================
INSERT INTO trip_duration_coefficients (days_from, days_to, coefficient, description, valid_from, created_at) VALUES
(1, 7, 1.00, 'Short trip (1 week)', '2020-01-01', CURRENT_TIMESTAMP),
(8, 14, 0.95, 'Medium trip (2 weeks) - 5% discount', '2020-01-01', CURRENT_TIMESTAMP),
(15, 30, 0.90, 'Long trip (1 month) - 10% discount', '2020-01-01', CURRENT_TIMESTAMP),
(31, 60, 0.88, 'Extended trip (2 months) - 12% discount', '2020-01-01', CURRENT_TIMESTAMP),
(61, 90, 0.85, 'Very long trip (3 months) - 15% discount', '2020-01-01', CURRENT_TIMESTAMP),
(91, 365, 0.82, 'Ultra long trip (3+ months) - 18% discount', '2020-01-01', CURRENT_TIMESTAMP);

-- ============================================
-- 6. RISK BUNDLES
-- ============================================
INSERT INTO risk_bundles (code, name_en, name_ru, description, discount_percentage, required_risks, valid_from, is_active, created_at) VALUES
('ACTIVE_TRAVELER', 'Active Traveler Package', 'Пакет Активный путешественник', 'For sports enthusiasts', 15.00, '["SPORT_ACTIVITIES", "ACCIDENT_COVERAGE"]', '2020-01-01', true, CURRENT_TIMESTAMP),
('FULL_PROTECTION', 'Full Protection Package', 'Пакет Полная защита', 'Complete coverage', 20.00, '["TRIP_CANCELLATION", "LUGGAGE_LOSS", "FLIGHT_DELAY"]', '2020-01-01', true, CURRENT_TIMESTAMP),
('EXTREME_ADVENTURE', 'Extreme Adventure', 'Экстремальное приключение', 'For adrenaline junkies', 18.00, '["EXTREME_SPORT", "ACCIDENT_COVERAGE", "CHRONIC_DISEASES"]', '2020-01-01', true, CURRENT_TIMESTAMP);

-- ============================================
-- 7. AGE RISK COEFFICIENTS
-- ============================================
INSERT INTO age_risk_coefficients (risk_type_code, age_from, age_to, coefficient_modifier, description, valid_from, created_at) VALUES
('EXTREME_SPORT', 18, 35, 1.00, 'Standard rate for young adults', '2020-01-01', CURRENT_TIMESTAMP),
('EXTREME_SPORT', 36, 50, 1.30, '+30% for middle-aged', '2020-01-01', CURRENT_TIMESTAMP),
('EXTREME_SPORT', 51, 65, 1.80, '+80% for seniors', '2020-01-01', CURRENT_TIMESTAMP),
('EXTREME_SPORT', 66, 80, 2.50, '+150% for elderly', '2020-01-01', CURRENT_TIMESTAMP),
('SPORT_ACTIVITIES', 18, 50, 1.00, 'Standard rate', '2020-01-01', CURRENT_TIMESTAMP),
('SPORT_ACTIVITIES', 51, 65, 1.20, '+20% for seniors', '2020-01-01', CURRENT_TIMESTAMP),
('SPORT_ACTIVITIES', 66, 80, 1.50, '+50% for elderly', '2020-01-01', CURRENT_TIMESTAMP),
('CHRONIC_DISEASES', 18, 45, 1.00, 'Standard rate', '2020-01-01', CURRENT_TIMESTAMP),
('CHRONIC_DISEASES', 46, 60, 1.40, '+40% for middle-aged', '2020-01-01', CURRENT_TIMESTAMP),
('CHRONIC_DISEASES', 61, 70, 1.80, '+80% for seniors', '2020-01-01', CURRENT_TIMESTAMP),
('CHRONIC_DISEASES', 71, 80, 2.50, '+150% for elderly', '2020-01-01', CURRENT_TIMESTAMP);

-- ============================================
-- 8. UNDERWRITING RULES CONFIG
-- ============================================
INSERT INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active, created_at) VALUES
('AgeRule', 'MAX_AGE', '80', 'Maximum allowed age', '2020-01-01', true, CURRENT_TIMESTAMP),
('AgeRule', 'REVIEW_AGE_THRESHOLD', '75', 'Age threshold for review', '2020-01-01', true, CURRENT_TIMESTAMP),
('AdditionalRisksRule', 'MAX_AGE_FOR_EXTREME_SPORT', '70', 'Max age for extreme sport', '2020-01-01', true, CURRENT_TIMESTAMP),
('AdditionalRisksRule', 'REVIEW_AGE_FOR_EXTREME_SPORT', '60', 'Review age for extreme sport', '2020-01-01', true, CURRENT_TIMESTAMP),
('TripDurationRule', 'MAX_DAYS', '180', 'Maximum trip duration', '2020-01-01', true, CURRENT_TIMESTAMP),
('TripDurationRule', 'REVIEW_DAYS_THRESHOLD', '90', 'Days requiring review', '2020-01-01', true, CURRENT_TIMESTAMP),
('MedicalCoverageRule', 'REVIEW_AGE', '70', 'Age for medical review', '2020-01-01', true, CURRENT_TIMESTAMP),
('MedicalCoverageRule', 'BLOCKING_AGE', '75', 'Age blocking high coverage', '2020-01-01', true, CURRENT_TIMESTAMP),
('MedicalCoverageRule', 'REVIEW_COVERAGE_THRESHOLD', '100000', 'Coverage requiring review', '2020-01-01', true, CURRENT_TIMESTAMP),
('MedicalCoverageRule', 'BLOCKING_COVERAGE_THRESHOLD', '200000', 'Max coverage for elderly', '2020-01-01', true, CURRENT_TIMESTAMP);