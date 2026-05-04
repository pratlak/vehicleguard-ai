CREATE SCHEMA IF NOT EXISTS risk_rules;

CREATE TABLE IF NOT EXISTS risk_rules.vehicle_base_rates (
    id SERIAL PRIMARY KEY,
    vehicle_category VARCHAR(50) UNIQUE NOT NULL,
    base_annual_premium DECIMAL(10,2) NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);
