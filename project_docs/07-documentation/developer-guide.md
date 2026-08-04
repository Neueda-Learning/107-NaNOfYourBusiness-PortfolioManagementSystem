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

