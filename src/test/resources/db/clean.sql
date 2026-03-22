-- ============================================
-- CLEAN.SQL — сброс тестовых данных
-- ЭТАП 3 (рефакторинг): Изоляция интеграционных тестов
--
-- Выполняется BEFORE_TEST_METHOD перед каждым тестом.
-- Гарантирует что порядок выполнения тестов не важен:
-- каждый тест начинает с чистого и предсказуемого состояния.
--
-- ПОРЯДОК DELETE важен — сначала зависимые таблицы,
-- потом родительские (FK constraints).
-- ============================================

DELETE FROM age_risk_coefficients;
DELETE FROM underwriting_rules_config;
DELETE FROM risk_bundles;
DELETE FROM trip_duration_coefficients;
DELETE FROM discounts;
DELETE FROM promo_codes;
DELETE FROM risk_types;
DELETE FROM medical_risk_limit_levels;
DELETE FROM countries;