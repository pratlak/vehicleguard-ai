# VehicleGuard AI — Prompt Engineering Document
**Author:** Lakshmi Pratyusha Tummuri | April 2026

> This document captures all AI prompts used during the BMAD build, decisions made, iterations, and what worked vs what didn't.

---

## 1. Master Prompt — Claude Code (Developer Agent)

The Master Prompt was the single most important artifact in the BMAD process. It was given to Claude Code CLI to autonomously generate all 50+ files across 3 microservices.

**File:** [master-prompt.md](master-prompt.md)

**Key design decisions in the prompt:**
- Specified exact folder structure for all 3 services
- Included complete DB schema with column types
- Specified exact risk scoring algorithm with 14 factors
- Included sample request/response JSON for all endpoints
- Specified test classes to generate (unit + integration)
- Gave explicit ordering — "START BY DOING THIS IN ORDER"

**What worked:**
- Extremely detailed spec = very accurate code generation
- Including example JSON responses = correct DTO structures
- Specifying folder structure = correct package organization
- Ordering the build steps = logical file dependency resolution

**What didn't work:**
- Model name `claude-sonnet-4-20250514` didn't exist — had to change to `claude-sonnet-4-5-20250929`
- Test mocks for WebClient were incorrect — needed manual fixes
- SUBMIT_QUOTE system prompt didn't prevent markdown wrapping of JSON

---

## 2. AI Explanation Prompt (Production)

Used in `AIExplanationService.java` to generate the premium explanation shown to users.

### System Prompt
```
You are a senior auto insurance underwriter. Given a risk assessment breakdown, 
write a clear, professional 2-3 sentence explanation for a customer explaining 
which factors most influenced their premium and why those factors matter from a 
risk perspective. Never use internal factor keys. Use plain English only. 
Do not mention exact dollar amounts.
```

### User Message Format
```
Risk Assessment:
- Risk Score: {score}/100
- Risk Tier: {tier}
- Driver Age: {age}
- Years Licensed: {years}
- Vehicle: {make} {model} ({year})
- Coverage Type: {coverage}

Applied Risk Factors:
- {factor_label} (score impact: {impact})
...

Please explain these risk factors to the customer in 2-3 sentences.
```

### Why this worked:
- "Never use internal factor keys" — prevents ugly keys like `driver_age_under_25` in output
- "Do not mention exact dollar amounts" — keeps explanation qualitative and honest
- "Plain English only" — prevents technical jargon
- Persona framing ("senior underwriter") = professional tone

---

## 3. Chatbot System Prompt — Iterations

This prompt went through multiple iterations to fix the SUBMIT_QUOTE detection issue.

### Version 1 (Original — didn't work)
```
You are VehicleGuard, a friendly vehicle insurance assistant. Your job is to help 
users get a premium estimate by collecting: driver age, years licensed, violations 
in last 5 years, at-fault accidents in last 5 years, vehicle make, model and year, 
ZIP code, and coverage preference. Ask for one piece of information at a time. 
When all fields are collected, respond with JSON only: 
{"action": "SUBMIT_QUOTE", "data": {...}}
```

**Problem:** Claude wrapped the JSON in markdown code blocks:
````
```json
{"action": "SUBMIT_QUOTE", ...}
```
````
This broke the regex detection on the backend.

### Version 2 (Added explicit instruction)
```
CRITICAL: When you have collected all information and need to submit, respond with 
ONLY raw JSON with no markdown formatting, no code blocks, no backticks. 
Just the plain JSON object.
```

**Problem:** Claude still occasionally used markdown. More reliable but not 100%.

### Version 3 (Final — works reliably)
```
You are VehicleGuard, a friendly vehicle insurance assistant. Your job is to help 
users get a premium estimate by collecting: driver age, years licensed, violations 
in last 5 years, at-fault accidents in last 5 years, vehicle make, model and year, 
ZIP code, and coverage preference (liability/collision/comprehensive/full). 
Ask for one piece of information at a time in a conversational friendly way. 
When all fields are collected, respond with JSON only: 
{"action": "SUBMIT_QUOTE", "data": {"driverAge": <number>, "licenseYears": <number>, 
"violationsLast5Yr": <number>, "accidentsLast5Yr": <number>, "vehicleMake": "<string>", 
"vehicleModel": "<string>", "vehicleYear": <number>, "zipCode": "<string>", 
"coverageType": "<LIABILITY|COLLISION|COMPREHENSIVE|FULL>"}}. 
If user asks why their premium is high, explain clearly using the risk breakdown provided.
```

**Backend fix applied alongside:** Stripped markdown backticks before regex matching in `detectAndParseSubmitQuote()`.

**What worked:**
- Specifying exact JSON field names and types = consistent structure
- Listing valid enum values for coverageType = no invalid values
- Both prompt + backend fix together = reliable detection

---

## 4. System Architect Prompts (ClaudeAI Chat)

All architectural decisions were made through prompts to Claude.ai.

### Key prompts and decisions:

**"Docker vs AWS — which should we build on first?"**
- Decision: Docker for local, AWS for production
- Reason: Faster iteration locally, professional deployment on AWS

**"Should we use Kaggle data directly or synthetic seed data?"**
- Decision: Synthetic seed data
- Reason: Cleaner, faster, demo-ready — Kaggle CSV would need complex ETL

**"EC2 vs ECS Fargate for deployment?"**
- Decision: ECS Fargate
- Reason: No server management, better for containerized microservices, scales automatically

**"Should we use ALB or just expose ECS directly?"**
- Decision: ALB for stable URL
- Reason: ECS task IPs change on restart — ALB provides stable DNS

**"Maven vs Gradle?"**
- Decision: Maven
- Reason: More common, easier to understand, familiar from Amazon experience

---

## 5. Debugging Prompts — What Worked

During debugging sessions, these prompt patterns were most effective:

### Pattern 1: Share exact error + ask for root cause
```
Here is the exact error from docker-compose logs:
[paste error]
What is the root cause and exact fix?
```

### Pattern 2: Ask to add debug logging before fixing
```
Add a log.info() line right after [specific line] to see what value [variable] 
has at runtime. Then rebuild and share the log output.
```

### Pattern 3: Compare planned vs actual
```
The task definition shows RATES_SERVICE_URL = localhost but it should be the 
ECS IP. Which revision is actually running?
```

---

## 6. Prompt Engineering Lessons Learned

| Lesson | Details |
|--------|---------|
| Be extremely specific | Vague prompts = hallucinated code. Exact column types, field names, endpoint paths = accurate output |
| Include examples | Sample JSON request/response in the prompt = correct DTO generation |
| Specify ordering | "Do this in order: 1, 2, 3..." = logical dependency resolution |
| Expect iteration | First output rarely perfect — plan for 2-3 iterations on complex components |
| Backend + prompt together | Some issues need both a prompt fix AND a code fix (e.g., SUBMIT_QUOTE) |
| Token limits are real | Large prompts hit limits — design for resumable sessions |
| Model names change | Always verify model exists before using in production code |
| Never paste secrets | API keys in chat = automatic revocation. Always use .env files |
