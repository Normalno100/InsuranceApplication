INSERT INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from) VALUES
('TRAVEL_MEDICAL', 'Medical Coverage', 'Медицинское покрытие', 0.00, true, 'Base medical coverage', '2025-01-01'),
('SPORT_ACTIVITIES', 'Sport Activities', 'Активный спорт', 0.30, false, 'Skiing, snowboarding, diving', '2025-01-01'),
('EXTREME_SPORT', 'Extreme Sport', 'Экстремальный спорт', 0.60, false, 'Mountaineering, parachuting', '2025-01-01'),
('PREGNANCY', 'Pregnancy Coverage', 'Покрытие беременности', 0.20, false, 'Up to 31 weeks', '2025-01-01'),
('CHRONIC_DISEASES', 'Chronic Diseases', 'Хронические заболевания', 0.40, false, 'Diabetes, asthma, etc.', '2025-01-01'),
('ACCIDENT_COVERAGE', 'Accident Coverage', 'От несчастных случаев', 0.20, false, 'Extended accident coverage', '2025-01-01'),
('TRIP_CANCELLATION', 'Trip Cancellation', 'Отмена поездки', 0.15, false, 'Trip cancellation insurance', '2025-01-01'),
('LUGGAGE_LOSS', 'Luggage Loss', 'Потеря багажа', 0.10, false, 'Lost luggage coverage', '2025-01-01'),
('FLIGHT_DELAY', 'Flight Delay', 'Задержка рейса', 0.05, false, 'Flight delay compensation', '2025-01-01'),
('CIVIL_LIABILITY', 'Civil Liability', 'Гражданская ответственность', 0.10, false, 'Third party liability', '2025-01-01')
ON CONFLICT DO NOTHING;