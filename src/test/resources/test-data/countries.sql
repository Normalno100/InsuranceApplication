INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from) VALUES
-- Low risk
('ES', 'Spain', 'Испания', 'LOW', 1.0, '2025-01-01'),
('FR', 'France', 'Франция', 'LOW', 1.0, '2025-01-01'),
('DE', 'Germany', 'Германия', 'LOW', 1.0, '2025-01-01'),
('IT', 'Italy', 'Италия', 'LOW', 1.0, '2025-01-01'),
('AT', 'Austria', 'Австрия', 'LOW', 1.0, '2025-01-01'),
-- Medium risk
('TR', 'Turkey', 'Турция', 'MEDIUM', 1.3, '2025-01-01'),
('US', 'United States', 'США', 'MEDIUM', 1.3, '2025-01-01'),
-- High risk
('EG', 'Egypt', 'Египет', 'HIGH', 1.8, '2025-01-01')
ON CONFLICT DO NOTHING;