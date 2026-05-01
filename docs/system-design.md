# VehicleGuard AI — System Design
**Author:** Lakshmi Pratyusha Tummuri | April 2026 | v2.0

> v2.0 reflects actual implementation decisions. Original v1.0 planned a single microservice — actual build uses 3 microservices.

---

## 1. Executive Summary

VehicleGuard AI is a vehicle insurance risk assessment platform built to demonstrate AI-augmented engineering. The platform takes structured driver and vehicle inputs, applies a rule-based risk scoring engine, and returns a calculated premium estimate alongside an AI-generated natural language explanation — answering the critical user question: *"Why is my premium this amount?"*

**Goal:** Demonstrate AI-augmented product thinking, system design depth, and full-stack implementation quality using the BMAD framework.

**Live URL:** https://dhr91rzgdav9i.cloudfront.net

---

## 2. Problem Statement

Traditional vehicle insurance quoting is opaque:
- Customers receive a premium number with no explanation
- Agents manually assess risk without consistent framework
- Creates trust gaps and high drop-off in quote flows

**VehicleGuard AI solves this with:**
1. A deterministic risk scoring engine backed by configurable DB rules — every quote is auditable
2. An AI explanation layer — every score becomes a conversation, not just a number
3. Persistent quote history — every calculation is stored and reviewable

---

## 3. Architecture

### 3.1 High-Level Architecture

```
User
 │
 ▼
CloudFront (HTTPS) ──────────────────────────────────────────────
 │                                                               │
 ▼ /* (default)                                    ▼ /api/* (behavior)
S3 (React Frontend)                          ALB (Load Balancer)
                                                    │
                                                    ▼
                                          ECS Fargate: risk-engine (8080)
                                                    │
                                          ┌─────────┼─────────┐
                                          ▼         ▼         ▼
                                    rates-service  RDS    Claude API
                                    (ECS, 8081)  (PostgreSQL) (Anthropic)
```

### 3.2 3-Service Microservices Architecture

| Service | Port | Technology | Responsibility |
|---------|------|-----------|----------------|
| **rates-service** | 8081 | Spring Boot 3.2 | Returns base premium by vehicle category |
| **risk-engine** | 8080 | Spring Boot 3.2 | Risk scoring, AI explanation, chatbot, quote persistence |
| **frontend** | 3000 | React 18 + TypeScript | Quote form, results page, chatbot UI |
| **postgres** | 5432 | PostgreSQL 16 | risk_rules schema + quotes schema |

**Note:** Original design was single microservice. Pivoted to 3 microservices to demonstrate proper service separation and inter-service communication.

### 3.3 Service Responsibilities

**rates-service:**
- Single purpose: lookup base premium by vehicle category
- Seeded with 6 vehicle categories and realistic base rates
- Called by risk-engine via WebClient (reactive HTTP)
- Fully independent — can be updated without touching risk-engine

**risk-engine:**
- Loads 14 risk factor rules from PostgreSQL at startup
- Evaluates each factor against driver/vehicle inputs
- Calls rates-service for base premium
- Applies risk multipliers to calculate final premium
- Calls Claude API for AI-generated explanation
- Handles multi-turn chatbot conversations
- Persists all quotes to PostgreSQL

**frontend:**
- Quote form with all driver/vehicle/location inputs
- Results page with risk breakdown, premium, and AI explanation
- Chat panel with conversational quote flow
- Calls risk-engine only — never directly to rates-service

---

## 4. Data Design

### 4.1 Schema Overview

One PostgreSQL database with two logical schemas, keeping risk rules separate from quote data. This mirrors real-world patterns where rules are maintained by underwriting teams independently of transactional data.

### 4.2 Schema: risk_rules

**Table: risk_factors** — configurable scoring rules

| Column | Type | Description |
|--------|------|-------------|
| id | SERIAL PRIMARY KEY | Auto-generated |
| factor_key | VARCHAR(100) UNIQUE | Machine-readable name e.g. `driver_age_under_25` |
| factor_label | VARCHAR(200) | Human-readable label for UI |
| category | VARCHAR(50) | driver / vehicle / location / coverage |
| base_score_impact | DECIMAL(5,2) | Points added to risk score when triggered |
| premium_multiplier | DECIMAL(4,3) | Multiplier applied to base premium |
| is_active | BOOLEAN | Whether rule is enforced |
| description | TEXT | Why this rule exists |

**Table: vehicle_base_rates** — base premium by vehicle category

| Column | Type | Description |
|--------|------|-------------|
| id | SERIAL PRIMARY KEY | Auto-generated |
| vehicle_category | VARCHAR(50) UNIQUE | sedan, suv, truck, sports, luxury, electric |
| base_annual_premium | DECIMAL(10,2) | Starting premium before risk adjustments |
| description | TEXT | Classification criteria |

### 4.3 Schema: quotes

**Table: quote_requests** — every generated quote

| Column | Type | Description |
|--------|------|-------------|
| id | UUID PRIMARY KEY | Unique quote ID returned to user |
| created_at | TIMESTAMP | Quote generation time |
| driver_age | INTEGER | Age of primary driver |
| license_years | INTEGER | Years licensed |
| violations_last_5yr | INTEGER | Traffic violations in past 5 years |
| accidents_last_5yr | INTEGER | At-fault accidents in past 5 years |
| vehicle_make | VARCHAR(100) | e.g. Ford |
| vehicle_model | VARCHAR(100) | e.g. Mustang |
| vehicle_year | INTEGER | e.g. 2021 |
| vehicle_category | VARCHAR(50) | Auto-detected from model name |
| zip_code | VARCHAR(10) | Driver's ZIP code |
| state_code | VARCHAR(2) | Derived from ZIP |
| coverage_type | VARCHAR(20) | LIABILITY/COLLISION/COMPREHENSIVE/FULL |
| risk_score | DECIMAL(5,2) | 0-100 normalized score |
| risk_tier | VARCHAR(20) | LOW/MEDIUM/HIGH/VERY_HIGH |
| annual_premium_usd | DECIMAL(10,2) | Calculated annual premium |
| monthly_premium_usd | DECIMAL(10,2) | annual / 12 |
| applied_factors_json | JSONB | All triggered risk factors as JSON snapshot |
| ai_explanation | TEXT | Full Claude API response |
| input_payload_json | JSONB | Original request for audit trail |

**Why JSONB for applied_factors?**
If a risk rule changes in the future, historical quotes still show the exact rules that applied at quote time. Foreign key joins would not preserve this historical accuracy.

---

## 5. Risk Scoring Algorithm

```
1. Load all active risk_factors from PostgreSQL (cached at startup via @PostConstruct)

2. Evaluate 14 conditions against request:
   - driver_age_under_25:  driverAge < 25         → +15 pts, ×1.25
   - driver_age_over_70:   driverAge > 70          → +10 pts, ×1.15
   - license_under_2yr:    licenseYears < 2        → +12 pts, ×1.20
   - violation_count_1:    violations == 1         → +8 pts,  ×1.10
   - violation_count_2plus: violations >= 2        → +18 pts, ×1.30
   - accident_count_1:     accidents == 1          → +10 pts, ×1.15
   - accident_count_2plus: accidents >= 2          → +22 pts, ×1.45
   - vehicle_sports:       category == "sports"    → +12 pts, ×1.20
   - vehicle_luxury:       category == "luxury"    → +8 pts,  ×1.15
   - vehicle_age_over_15:  (year - vehicleYear) > 15 → +6 pts, ×1.10
   - coverage_comprehensive: coverageType == COMPREHENSIVE → ×1.30
   - coverage_collision:   coverageType == COLLISION      → ×1.15
   - coverage_full:        coverageType == FULL           → ×1.50
   - high_density_zip:     ZIP in high-density list       → +5 pts, ×1.08

3. raw_score = sum of all triggered base_score_impact values
   MAX_POSSIBLE_SCORE = 90.0

4. normalized_score = (raw_score / MAX_POSSIBLE_SCORE) * 100

5. Call rates-service: GET /rates?vehicleCategory={category}
   → Returns base_annual_premium

6. Apply multipliers sequentially:
   final_premium = base_rate × mult1 × mult2 × ... × multn

7. Assign risk tier:
   0-25   → LOW
   26-50  → MEDIUM
   51-75  → HIGH
   76+    → VERY_HIGH

8. Call Claude API → AI explanation
9. Save to PostgreSQL
10. Return AssessResponse
```

---

## 6. AI Integration

### 6.1 Premium Explanation (Single-turn)
- Called once per quote after risk scoring
- System prompt: Senior insurance underwriter persona
- Input: Risk score, tier, driver info, applied factors
- Output: 2-3 sentence plain-English explanation
- Model: claude-sonnet-4-5-20250929
- Max tokens: 512

### 6.2 Chatbot (Multi-turn)
- Collects 9 required fields conversationally
- Full conversation history sent on every API call (stateless server-side)
- Detects SUBMIT_QUOTE JSON action and calls risk assessment automatically
- Answers follow-up questions using quote context
- Max tokens: 1024

**Key implementation detail:** Claude wraps JSON responses in markdown code blocks. Backend strips backticks before parsing SUBMIT_QUOTE action.

---

## 7. API Design

### rates-service
```
GET  /rates?vehicleCategory=sedan&state=CA
     → { baseAnnualPremium: 1100.00, vehicleCategory: "sedan", state: "CA" }

GET  /actuator/health
```

### risk-engine
```
POST /api/v1/risk/assess
     Request:  { driverAge, licenseYears, violationsLast5Yr, accidentsLast5Yr,
                 vehicleMake, vehicleModel, vehicleYear, zipCode, coverageType }
     Response: { quoteId, riskScore, riskTier, annualPremiumUsd, monthlyPremiumUsd,
                 coverageType, appliedFactors[], aiExplanation, createdAt }

GET  /api/v1/risk/quote/{quoteId}
     → Returns saved quote by ID

POST /api/v1/chat/message
     Request:  { sessionId, message, quoteContext? }
     Response: { reply, sessionId, suggestedActions[], detectedAction?, quoteData? }

GET  /actuator/health
```

---

## 8. AWS Infrastructure

### 8.1 Services Used

| Service | Resource | Purpose |
|---------|----------|---------|
| ECR | vehicleguard-risk-engine | Docker image registry |
| ECR | vehicleguard-rates-service | Docker image registry |
| ECS Fargate | vehicleguard-cluster | Runs both backend services |
| RDS | vehicleguard-db (db.t3.micro) | PostgreSQL 16 managed database |
| S3 | vehicleguard-ai-frontend | Static React build files |
| CloudFront | E2PSR1U07IG0R4 | CDN + HTTPS + API routing |
| ALB | vehicleguard-alb | Load balancer for risk-engine |

### 8.2 CloudFront Routing
- `/*` (default) → S3 (React frontend files)
- `/api/*` (behavior) → ALB → ECS risk-engine

This single CloudFront distribution serves both frontend and API, enabling HTTPS for everything from one URL.

### 8.3 Key Deployment Decisions

| Decision | Reason |
|----------|--------|
| ECS Fargate over EC2 | No server management, auto-scales, better for containers |
| Images built with --platform linux/amd64 | Mac M3 builds ARM64 by default; Fargate requires AMD64 |
| CloudFront /api/* behavior | Enables HTTPS for frontend while backend is HTTP |
| Separate Flyway history tables per service | Avoids migration conflicts on shared PostgreSQL DB |
| Maven -Dmaven.test.skip=true in Dockerfile | Skips test compilation; -DskipTests alone doesn't |

---

## 9. Local Development

### Prerequisites
- Docker Desktop
- Java 21
- Node.js 20+

### Setup
```bash
git clone https://github.com/pratlak/vehicleguard-ai.git
cd vehicleguard-ai
echo "ANTHROPIC_API_KEY=your-key" > .env
docker-compose up
```

### Services
- Frontend: http://localhost:3000
- Risk Engine: http://localhost:8080
- Rates Service: http://localhost:8081
- PostgreSQL: localhost:5432

---

## 10. Security Considerations

- No secrets in code — all via environment variables
- `.env` file in `.gitignore` — never committed
- Anthropic API key stored as ECS task environment variable
- CORS restricted to known origins (localhost:3000, CloudFront domain)
- RDS not publicly accessible except via security group rules
- All production traffic via HTTPS (CloudFront)
