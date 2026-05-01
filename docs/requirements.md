# VehicleGuard AI — Requirements Document
**Author:** Lakshmi Pratyusha Tummuri | April 2026 | v2.0

> v2.0 reflects actual decisions and pivots made during the BMAD build session. Original v1.0 planned items are marked where changed.

---

## 1. Product Overview

VehicleGuard AI is a vehicle insurance risk assessment platform built using the BMAD (Breakthrough Method of Agile AI-driven Development) framework. The platform takes structured driver and vehicle inputs, applies a configurable risk scoring engine, and returns a calculated premium estimate with an AI-generated plain-English explanation.

**Core Value:** Turn a black-box insurance premium into a transparent, explainable, AI-powered experience for the customer.

**Live URL:** https://dhr91rzgdav9i.cloudfront.net

---

## 2. Problem Statement

- Traditional vehicle insurance quoting is opaque — customers receive a number with no explanation
- Agents manually assess risk without a consistent scoring framework
- No conversational interface for customers to get guided quotes

---

## 3. In-Scope Features

### 3.1 Risk Assessment API (rates-service + risk-engine)
- Accept driver inputs: age, license years, violations, accidents
- Accept vehicle inputs: make, model, year, category (auto-detected from model name)
- Accept location input: ZIP code
- Accept coverage type: liability, collision, comprehensive, full
- Apply configurable rule-based scoring engine from PostgreSQL (14 risk factors)
- Call rates-service to get base premium by vehicle category
- Return: risk score, risk tier, annual premium, monthly premium, applied factors breakdown
- Call Claude API to generate plain-English premium explanation
- Persist every quote to PostgreSQL with full input/output payload

### 3.2 Chatbot (risk-engine)
- Conversational quote flow — collects all inputs one at a time
- Detects SUBMIT_QUOTE JSON intent — calls risk assessment automatically
- Answers follow-up questions about premium using Claude API
- Full conversation history passed per call — stateless on server
- **Actual fix:** Claude returned JSON wrapped in markdown backticks — fixed by stripping backticks before regex matching in ChatbotService

### 3.3 React Frontend
- Quote Form page — all input fields, coverage type selector
- Quote Results page — risk score, premium, factor breakdown, AI explanation
- Chat Panel — full conversational UI, auto-focus after each message
- **Actual fix:** Autofocus added via inputRef + useEffect on loading state change

---

## 4. Out of Scope
- User authentication / login
- Real payment gateway
- Multi-tenant support
- Claims processing
- Policy document generation
- SMS / push notifications
- Analytics dashboard (planned for future)
- Custom quote search by name/DOB (planned for future)

---

## 5. Architecture

**Pattern:** 3-service microservices architecture deployed locally via Docker Compose and on AWS for production.

| Service | Port (Local) | AWS Target | Responsibility |
|---------|-------------|------------|---------------|
| rates-service | 8081 | AWS ECS Fargate | Returns base premium by vehicle category |
| risk-engine | 8080 | AWS ECS Fargate | Scoring, Claude AI explanation, chatbot, quote persistence |
| frontend | 3000 | AWS S3 + CloudFront | React + TypeScript + Ant Design — quote form, results, chatbot UI |
| postgres | 5432 | AWS RDS PostgreSQL 16 | risk_rules schema + quotes schema |

**Note:** Original plan used EC2. **Actual deployment used ECS Fargate** — no server management needed, better for containerized workloads.

---

## 6. Tech Stack

| Layer | Technology | Notes |
|-------|-----------|-------|
| Backend | Java 21 + Spring Boot 3.2 | Two services — rates-service + risk-engine |
| Frontend | React 18 + TypeScript + Ant Design 5 | Single page app |
| Database | PostgreSQL 16 | Two schemas — risk_rules + quotes |
| AI | Claude API (claude-sonnet-4-5-20250929) | **Changed from** claude-sonnet-4-20250514 — model not available |
| Migrations | Flyway | Separate history tables per service to avoid conflicts |
| Containers | Docker + Docker Compose | Local build and testing |
| CI/CD | GitHub Actions | Build and test pipeline |
| Cloud | AWS ECS Fargate + RDS + ECR + S3 + CloudFront + ALB | Free tier |
| Dev Tools | Claude Code CLI + GitHub Copilot | BMAD AI-driven development |

---

## 7. Development Approach — BMAD

Built using BMAD (Breakthrough Method of Agile AI-Driven Development) with 4 specialised AI agents:

| Agent Role | Tool | Responsibility |
|-----------|------|---------------|
| Product Owner | Pratyusha Tummuri | Scope, decisions, validation, direction |
| System Architect | Claude.ai main chat | Design, Master Prompt, architecture, all docs |
| Developer Agent | Claude Code CLI | Generated all code autonomously from Master Prompt |
| Inline Assistant | GitHub Copilot | Inline code completion in IntelliJ + VS Code |

**Result:** 50+ files across 3 microservices generated in one autonomous build session. 6-week project compressed into 2 days.

---

## 8. AI Integration

### 8.1 Premium Explanation
- Triggered after every risk assessment calculation
- System prompt: Senior insurance underwriter persona
- Input: Full score breakdown with applied factors
- Output: 2-3 sentence plain-English explanation
- Max tokens: 512

### 8.2 Chatbot
- Collects all 9 required inputs conversationally (one question at a time)
- Detects SUBMIT_QUOTE JSON action — calls risk assessment
- Answers follow-up questions using quote context
- Max tokens: 1024 — stateless server side
- **Multi-turn:** Full conversation history sent to Claude on every message via callClaudeWithHistory()
- **Key fix:** System prompt updated to explicitly say "respond with ONLY raw JSON" to prevent markdown wrapping

---

## 9. Data Design

### Schema: risk_rules
- `risk_factors` — 14 configurable scoring rules (synthetic seed data matching realistic insurance rates)
- `vehicle_base_rates` — base premium by vehicle category

**Note:** Original plan was to use Kaggle CSV data directly. **Actual decision:** Used realistic hardcoded seed data instead — simpler, cleaner, demo-ready, same result.

### Schema: quotes
- `quote_requests` — full input + output per quote
- UUID primary key returned to user
- applied_factors_json (JSONB) — every triggered rule stored as JSON snapshot
- ai_explanation (TEXT) — full Claude response stored

### Key fixes during build:
- `id` column type mismatch: Java entity used `Long` but DB had `SERIAL` (INTEGER) — fixed to `Integer` with `@Column(columnDefinition = "serial")`
- `state_code` column type mismatch: DB had `CHAR(2)` but Hibernate expected `VARCHAR` — fixed in V2 migration to use `VARCHAR(2)`
- Flyway conflict on shared DB: Solved using distinct `flyway.table` per service

---

## 10. AWS Deployment

**Strategy:** Build and test locally on Docker. Deploy to AWS free tier for production demo.

| AWS Service | Usage |
|-------------|-------|
| AWS ECS Fargate | Hosts rates-service + risk-engine (replaces planned EC2) |
| AWS RDS (db.t3.micro) | PostgreSQL 16 — replaces Docker Postgres |
| AWS ECR | Docker image registry — AMD64 images (required for Fargate) |
| AWS S3 | React frontend static build files |
| AWS CloudFront | CDN — serves React app + routes /api/* to ALB |
| AWS ALB | Application Load Balancer — routes to ECS tasks |

**Key deployment decisions:**
- Docker images must be built with `--platform linux/amd64` for ECS Fargate (Mac M3 builds ARM64 by default)
- CloudFront `/api/*` behavior routes API calls to ALB — enables HTTPS for frontend while backend is HTTP
- ALB outbound rules must allow ALL traffic (not just 443) to reach ECS tasks on port 8080
- CORS config must include CloudFront domain in allowed origins

---

## 11. Known Limitations & Future Improvements

- ALB target registration is currently manual — ECS auto-registration had health check issues
- No user authentication
- UI is functional but basic — enterprise dark theme planned
- No analytics dashboard yet
