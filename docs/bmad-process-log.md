# VehicleGuard AI — BMAD Process Log
**Author:** Lakshmi Pratyusha Tummuri | April 2026 | v4.0

> This document captures every step, decision, pivot, and fix made during the AI-augmented development of VehicleGuard AI using the BMAD framework. Updated to reflect what actually happened vs what was originally planned.

---

## BMAD Role Summary

| Role | Tool / Agent | Steps Involved |
|------|-------------|----------------|
| Product Owner | Pratyusha Tummuri | All steps — scope, validation, direction |
| System Architect | Claude.ai main chat | Design, prompts, architecture, debugging |
| Developer Agent | Claude Code CLI | Autonomous code generation |
| Inline Assistant | GitHub Copilot | Inline code completion |

---

## Phase 1 — Planning (Steps 1–15) ✅ Complete

| # | Action / Decision                                                                     | Notes |
|---|---------------------------------------------------------------------------------------|-------|
| 1 | Uploaded project context — MVP doc, resume, job description                           | Starting point for all planning |
| 2 | Defined project scope — vehicle insurance, risk API + chatbot, 3-service architecture | Core product decision |
| 3 | Generated System Design Document v4 — architecture, DB schema, API spec               | Full system design before any code |
| 4 | Generated Architecture Diagram v2 — 6-lane flow with AWS services                     | Visual architecture reference |
| 5 | Generated UI Mockups — quote form, results page, chatbot conversation flow            | Dark enterprise design — aspirational |
| 6 | Made Docker vs AWS decision — Docker for local, AWS for production                    | Key architectural pivot |
| 7 | Discussed Claude context windows, limits, tool strategy                               | Understanding AI tool constraints |
| 8 | Set up AI tool strategy — Claude main chat + Claude Code + GitHub Copilot             | Multi-agent BMAD setup |
| 9 | Set up physical workspace — 3-screen layout                                           | Productivity optimization |
| 10 | Created document tracking plan — trimmed to 5 essential docs                          | Focus on what matters |
| 11 | Planned confidentiality strategy for all final documents                              | Professional documentation |
| 12 | Set up chat backup system — Tampermonkey + Greasy Fork                                | Context preservation |
| 13 | Created Chrome bookmark folder structure                                              | Build, Tools, Docs, References |
| 14 | Pivoted to BMAD approach — AI-first development to compress 6-week project into 2 days                                                  | Most important strategic decision |
| 15 | Reviewed and updated requirement document — BMAD + AWS approach                       | Baseline for Claude Code |

---

## Phase 2 — Setup (Steps 16–23) ✅ Complete

| # | Action / Decision | Notes                                                  |
|---|------------------|--------------------------------------------------------|
| 16 | Installed GitHub Copilot in IntelliJ + VS Code | Free tier                                              |
| 17 | Created GitHub repo vehicleguard-ai | Public repo                                            |
| 18 | Downloaded Kaggle vehicle insurance dataset | AutoInsuranceClaims2024.csv                            |
| 19 | Installed Node.js via macOS installer | npm 11.12.1                                            |
| 20 | Installed Claude Code CLI via npx | v2.1.1.21 — used npx due to permission issues          |
| 21 | Set Anthropic API key in .env file | Key rotated multiple times for security purposes       |
| 22 | Generated Master Prompt v2 for Claude Code | Complete spec — 422 lines — includes integration tests |
| 23 | Pasted Master Prompt into Claude Code — autonomous build begins | Hit token limits — resumed with summary option         |

---

## Phase 3 — Build (Steps 24–29) ✅ Complete

| # | Action / Decision | Notes |
|---|------------------|-------|
| 24 | Claude Code built rates-service | All Java files, migrations, tests generated |
| 25 | Claude Code built risk-engine | Scoring, Claude AI, chatbot, quote persistence |
| 26 | Claude Code built React frontend | Form, results, chatbot UI |
| 27 | Claude Code wrote Flyway migrations V1-V3 + seed data | Synthetic seed data used instead of Kaggle CSV |
| 28 | Claude Code wrote GitHub Actions CI/CD pipeline | Build + test pipeline |
| 29 | Fixed ChatbotServiceTest + ChatbotIntegrationTest | callClaudeAPI → callClaudeWithHistory mock update |

---

## Phase 4 — Local Testing + Fixes (Steps 30–38) ✅ Complete

| # | Action / Decision | Notes |
|---|------------------|-------|
| 30 | Installed Docker Desktop for Mac | M3 Pro — Apple Silicon |
| 31 | Fixed pom.xml — flyway-database-postgresql missing version | Added version 10.10.0 to both services |
| 32 | Fixed Dockerfile — changed -DskipTests to -Dmaven.test.skip=true | Skip test compilation, not just execution |
| 33 | Fixed VehicleBaseRate.java — id type Long → Integer with serial columnDefinition | Hibernate schema validation fix |
| 34 | Fixed QuoteRequest.java — state_code CHAR(2) → VARCHAR(2) in migration | Type mismatch fix |
| 35 | Fixed docker-compose.yml — wget health check instead of curl | Alpine Linux doesn't have curl |
| 36 | Added start_period: 60s to health check | Give Spring Boot time to start |
| 37 | docker-compose up — all 4 containers running | First successful local run |
| 38 | Tested quote form in browser — working ✅ | Risk score, premium, AI explanation all correct |

---

## Phase 5 — Chatbot Fixes (Steps 39–44) ✅ Complete

| # | Action / Decision | Notes |
|---|------------------|-------|
| 39 | Chatbot error — Claude API returning 400 | Missing anthropic-version header — already present, actual cause was wrong model name |
| 40 | Changed model from claude-sonnet-4-20250514 to claude-sonnet-4-5-20250929 | Original model didn't exist in API |
| 41 | Added credits to Anthropic API account | $5 added — API account separate from Claude.ai subscription |
| 42 | Fixed SUBMIT_QUOTE detection — regex not matching nested JSON | Rewrote detectAndParseSubmitQuote() to strip markdown backticks before parsing |
| 43 | Updated chatbot system prompt — explicit JSON-only instruction | "respond with ONLY raw JSON, no markdown formatting" |
| 44 | Fixed chatbot premium display in chat | Added premium result message after SUBMIT_QUOTE detected |
| 45 | Added autofocus to chat input | inputRef + useEffect on loading state |
| 46 | Pushed all code to GitHub | API key accidentally exposed — rotated key |
| 47 | Removed .env from git tracking | Added to .gitignore, used git filter-branch to clean history |

---

## Phase 6 — AWS Deployment (Steps 48–62) ✅ Complete

| # | Action / Decision | Notes |
|---|------------------|-------|
| 48 | Created AWS account | us-east-2 (Ohio) region |
| 49 | Created IAM user vehicleguard-deploy | Attached ECR, ECS, RDS, S3, CloudFront, ELB policies |
| 50 | Configured AWS CLI | aws configure with IAM user credentials |
| 51 | Created ECR repositories | vehicleguard-risk-engine + vehicleguard-rates-service |
| 52 | Built and pushed Docker images to ECR | First attempt failed — Mac M3 builds ARM64, Fargate needs AMD64 |
| 53 | Rebuilt with --platform linux/amd64 | Required for ECS Fargate compatibility |
| 54 | Created RDS PostgreSQL 16 db.t3.micro | vehicleguard-db — free tier |
| 55 | Created vehicleguard database on RDS | Connected via psql with SSL |
| 56 | Created ECS cluster vehicleguard-cluster | Fargate serverless |
| 57 | Created ECS task definitions | rates-service (port 8081) + risk-engine (port 8080) |
| 58 | Deployed both ECS services | 1/1 running — both healthy |
| 59 | Tested APIs directly via curl | Both services UP, RDS connected |
| 60 | Updated RATES_SERVICE_URL in risk-engine | Changed from localhost to rates-service ECS public IP |
| 61 | Built React frontend for production | npm run build |
| 62 | Created S3 bucket + enabled static hosting | vehicleguard-ai-frontend |
| 63 | Created CloudFront distribution | dhr91rzgdav9i.cloudfront.net |
| 64 | Added ALB for stable backend URL | vehicleguard-alb — long debugging session |
| 65 | Fixed ALB health check — outbound rules | ALB SG outbound must allow ALL traffic to ECS |
| 66 | Added CloudFront /api/* behavior | Routes API calls to ALB — enables HTTPS + routing |
| 67 | Added CloudFront domain to CORS config | allowedOrigins includes dhr91rzgdav9i.cloudfront.net |
| 68 | Rebuilt + redeployed frontend with /api/v1 base URL | Relative URL works with CloudFront proxy |
| 69 | Full stack tested on AWS ✅ | Both quote form and chatbot working live |
| 70 | Pushed final code to GitHub | .env and AWSCLIV2.pkg excluded |

---

## Phase 7 — Docs + Demo Prep (Steps 71+) ⏳ In Progress

| # | Action / Decision | Status |
|---|------------------|--------|
| 71 | Generated README.md | ✅ Done |
| 72 | Generated requirements.md (this doc updated) | ✅ Done |
| 73 | Generated BMAD process log (this doc) | ✅ Done |
| 74 | Generated master-prompt.md | ✅ Done |
| 75 | Generated prompt-engineering.md | ✅ Done |
| 76 | Fix unit tests | ⏳ Pending |
| 77 | Fix GitHub Actions pipeline | ⏳ Pending |
| 78 | Take demo screenshots | ⏳ Pending |

---

## Key Decisions Made During Build

| Decision | Original Plan | Actual Decision | Reason |
|----------|--------------|-----------------|--------|
| AWS compute | EC2 t2.micro | ECS Fargate | No server management, better for containers |
| Kaggle data | Parse CSV into DB | Synthetic seed data | Cleaner, demo-ready, same result |
| Claude model | claude-sonnet-4-20250514 | claude-sonnet-4-5-20250929 | Original model didn't exist in API |
| Test build | -DskipTests | -Dmaven.test.skip=true | Skip compilation too, not just execution |
| SUBMIT_QUOTE detection | Regex on raw response | Strip markdown backticks first | Claude wraps JSON in code blocks |
| ALB registration | ECS auto-register | Manual IP script | Auto-registration health check issues |
| Secrets Manager | Planned | Skipped | Environment variables sufficient for demo |

---

## Challenges + How They Were Solved

| Challenge | Root Cause | Fix |
|-----------|-----------|-----|
| Docker build fails | Maven test compilation errors | -Dmaven.test.skip=true |
| Hibernate schema validation fails | SERIAL vs BIGINT type mismatch | Changed Java entity id to Integer |
| Chatbot not detecting SUBMIT_QUOTE | Claude wraps JSON in markdown backticks | Strip backticks before parsing |
| ECS images won't pull | Mac M3 builds ARM64, Fargate needs AMD64 | --platform linux/amd64 flag |
| ALB health check times out | ALB SG outbound rules blocked | Allow ALL traffic outbound from ALB SG |
| CORS error on CloudFront | CloudFront domain not in allowed origins | Added to WebClientConfig.java |
| API key exposed in chat | Pasted key directly in conversation | Rotated key, added .env to .gitignore |
