-- src/test/resources/test-data/age-coefficients.sql
INSERT INTO age_coefficients (age_from, age_to, coefficient, age_group) VALUES
(0, 17, 0.8, 'Children'),
(18, 24, 0.9, 'Young adults'),
(25, 64, 1.0, 'Adults'),
(65, 74, 1.5, 'Seniors'),
(75, 80, 2.0, 'Elderly');