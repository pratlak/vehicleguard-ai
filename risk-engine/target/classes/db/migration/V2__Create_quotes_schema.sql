CREATE SCHEMA IF NOT EXISTS quotes;

CREATE TABLE IF NOT EXISTS quotes.quote_requests (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at          TIMESTAMP DEFAULT NOW(),
    driver_age          INTEGER NOT NULL,
    license_years       INTEGER NOT NULL,
    violations_last_5yr INTEGER NOT NULL,
    accidents_last_5yr  INTEGER NOT NULL,
    vehicle_make        VARCHAR(100),
    vehicle_model       VARCHAR(100),
    vehicle_year        INTEGER,
    vehicle_category    VARCHAR(50),
    zip_code            VARCHAR(10),
    state_code          VARCHAR(2),
    coverage_type       VARCHAR(20),
    risk_score          DECIMAL(5,2),
    risk_tier           VARCHAR(20),
    annual_premium_usd  DECIMAL(10,2),
    monthly_premium_usd DECIMAL(10,2),
    applied_factors_json JSONB,
    ai_explanation      TEXT,
    input_payload_json  JSONB
);
