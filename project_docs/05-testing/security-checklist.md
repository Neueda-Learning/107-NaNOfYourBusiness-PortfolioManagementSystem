# Security Checklist

## Scope
Security baseline for a single-user training project without authentication. Focus is on safe input handling, dependency hygiene, and deployment hardening.

## Application Security
- [ ] Validate all incoming request DTO fields with Bean Validation.
- [ ] Reject invalid enum/type values (`AssetType`) with clear `400` errors.
- [ ] Sanitize and bound string fields (`symbolOrName`) to avoid abusive payload sizes.
- [ ] Use parameterized SQL only (`JdbcTemplate` placeholders); no dynamic SQL concatenation.
- [ ] Ensure global exception handler never leaks stack traces or secrets in responses.
- [ ] Return consistent error response shape as defined in `API-contract.md`.

## API Behavior
- [ ] Enforce HTTP method correctness (`GET` read-only, `POST` create, etc.).
- [ ] Add request size limits at server/proxy level.
- [ ] Define simple rate limiting for external quote refresh endpoint to prevent abuse.
- [ ] Configure CORS to only required origins during development.

## Secrets and Configuration
- [ ] Do not commit real DB passwords or API keys.
- [ ] Use environment variables for `spring.datasource.*` credentials.
- [ ] Use environment variables for external market API URLs/keys.
- [ ] Keep `.env`/local override files out of source control.

## Dependency and Build Security
- [ ] Run dependency vulnerability checks regularly.
- [ ] Keep Spring Boot and plugins updated to supported versions.
- [ ] Lock Maven wrapper usage (`mvnw`) in CI to avoid toolchain drift.

## Data Security
- [ ] Use least-privilege DB account (only required schema privileges).
- [ ] Enable DB backups for non-local environments.
- [ ] Ensure deleted records are actually removed if business requires hard delete.

## Operational Security
- [ ] Enable structured logging with correlation IDs where possible.
- [ ] Avoid logging full request payloads that may contain sensitive financial amounts unless needed for debugging.
- [ ] Define incident contacts and escalation path for service downtime.

## Optional Hardening (Phase 2)
- [ ] Add authentication/authorization if app scope expands beyond single-user.
- [ ] Add HTTPS termination and strict transport security headers in hosted environments.
- [ ] Add audit trail for create/update/delete events.

