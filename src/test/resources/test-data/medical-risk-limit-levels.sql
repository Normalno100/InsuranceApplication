-- src/test/resources/test-data/medical-risk-limit-levels.sql
INSERT INTO medical_risk_limit_levels (level_code, coverage_amount, base_rate, description) VALUES
('5000', 5000.00, 5.00, 'Basic coverage - 5,000 EUR'),
('10000', 10000.00, 8.00, 'Standard coverage - 10,000 EUR'),
('20000', 20000.00, 12.00, 'Enhanced coverage - 20,000 EUR'),
('50000', 50000.00, 20.00, 'Premium coverage - 50,000 EUR'),
('100000', 100000.00, 30.00, 'Gold coverage - 100,000 EUR'),
('200000', 200000.00, 45.00, 'Platinum coverage - 200,000 EUR'),
('500000', 500000.00, 75.00, 'Ultimate coverage - 500,000 EUR');