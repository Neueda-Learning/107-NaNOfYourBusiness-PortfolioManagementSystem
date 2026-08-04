# CI/CD Plan

## Objective
Automate build, test, and deployment checks so the team can deliver frequent, low-risk updates.

## Branching Strategy
- `main`: releasable branch.
- `feature/*`: short-lived branches for tasks.
- Pull request required before merge to `main`.

## CI Pipeline Stages
1. **Checkout + Setup**
   - Use Maven wrapper (`mvnw`) and Java 17.
2. **Build**
   - `./mvnw -q -DskipTests package` (Linux runners) or equivalent on Windows.
3. **Test**
   - `./mvnw test` including unit tests.
   - Optional integration test stage for merge-to-main only.
4. **Static Quality Gates**
   - Optional: Checkstyle/SpotBugs/PMD.
5. **Artifact Publish**
   - Store JAR and/or Docker image with commit SHA tag.

## CD Pipeline Stages (for demo/prod-like env)
1. Pull built artifact/image.
2. Inject environment-specific config/secrets.
3. Deploy service.
4. Run smoke tests (`/actuator/health`, basic CRUD GET).
5. Mark release successful or trigger rollback.

## Quality Gates
- All required tests pass.
- No high-severity vulnerability findings in dependencies.
- API contract updates reviewed when endpoint changes occur.

## Release Checklist
- [ ] `API-contract.md` aligned with implementation.
- [ ] Testing docs updated for any new behavior.
- [ ] Deployment variables validated for target environment.
- [ ] Rollback procedure tested recently.

## Example Minimal CI Commands
```powershell
.\mvnw -DskipTests package
.\mvnw test
```

## Notes
- Keep pipeline fast for PR feedback; move heavier tests to scheduled or merge pipelines.
- If using GitHub Actions, keep workflow YAML in `.github/workflows/`.

