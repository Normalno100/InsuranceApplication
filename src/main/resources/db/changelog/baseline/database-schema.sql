-- ============================================
-- TRAVEL INSURANCE DATABASE SCHEMA
-- ============================================

-- ============================================
-- 1. СПРАВОЧНИКИ (REFERENCE TABLES)
-- ============================================

-- Таблица коэффициентов возраста
CREATE TABLE age_coefficients (
    id BIGSERIAL PRIMARY KEY,
    age_from INT NOT NULL,
    age_to INT NOT NULL,
    coefficient DECIMAL(5, 2) NOT NULL,
    description VARCHAR(100),
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_age_range UNIQUE (age_from, age_to, valid_from)
);

CREATE INDEX idx_age_range ON age_coefficients(age_from, age_to);
CREATE INDEX idx_validity ON age_coefficients(valid_from, valid_to);

-- Начальные данные
INSERT INTO age_coefficients (age_from, age_to, coefficient, description) VALUES
(0, 5, 1.10, 'Infants and toddlers'),
(6, 17, 0.90, 'Children and teenagers'),
(18, 30, 1.00, 'Young adults'),
(31, 40, 1.10, 'Adults'),
(41, 50, 1.30, 'Middle-aged'),
(51, 60, 1.60, 'Senior'),
(61, 70, 2.00, 'Elderly'),
(71, 80, 2.50, 'Very elderly');

-- ============================================

-- Таблица базовых ставок медицинского покрытия
CREATE TABLE medical_risk_limit_levels (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) NOT NULL,
    coverage_amount DECIMAL(12, 2) NOT NULL,
    daily_rate DECIMAL(8, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_level_code UNIQUE (code, valid_from)
);

CREATE INDEX idx_coverage_amount ON medical_risk_limit_levels(coverage_amount);

-- Начальные данные
INSERT INTO medical_risk_limit_levels (code, coverage_amount, daily_rate) VALUES
('LEVEL_5000', 5000.00, 1.50),
('LEVEL_10000', 10000.00, 2.00),
('LEVEL_20000', 20000.00, 3.00),
('LEVEL_50000', 50000.00, 4.50),
('LEVEL_100000', 100000.00, 7.00),
('LEVEL_200000', 200000.00, 12.00),
('LEVEL_500000', 500000.00, 20.00);

-- ============================================

-- Таблица стран с коэффициентами риска
CREATE TABLE countries (
    id BIGSERIAL PRIMARY KEY,
    iso_code VARCHAR(2) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    name_ru VARCHAR(100),
    risk_group VARCHAR(20) NOT NULL,
    risk_coefficient DECIMAL(5, 2) NOT NULL,
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_country_code UNIQUE (iso_code, valid_from)
);

CREATE INDEX idx_country_risk ON countries(risk_group);
CREATE INDEX idx_country_iso ON countries(iso_code);

-- Начальные данные
INSERT INTO countries (iso_code, name_en, name_ru, risk_group, risk_coefficient) VALUES
-- Низкий риск
('ES', 'Spain', 'Испания', 'LOW', 1.0),
('DE', 'Germany', 'Германия', 'LOW', 1.0),
('FR', 'France', 'Франция', 'LOW', 1.0),
('IT', 'Italy', 'Италия', 'LOW', 1.0),
('AT', 'Austria', 'Австрия', 'LOW', 1.0),
('JP', 'Japan', 'Япония', 'LOW', 1.0),
('CA', 'Canada', 'Канада', 'LOW', 1.0),
('AU', 'Australia', 'Австралия', 'LOW', 1.0),
-- Средний риск
('TH', 'Thailand', 'Таиланд', 'MEDIUM', 1.3),
('VN', 'Vietnam', 'Вьетнам', 'MEDIUM', 1.3),
('TR', 'Turkey', 'Турция', 'MEDIUM', 1.3),
('AE', 'UAE', 'ОАЭ', 'MEDIUM', 1.3),
('CN', 'China', 'Китай', 'MEDIUM', 1.3),
('MX', 'Mexico', 'Мексика', 'MEDIUM', 1.3),
('BR', 'Brazil', 'Бразилия', 'MEDIUM', 1.3),
('US', 'United States', 'США', 'MEDIUM', 1.3),
-- Высокий риск
('IN', 'India', 'Индия', 'HIGH', 1.8),
('EG', 'Egypt', 'Египет', 'HIGH', 1.8),
('KE', 'Kenya', 'Кения', 'HIGH', 1.8),
('ZA', 'South Africa', 'ЮАР', 'HIGH', 1.8),
('MA', 'Morocco', 'Марокко', 'HIGH', 1.8),
-- Очень высокий риск
('AF', 'Afghanistan', 'Афганистан', 'VERY_HIGH', 2.5),
('IQ', 'Iraq', 'Ирак', 'VERY_HIGH', 2.5),
('SY', 'Syria', 'Сирия', 'VERY_HIGH', 2.5);

-- ============================================

-- Таблица типов рисков
CREATE TABLE risk_types (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL,
    name_en VARCHAR(100) NOT NULL,
    name_ru VARCHAR(100),
    coefficient DECIMAL(5, 2) NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    description TEXT,
    valid_from DATE NOT NULL DEFAULT CURRENT_DATE,
    valid_to DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_risk_code UNIQUE (code, valid_from)
);

CREATE INDEX idx_risk_mandatory ON risk_types(is_mandatory);

-- Начальные данные
INSERT INTO risk_types (code, name_en, name_ru, coefficient, is_mandatory, description) VALUES
('TRAVEL_MEDICAL', 'Medical Coverage', 'Медицинское покрытие', 0.00, TRUE, 'Base medical coverage'),
('SPORT_ACTIVITIES', 'Sport Activities', 'Активный спорт', 0.30, FALSE, 'Skiing, snowboarding, diving'),
('EXTREME_SPORT', 'Extreme Sport', 'Экстремальный спорт', 0.60, FALSE, 'Mountaineering, parachuting'),
('PREGNANCY', 'Pregnancy Coverage', 'Покрытие беременности', 0.20, FALSE, 'Up to 31 weeks'),
('CHRONIC_DISEASES', 'Chronic Diseases', 'Хронические заболевания', 0.40, FALSE, 'Diabetes, asthma, etc.'),
('ACCIDENT_COVERAGE', 'Accident Coverage', 'От несчастных случаев', 0.20, FALSE, 'Extended accident coverage'),
('TRIP_CANCELLATION', 'Trip Cancellation', 'Отмена поездки', 0.15, FALSE, 'Trip cancellation insurance'),
('LUGGAGE_LOSS', 'Luggage Loss', 'Потеря багажа', 0.10, FALSE, 'Lost luggage coverage'),
('FLIGHT_DELAY', 'Flight Delay', 'Задержка рейса', 0.05, FALSE, 'Flight delay compensation'),
('CIVIL_LIABILITY', 'Civil Liability', 'Гражданская ответственность', 0.10, FALSE, 'Third party liability');

-- ============================================

-- Таблица валют
CREATE TABLE currencies (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    symbol VARCHAR(5),
    exchange_rate_to_eur DECIMAL(10, 6) NOT NULL DEFAULT 1.0,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Начальные данные
INSERT INTO currencies (code, name, symbol, exchange_rate_to_eur) VALUES
('EUR', 'Euro', '€', 1.0),
('USD', 'US Dollar', '$', 0.92),
('GBP', 'British Pound', '£', 1.17),
('CHF', 'Swiss Franc', 'CHF', 1.03),
('JPY', 'Japanese Yen', '¥', 0.0063),
('CNY', 'Chinese Yuan', '¥', 0.13),
('RUB', 'Russian Ruble', '₽', 0.0095);

-- ============================================
-- 2. ОСНОВНЫЕ БИЗНЕС-ТАБЛИЦЫ
-- ============================================

-- Таблица персон
CREATE TABLE persons (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20),
    passport_number VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_person_name ON persons(last_name, first_name);
CREATE INDEX idx_person_birth_date ON persons(birth_date);
CREATE INDEX idx_person_email ON persons(email);

-- ============================================

-- Таблица договоров страхования
CREATE TABLE agreements (
    id BIGSERIAL PRIMARY KEY,
    agreement_number VARCHAR(50) NOT NULL UNIQUE,
    person_id BIGINT NOT NULL REFERENCES persons(id),

    -- Даты договора
    date_from DATE NOT NULL,
    date_to DATE NOT NULL,
    days_count INT NOT NULL,

    -- Страна и риски
    country_iso_code VARCHAR(2) NOT NULL,
    medical_risk_limit_level VARCHAR(20) NOT NULL,

    -- Цена
    premium_amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',

    -- Статус
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', -- DRAFT, ACTIVE, EXPIRED, CANCELLED

    -- Мета-информация
    calculation_details JSONB, -- детали расчета
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_date_order CHECK (date_to >= date_from),
    CONSTRAINT chk_days_positive CHECK (days_count > 0),
    CONSTRAINT chk_premium_positive CHECK (premium_amount > 0)
);

CREATE INDEX idx_agreement_number ON agreements(agreement_number);
CREATE INDEX idx_agreement_person ON agreements(person_id);
CREATE INDEX idx_agreement_dates ON agreements(date_from, date_to);
CREATE INDEX idx_agreement_status ON agreements(status);
CREATE INDEX idx_agreement_country ON agreements(country_iso_code);

-- ============================================

-- Таблица выбранных рисков по договору
CREATE TABLE agreement_risks (
    id BIGSERIAL PRIMARY KEY,
    agreement_id BIGINT NOT NULL REFERENCES agreements(id) ON DELETE CASCADE,
    risk_type_code VARCHAR(50) NOT NULL,
    premium_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agreement_risk UNIQUE (agreement_id, risk_type_code)
);

CREATE INDEX idx_agreement_risk ON agreement_risks(agreement_id);

-- ============================================

-- Таблица промо-кодов
CREATE TABLE promo_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),

    -- Тип скидки
    discount_type VARCHAR(20) NOT NULL, -- PERCENTAGE, FIXED_AMOUNT
    discount_value DECIMAL(10, 2) NOT NULL,

    -- Ограничения
    min_premium_amount DECIMAL(12, 2),
    max_discount_amount DECIMAL(12, 2),

    -- Применимость
    applicable_countries VARCHAR(255), -- JSON array или comma-separated
    applicable_risk_levels VARCHAR(255),

    -- Период действия
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,

    -- Ограничения использования
    max_usage_count INT,
    current_usage_count INT DEFAULT 0,

    -- Статус
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_discount_positive CHECK (discount_value > 0),
    CONSTRAINT chk_dates CHECK (valid_to >= valid_from)
);

CREATE INDEX idx_promo_code ON promo_codes(code);
CREATE INDEX idx_promo_validity ON promo_codes(valid_from, valid_to);
CREATE INDEX idx_promo_active ON promo_codes(is_active);

-- Начальные данные
INSERT INTO promo_codes (code, description, discount_type, discount_value, valid_from, valid_to, max_usage_count) VALUES
('SUMMER2025', 'Summer discount 10%', 'PERCENTAGE', 10, '2025-06-01', '2025-08-31', 1000),
('WINTER2025', 'Winter discount 15%', 'PERCENTAGE', 15, '2025-12-01', '2026-02-28', 500),
('WELCOME50', 'Welcome bonus 50 EUR', 'FIXED_AMOUNT', 50, '2025-01-01', '2025-12-31', 100),
('FAMILY20', 'Family discount 20%', 'PERCENTAGE', 20, '2025-01-01', '2025-12-31', NULL);

-- ============================================

-- Таблица использования промо-кодов
CREATE TABLE promo_code_usage (
    id BIGSERIAL PRIMARY KEY,
    promo_code_id BIGINT NOT NULL REFERENCES promo_codes(id),
    agreement_id BIGINT NOT NULL REFERENCES agreements(id),
    discount_amount DECIMAL(12, 2) NOT NULL,
    used_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_agreement_promo UNIQUE (agreement_id)
);

CREATE INDEX idx_promo_usage_code ON promo_code_usage(promo_code_id);
CREATE INDEX idx_promo_usage_agreement ON promo_code_usage(agreement_id);

-- ============================================

-- Таблица скидок (групповые, корпоративные)
CREATE TABLE discounts (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    description TEXT,

    -- Тип скидки
    discount_type VARCHAR(20) NOT NULL, -- GROUP, CORPORATE, SEASONAL, LOYALTY
    discount_percentage DECIMAL(5, 2) NOT NULL,

    -- Условия применения
    min_persons_count INT,
    min_premium_amount DECIMAL(12, 2),

    -- Период действия
    valid_from DATE NOT NULL,
    valid_to DATE,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Начальные данные
INSERT INTO discounts (code, name, discount_type, discount_percentage, min_persons_count, valid_from) VALUES
('GROUP_5', 'Group discount 5 persons', 'GROUP', 10, 5, '2025-01-01'),
('GROUP_10', 'Group discount 10 persons', 'GROUP', 15, 10, '2025-01-01'),
('CORPORATE', 'Corporate discount', 'CORPORATE', 20, 1, '2025-01-01'),
('LOYALTY_5', 'Loyalty 5%', 'LOYALTY', 5, 1, '2025-01-01'),
('LOYALTY_10', 'Loyalty 10%', 'LOYALTY', 10, 1, '2025-01-01');

-- ============================================

-- Таблица применения скидок
CREATE TABLE agreement_discounts (
    id BIGSERIAL PRIMARY KEY,
    agreement_id BIGINT NOT NULL REFERENCES agreements(id) ON DELETE CASCADE,
    discount_id BIGINT NOT NULL REFERENCES discounts(id),
    discount_amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_agreement_discount ON agreement_discounts(agreement_id);

-- ============================================

-- Таблица платежей
CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    agreement_id BIGINT NOT NULL REFERENCES agreements(id),

    -- Сумма
    amount DECIMAL(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'EUR',

    -- Статус
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, SUCCESS, FAILED, REFUNDED

    -- Платежная система
    payment_method VARCHAR(50), -- CARD, PAYPAL, BANK_TRANSFER
    payment_provider VARCHAR(50), -- STRIPE, PAYPAL, etc.
    external_transaction_id VARCHAR(100),

    -- Даты
    paid_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_payment_agreement ON payments(agreement_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_external_id ON payments(external_transaction_id);

-- ============================================
-- 3. АУДИТ И ЛОГИРОВАНИЕ
-- ============================================

-- Таблица логов расчетов
CREATE TABLE calculation_logs (
    id BIGSERIAL PRIMARY KEY,
    request_id UUID NOT NULL UNIQUE,

    -- Входные данные
    request_data JSONB NOT NULL,

    -- Результат
    response_data JSONB,
    calculation_details JSONB,

    -- Ошибки
    has_errors BOOLEAN DEFAULT FALSE,
    errors JSONB,

    -- Время выполнения
    execution_time_ms INT,

    -- IP и user agent
    client_ip VARCHAR(45),
    user_agent TEXT,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_calc_log_request_id ON calculation_logs(request_id);
CREATE INDEX idx_calc_log_created ON calculation_logs(created_at);
CREATE INDEX idx_calc_log_errors ON calculation_logs(has_errors);

-- ============================================

-- Таблица истории изменений коэффициентов (audit trail)
CREATE TABLE coefficient_changes_audit (
    id BIGSERIAL PRIMARY KEY,
    table_name VARCHAR(100) NOT NULL,
    record_id BIGINT NOT NULL,

    -- Изменения
    old_value JSONB,
    new_value JSONB,

    -- Кто и когда
    changed_by VARCHAR(100),
    changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    reason TEXT
);

CREATE INDEX idx_audit_table ON coefficient_changes_audit(table_name, record_id);
CREATE INDEX idx_audit_date ON coefficient_changes_audit(changed_at);

-- ============================================
-- 4. ПРЕДСТАВЛЕНИЯ (VIEWS)
-- ============================================

-- Представление активных договоров с деталями
CREATE VIEW v_active_agreements AS
SELECT
    a.id,
    a.agreement_number,
    p.first_name,
    p.last_name,
    p.birth_date,
    EXTRACT(YEAR FROM AGE(p.birth_date)) AS age,
    a.date_from,
    a.date_to,
    a.days_count,
    c.name_en AS country_name,
    c.risk_coefficient AS country_risk,
    a.medical_risk_limit_level,
    a.premium_amount,
    a.currency,
    a.status,
    a.created_at
FROM agreements a
JOIN persons p ON a.person_id = p.id
JOIN countries c ON a.country_iso_code = c.iso_code
WHERE a.status = 'ACTIVE'
  AND a.date_to >= CURRENT_DATE;

-- ============================================

-- Представление статистики по странам
CREATE VIEW v_country_statistics AS
SELECT
    c.iso_code,
    c.name_en,
    c.risk_group,
    COUNT(a.id) AS agreements_count,
    SUM(a.premium_amount) AS total_premium,
    AVG(a.premium_amount) AS avg_premium,
    AVG(a.days_count) AS avg_days
FROM countries c
LEFT JOIN agreements a ON c.iso_code = a.country_iso_code
GROUP BY c.iso_code, c.name_en, c.risk_group;

-- ============================================

-- Представление использования промо-кодов
CREATE VIEW v_promo_code_statistics AS
SELECT
    pc.code,
    pc.description,
    pc.discount_type,
    pc.discount_value,
    pc.max_usage_count,
    pc.current_usage_count,
    COUNT(pcu.id) AS actual_usage_count,
    SUM(pcu.discount_amount) AS total_discount_given,
    pc.valid_from,
    pc.valid_to,
    pc.is_active
FROM promo_codes pc
LEFT JOIN promo_code_usage pcu ON pc.id = pcu.promo_code_id
GROUP BY pc.id, pc.code, pc.description, pc.discount_type,
         pc.discount_value, pc.max_usage_count, pc.current_usage_count,
         pc.valid_from, pc.valid_to, pc.is_active;

-- ============================================
-- 5. ФУНКЦИИ И ТРИГГЕРЫ
-- ============================================

-- Функция автоматического обновления updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Применяем триггеры ко всем таблицам с updated_at
CREATE TRIGGER update_persons_updated_at BEFORE UPDATE ON persons
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_agreements_updated_at BEFORE UPDATE ON agreements
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_promo_codes_updated_at BEFORE UPDATE ON promo_codes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_countries_updated_at BEFORE UPDATE ON countries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================

-- Функция валидации применения промо-кода
CREATE OR REPLACE FUNCTION validate_promo_code(
    p_code VARCHAR,
    p_agreement_date DATE,
    p_premium_amount DECIMAL
)
RETURNS TABLE(is_valid BOOLEAN, error_message TEXT, discount_amount DECIMAL) AS $$
DECLARE
    v_promo promo_codes%ROWTYPE;
    v_discount DECIMAL;
BEGIN
    -- Поиск промо-кода
    SELECT * INTO v_promo
    FROM promo_codes
    WHERE code = p_code
      AND is_active = TRUE
      AND valid_from <= p_agreement_date
      AND valid_to >= p_agreement_date;

    IF NOT FOUND THEN
        RETURN QUERY SELECT FALSE, 'Promo code not found or expired', 0::DECIMAL;
        RETURN;
    END IF;

    -- Проверка лимита использований
    IF v_promo.max_usage_count IS NOT NULL
       AND v_promo.current_usage_count >= v_promo.max_usage_count THEN
        RETURN QUERY SELECT FALSE, 'Promo code usage limit reached', 0::DECIMAL;
        RETURN;
    END IF;

    -- Проверка минимальной суммы
    IF v_promo.min_premium_amount IS NOT NULL
       AND p_premium_amount < v_promo.min_premium_amount THEN
        RETURN QUERY SELECT FALSE, 'Premium amount too low for this promo code', 0::DECIMAL;
        RETURN;
    END IF;

    -- Расчет скидки
    IF v_promo.discount_type = 'PERCENTAGE' THEN
        v_discount := p_premium_amount * v_promo.discount_value / 100;
    ELSE
        v_discount := v_promo.discount_value;
    END IF;

    -- Проверка максимальной скидки
    IF v_promo.max_discount_amount IS NOT NULL
       AND v_discount > v_promo.max_discount_amount THEN
        v_discount := v_promo.max_discount_amount;
    END IF;

    RETURN QUERY SELECT TRUE, NULL::TEXT, v_discount;
END;
$$ LANGUAGE plpgsql;

-- ============================================
-- 6. ИНДЕКСЫ ДЛЯ ПРОИЗВОДИТЕЛЬНОСТИ
-- ============================================

-- Составные индексы для частых запросов
CREATE INDEX idx_agreement_person_dates ON agreements(person_id, date_from, date_to);
CREATE INDEX idx_agreement_country_status ON agreements(country_iso_code, status);

-- Индексы для JSONB полей
CREATE INDEX idx_agreement_calculation_details ON agreements USING GIN (calculation_details);
CREATE INDEX idx_calc_log_request_data ON calculation_logs USING GIN (request_data);

-- ============================================
-- 7. КОММЕНТАРИИ
-- ============================================

COMMENT ON TABLE agreements IS 'Договоры страхования';
COMMENT ON TABLE persons IS 'Застрахованные лица';
COMMENT ON TABLE promo_codes IS 'Промо-коды для скидок';
COMMENT ON TABLE countries IS 'Справочник стран с коэффициентами риска';
COMMENT ON TABLE risk_types IS 'Типы страховых рисков';
COMMENT ON TABLE payments IS 'Платежи по договорам';

-- ============================================
-- КОНЕЦ СХЕМЫ
-- ============================================