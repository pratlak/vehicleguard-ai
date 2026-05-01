# VehicleGuard AI — Master Prompt
**Used with:** Claude Code CLI  
**Purpose:** Autonomous generation of all 50+ files across 3 microservices

> This is the exact prompt given to Claude Code CLI to build VehicleGuard AI from scratch using the BMAD Developer Agent approach.

---

```
You are building VehicleGuard AI — a vehicle insurance risk assessment platform. Build this completely from scratch using the exact specifications below.

PROJECT STRUCTURE:
vehicleguard-ai/
├── rates-service/          (Spring Boot - port 8081)
├── risk-engine/            (Spring Boot - port 8080)
├── frontend/               (React - port 3000)
├── docker-compose.yml
├── .github/
│   └── workflows/
│       └── pipeline.yml
└── README.md

TECH STACK:
- Backend: Java 21 + Spring Boot 3.2 + Maven
- Frontend: React 18 + TypeScript + Ant Design 5
- Database: PostgreSQL 16
- ORM: Spring Data JPA + Hibernate
- Migrations: Flyway
- Containers: Docker + Docker Compose
- AI: Anthropic Claude API (claude-sonnet-4-5-20250929)
- HTTP Client: Spring WebClient (reactive, non-blocking)
- Testing: JUnit 5 + Mockito + Spring Boot Test
- Integration Testing: @SpringBootTest + TestContainers (PostgreSQL)
- CI/CD: GitHub Actions

MICROSERVICE 1 - rates-service (port 8081):
[Returns base vehicle insurance premium by vehicle category and state]

MICROSERVICE 2 - risk-engine (port 8080):
[Main service — risk scoring, Claude AI explanation, chatbot, quote persistence]

MICROSERVICE 3 - frontend (port 3000):
[React + TypeScript + Ant Design — quote form, results page, chatbot]

DATABASE SCHEMA:

Schema: risk_rules
- risk_factors table (14 configurable scoring rules)
- vehicle_base_rates table (base premium by vehicle category)

Schema: quotes
- quote_requests table (all generated quotes with full input/output)

RISK SCORING ALGORITHM:
1. Load all active risk_factors from PostgreSQL
2. Evaluate 14 conditions against request inputs
3. Sum triggered base_score_impact values → normalize to 0-100
4. Call rates-service for base premium by vehicle category
5. Apply multipliers: final_premium = base_rate × mult1 × mult2 × multn
6. Assign risk_tier: 0-25=LOW, 26-50=MEDIUM, 51-75=HIGH, 76+=VERY_HIGH
7. Call Claude API for AI explanation
8. Save quote to PostgreSQL
9. Return complete AssessResponse

CLAUDE API INTEGRATION:
- Model: claude-sonnet-4-5-20250929
- AI Explanation: Senior underwriter persona, 2-3 sentences, plain English
- Chatbot: Collect 9 fields one at a time, detect SUBMIT_QUOTE JSON action

[Full 422-line prompt available in VehicleGuard_Master_Prompt_v2.txt]
```

---

## What This Prompt Generated

In a single Claude Code session (with token limit resumes):

| Category | Files Generated |
|----------|----------------|
| Java entities | RiskFactor.java, QuoteRequest.java, VehicleBaseRate.java |
| Java DTOs | AssessRequest, AssessResponse, ChatRequest, ChatResponse, AppliedFactor |
| Java services | RiskScoringService, AIExplanationService, ChatbotService, RatesService |
| Java controllers | RiskController, ChatController, RatesController |
| Java repositories | RiskFactorRepository, QuoteRepository, VehicleBaseRateRepository |
| Java config | WebClientConfig (CORS + WebClient beans) |
| Java exceptions | 4 custom exceptions + 2 GlobalExceptionHandlers |
| Flyway migrations | 5 SQL files (V1-V3 per service) |
| React components | QuoteForm, QuoteResult, ChatPanel, RiskBreakdownCard |
| React API | riskApi.ts, types/index.ts |
| Infrastructure | docker-compose.yml, 3 Dockerfiles, GitHub Actions pipeline |
| Tests | 7 unit test classes + 3 integration test classes |

**Total: 50+ files generated autonomously**

---

## Post-Generation Fixes Required

The prompt was ~95% accurate. These manual fixes were needed:

1. **pom.xml** — Added `<version>10.10.0</version>` to flyway-database-postgresql
2. **Dockerfile** — Changed `-DskipTests` to `-Dmaven.test.skip=true`
3. **VehicleBaseRate.java** — Changed id type from Long to Integer with serial columnDefinition
4. **QuoteRequest.java** — Changed state_code to VARCHAR(2) in migration
5. **ChatbotService.java** — Fixed SUBMIT_QUOTE regex to handle markdown-wrapped JSON
6. **ChatbotService.java** — Updated system prompt to be more explicit about JSON-only response
7. **AIExplanationService.java** — Added callClaudeWithHistory() for multi-turn chatbot
8. **WebClientConfig.java** — Added CloudFront domain to CORS allowed origins
9. **ChatPanel.tsx** — Added inputRef + useEffect for autofocus after each message
10. **ChatPanel.tsx** — Added SUBMIT_QUOTE JSON filtering from display messages
