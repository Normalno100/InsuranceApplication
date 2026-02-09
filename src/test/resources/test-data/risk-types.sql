-- src/test/resources/test-data/risk-types.sql
INSERT INTO risk_types (risk_code, name, description, base_premium, is_mandatory) VALUES
('TRAVEL_MEDICAL', 'Travel Medical Insurance', 'Mandatory medical coverage', 0.00, TRUE),
('SPORT_ACTIVITIES', 'Sport Activities', 'Coverage for sport activities', 15.00, FALSE),
('EXTREME_SPORT', 'Extreme Sport', 'Coverage for extreme sports', 50.00, FALSE),
('PREGNANCY', 'Pregnancy Coverage', 'Coverage for pregnancy-related risks', 25.00, FALSE),
('CHRONIC_DISEASES', 'Chronic Diseases', 'Coverage for chronic diseases', 30.00, FALSE),
('ACCIDENT_COVERAGE', 'Accident Coverage', 'Additional accident coverage', 20.00, FALSE),
('TRIP_CANCELLATION', 'Trip Cancellation', 'Coverage for trip cancellation', 35.00, FALSE),
('LUGGAGE_LOSS', 'Luggage Loss', 'Coverage for luggage loss', 10.00, FALSE),
('FLIGHT_DELAY', 'Flight Delay', 'Coverage for flight delays', 8.00, FALSE),
('CIVIL_LIABILITY', 'Civil Liability', 'Civil liability coverage', 12.00, FALSE);