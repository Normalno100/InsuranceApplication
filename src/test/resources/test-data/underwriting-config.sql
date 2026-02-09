-- src/test/resources/test-data/underwriting-config.sql
-- Age-based rules
INSERT INTO underwriting_rules (rule_name, rule_type, condition_field, condition_operator, condition_value, decision, severity, reason, priority) VALUES
('AGE_MAX_80', 'AGE', 'age', 'GREATER_THAN', '80', 'DECLINED', 'HIGH', 'Age exceeds maximum limit of 80 years', 100),
('AGE_MANUAL_REVIEW_75', 'AGE', 'age', 'GREATER_THAN', '75', 'REQUIRES_MANUAL_REVIEW', 'MEDIUM', 'Age requires manual underwriting review', 90);

-- Country risk rules
INSERT INTO underwriting_rules (rule_name, rule_type, condition_field, condition_operator, condition_value, decision, severity, reason, priority) VALUES
('COUNTRY_VERY_HIGH_RISK', 'COUNTRY', 'risk_level', 'EQUALS', 'VERY_HIGH', 'DECLINED', 'HIGH', 'Travel to very high risk country is declined', 95),
('COUNTRY_HIGH_RISK', 'COUNTRY', 'risk_level', 'EQUALS', 'HIGH', 'REQUIRES_MANUAL_REVIEW', 'MEDIUM', 'Travel to high risk country requires review', 85);

-- Duration rules
INSERT INTO underwriting_rules (rule_name, rule_type, condition_field, condition_operator, condition_value, decision, severity, reason, priority) VALUES
('DURATION_MAX_180', 'DURATION', 'days', 'GREATER_THAN', '180', 'DECLINED', 'HIGH', 'Trip duration exceeds maximum of 180 days', 80),
('DURATION_REVIEW_90', 'DURATION', 'days', 'GREATER_THAN', '90', 'REQUIRES_MANUAL_REVIEW', 'MEDIUM', 'Long trip duration requires review', 70);

-- Risk-specific rules
INSERT INTO underwriting_rules (rule_name, rule_type, condition_field, condition_operator, condition_value, decision, severity, reason, priority) VALUES
('EXTREME_SPORT_AGE_70', 'RISK_AGE', 'risk_code,age', 'EXTREME_SPORT,GREATER_THAN', '70', 'DECLINED', 'HIGH', 'Extreme sport coverage declined for age over 70', 85),
('EXTREME_SPORT_AGE_65', 'RISK_AGE', 'risk_code,age', 'EXTREME_SPORT,GREATER_THAN', '65', 'REQUIRES_MANUAL_REVIEW', 'MEDIUM', 'Extreme sport for age over 65 requires review', 75);