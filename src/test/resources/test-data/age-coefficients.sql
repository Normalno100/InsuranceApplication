INSERT INTO age_coefficients (age_from, age_to, coefficient, description, valid_from) VALUES
(0, 5, 1.10, 'Infants and toddlers', '2025-01-01'),
(6, 17, 0.90, 'Children and teenagers', '2025-01-01'),
(18, 30, 1.00, 'Young adults', '2025-01-01'),
(31, 40, 1.10, 'Adults', '2025-01-01'),
(41, 50, 1.30, 'Middle-aged', '2025-01-01'),
(51, 60, 1.60, 'Senior', '2025-01-01'),
(61, 70, 2.00, 'Elderly', '2025-01-01'),
(71, 80, 2.50, 'Very elderly', '2025-01-01')
ON CONFLICT DO NOTHING;