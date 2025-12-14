-- Очистка тестовых данных перед запуском тестов
-- Порядок важен из-за foreign key constraints

-- Удаляем данные из зависимых таблиц
DELETE FROM agreement_risks;
DELETE FROM agreement_discounts;
DELETE FROM promo_code_usage;
DELETE FROM payments;
DELETE FROM agreements;
DELETE FROM persons;

-- Удаляем справочные данные
DELETE FROM promo_codes;
DELETE FROM discounts;
DELETE FROM risk_types;
DELETE FROM medical_risk_limit_levels;
DELETE FROM countries;
DELETE FROM age_coefficients;
DELETE FROM currencies;
