# Rollback Plan

## Objective
Restore service quickly when a deployment introduces defects, data risks, or unacceptable performance.

## Rollback Triggers
- Health check failures after deployment.
- Error-rate spike above agreed threshold.
- Critical CRUD workflow broken.
- Incorrect portfolio calculations affecting dashboard output.

## Rollback Strategy
1. Keep previous stable artifact/image version tagged and available.
2. Roll back application first.
3. Roll back database schema only when schema changes are incompatible and safe to reverse.

## Procedure
1. Announce incident in team channel and freeze new deployments.
2. Identify last known good release tag.
3. Redeploy previous artifact/container image.
4. Run smoke tests:
   - `GET /actuator/health`
   - `GET /api/v1/portfolio-items`
   - `GET /api/v1/portfolio/summary`
5. Validate dashboard totals against known sample dataset.
6. Document root cause and corrective actions.

## Database Considerations
- Prefer backward-compatible migrations to reduce rollback complexity.
- Take a backup/snapshot before applying schema changes.
- If destructive migration shipped, restore from backup and replay only valid transactions if needed.

## Communication Template
- Incident start time
- Impacted features
- Rollback status
- ETA to recovery
- Post-incident follow-up owner

## Post-Rollback Actions
- Create hotfix task with reproducible failing case.
- Add or extend automated tests to prevent recurrence.
- Update `CHANGELOG.md` and deployment records.

