-- src/test/resources/test-data/duration-coefficients.sql
INSERT INTO duration_coefficients (days_from, days_to, coefficient, description) VALUES
(0, 10, 1.0, 'Short trip'),
(11, 30, 0.95, 'Medium trip - 5% discount'),
(31, 60, 0.90, 'Long trip - 10% discount'),
(61, 90, 0.85, 'Extended trip - 15% discount'),
(91, 180, 0.80, 'Very long trip - 20% discount'),
(181, 365, 0.75, 'Annual trip - 25% discount');