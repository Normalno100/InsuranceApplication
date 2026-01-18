-- Тестовые данные для таблицы medical_risk_limit_levels

DELETE FROM medical_risk_limit_levels;

INSERT INTO medical_risk_limit_levels (code, coverage_amount, daily_rate, currency, valid_from)
VALUES
('5000', 5000.00, 1.50, 'EUR', '2020-01-01'),
('10000', 10000.00, 2.00, 'EUR', '2020-01-01'),
('20000', 20000.00, 3.00, 'EUR', '2020-01-01'),
('50000', 50000.00, 4.50, 'EUR', '2020-01-01'),
('100000', 100000.00, 7.00, 'EUR', '2020-01-01'),
('200000', 200000.00, 12.00, 'EUR', '2020-01-01'),
('500000', 500000.00, 20.00, 'EUR', '2020-01-01');