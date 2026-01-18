-- Тестовые данные для таблицы risk_types
DELETE FROM risk_types;

-- Обязательный риск
INSERT INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from)
VALUES ('TRAVEL_MEDICAL', 'Medical Coverage', 'Медицинское покрытие', 0.00, TRUE, 'Base medical coverage', '2020-01-01');

-- Опциональные риски
INSERT INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description, valid_from)
VALUES
('SPORT_ACTIVITIES', 'Sport Activities', 'Активный спорт', 0.30, FALSE, 'Skiing, snowboarding, diving', '2020-01-01'),
('EXTREME_SPORT', 'Extreme Sport', 'Экстремальный спорт', 0.60, FALSE, 'Mountaineering, parachuting', '2020-01-01'),
('PREGNANCY', 'Pregnancy Coverage', 'Покрытие беременности', 0.20, FALSE, 'Up to 31 weeks', '2020-01-01'),
('CHRONIC_DISEASES', 'Chronic Diseases', 'Хронические заболевания', 0.40, FALSE, 'Diabetes, asthma, etc.', '2020-01-01'),
('ACCIDENT_COVERAGE', 'Accident Coverage', 'От несчастных случаев', 0.20, FALSE, 'Extended accident coverage', '2020-01-01'),
('TRIP_CANCELLATION', 'Trip Cancellation', 'Отмена поездки', 0.15, FALSE, 'Trip cancellation insurance', '2020-01-01'),
('LUGGAGE_LOSS', 'Luggage Loss', 'Потеря багажа', 0.10, FALSE, 'Lost luggage coverage', '2020-01-01'),
('FLIGHT_DELAY', 'Flight Delay', 'Задержка рейса', 0.05, FALSE, 'Flight delay compensation', '2020-01-01'),
('CIVIL_LIABILITY', 'Civil Liability', 'Гражданская ответственность', 0.10, FALSE, 'Third party liability', '2020-01-01');