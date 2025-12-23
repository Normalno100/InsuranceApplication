-- Тестовые данные для таблицы countries

DELETE FROM countries WHERE iso_code IN ('ES', 'DE', 'FR');

-- Страны с низким риском
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from)
VALUES
('ES', 'Spain', 'Испания', 'LOW', 1.0, '2020-01-01'),
('DE', 'Germany', 'Германия', 'LOW', 1.0, '2020-01-01'),
('FR', 'France', 'Франция', 'LOW', 1.0, '2020-01-01'),
('IT', 'Italy', 'Италия', 'LOW', 1.0, '2020-01-01');

-- Страны со средним риском
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from)
VALUES
('TH', 'Thailand', 'Таиланд', 'MEDIUM', 1.3, '2020-01-01'),
('TR', 'Turkey', 'Турция', 'MEDIUM', 1.3, '2020-01-01'),
('US', 'United States', 'США', 'MEDIUM', 1.3, '2020-01-01');

-- Страны с высоким риском
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from)
VALUES
('IN', 'India', 'Индия', 'HIGH', 1.8, '2020-01-01'),
('EG', 'Egypt', 'Египет', 'HIGH', 1.8, '2020-01-01');

-- Страны с очень высоким риском
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient, valid_from)
VALUES
('AF', 'Afghanistan', 'Афганистан', 'VERY_HIGH', 2.5, '2020-01-01');