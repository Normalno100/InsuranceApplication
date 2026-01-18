-- Тестовые данные для таблицы underwriting_rules_config

-- Полная очистка таблицы перед вставкой
DELETE FROM underwriting_rules_config;

-- AgeRule конфигурация
INSERT INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active)
VALUES
('AgeRule', 'MAX_AGE', '80', 'Maximum allowed age for insurance', '2020-01-01', TRUE),
('AgeRule', 'REVIEW_AGE_THRESHOLD', '75', 'Age threshold for manual review', '2020-01-01', TRUE);

-- MedicalCoverageRule конфигурация
INSERT INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active)
VALUES
('MedicalCoverageRule', 'REVIEW_AGE', '70', 'Age threshold for medical coverage review', '2020-01-01', TRUE),
('MedicalCoverageRule', 'BLOCKING_AGE', '75', 'Age threshold for blocking high coverage', '2020-01-01', TRUE),
('MedicalCoverageRule', 'REVIEW_COVERAGE_THRESHOLD', '100000', 'Coverage amount requiring review for older applicants', '2020-01-01', TRUE),
('MedicalCoverageRule', 'BLOCKING_COVERAGE_THRESHOLD', '200000', 'Maximum coverage for very old applicants', '2020-01-01', TRUE);

-- AdditionalRisksRule конфигурация
INSERT INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active)
VALUES
('AdditionalRisksRule', 'MAX_AGE_FOR_EXTREME_SPORT', '70', 'Maximum age for extreme sport coverage', '2020-01-01', TRUE),
('AdditionalRisksRule', 'REVIEW_AGE_FOR_EXTREME_SPORT', '60', 'Age requiring review for extreme sport', '2020-01-01', TRUE);

-- TripDurationRule конфигурация
INSERT INTO underwriting_rules_config (rule_name, parameter_name, parameter_value, description, valid_from, is_active)
VALUES
('TripDurationRule', 'MAX_DAYS', '180', 'Maximum trip duration in days', '2020-01-01', TRUE),
('TripDurationRule', 'REVIEW_DAYS_THRESHOLD', '90', 'Trip duration requiring manual review', '2020-01-01', TRUE);