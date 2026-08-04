# Developer Guide

## Purpose
Quick onboarding guide for contributors working on the Portfolio Manager backend/frontend.

## Prerequisites
- Java 17+
- Maven (or use wrapper `mvnw`)
- MySQL 8
- Git
- Optional: Docker Desktop

## Local Setup
1. Clone repository.
2. Configure DB and external API variables.
3. Start MySQL.
4. Start Spring Boot app.

### One-time Local DB Password Setup (No per-run env export)
Create a repo-root `.env` file once and keep local-only values there.

```dotenv
SPRING_DATASOURCE_PASSWORD=your_local_password
```

`application.properties` imports this file with `spring.config.import=optional:file:.env[.properties]`, so local runs pick it up automatically.
Environment variables still override `.env` values when set (CI/prod behavior unchanged).

```powershell
.\mvnw spring-boot:run
```

## Recommended Environment Variables
- `SPRING_DATASOURCE_URL`
- `SPRING_DATASOURCE_USERNAME`
- `SPRING_DATASOURCE_PASSWORD`
- `MARKET_API_BASE_URL`

## Project Structure (Current)
- Backend Java sources: `src/main/java/com/example/portfolio`
- App config: `src/main/resources/application.properties`
- Documentation root: `project_docs/`

## Development Workflow
1. Create feature branch.
2. Implement one small change at a time.
3. Keep `API-contract.md` updated before changing endpoint behavior.
4. Add/adjust tests.
5. Open PR with linked requirements and test evidence.

## Coding and Design Notes
- Keep MVP model simple (`STOCK`, `BOND`, `MUTUAL_FUND`).
- Treat SIP and Real Estate as phase-2 extension unless explicitly promoted to MVP.
- Use service layer for business calculations and fallback logic.
- Keep repository SQL explicit and reviewed.

## Testing Commands
```powershell
.\mvnw test
.\mvnw verify
```

## Contribution Checklist
- [ ] API contract still accurate.
- [ ] Tests added/updated.
- [ ] Docs updated in affected folder.
- [ ] No hardcoded credentials.

## Change Traceability Checklist
- [ ] US-12: local setup/documentation remains clear for API consumers and contributors.
- [ ] NFR-10: secrets remain externalized in config (`.env`) rather than Java source.
- [ ] NFR-17: local development works without repeating shell setup each run.
- [ ] NFR-19: docs reflect the active local configuration approach.

