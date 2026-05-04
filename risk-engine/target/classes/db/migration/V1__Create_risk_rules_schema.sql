CREATE SCHEMA IF NOT EXISTS risk_rules;

CREATE TABLE IF NOT EXISTS risk_rules.risk_factors (
    id                  SERIAL PRIMARY KEY,
    factor_key          VARCHAR(100) UNIQUE NOT NULL,
    factor_label        VARCHAR(200) NOT NULL,
    category            VARCHAR(50) NOT NULL,
    base_score_impact   DECIMAL(5,2) NOT NULL,
    premium_multiplier  DECIMAL(4,3) NOT NULL,
    is_active           BOOLEAN DEFAULT true,
    description         TEXT,
    created_at          TIMESTAMP DEFAULT NOW(),
    updated_at          TIMESTAMP DEFAULT NOW()
);
