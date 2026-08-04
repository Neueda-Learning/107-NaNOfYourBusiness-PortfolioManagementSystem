# Coding Standards - Portfolio Management System

These standards apply to backend code, tests, and supporting scripts.
Aim for consistency, readability, and contract compliance over cleverness.

## 1. General Principles

- Prefer simple, explicit code over abstraction-heavy patterns.
- Keep methods small and single-purpose.
- Avoid hidden side effects.
- Keep docs, tests, and implementation in sync.

## 2. Java and Spring Conventions

- Java version: 17+
- Use standard package layering: `controller`, `service`, `repository`, `dto`, `model`, `exception`.
- Class names: `PascalCase`; methods/fields: `camelCase`; constants: `UPPER_SNAKE_CASE`.
- Use constructor injection only.
- Avoid field injection.

## 3. DTO and Validation Rules

- Request DTOs must use Bean Validation annotations.
- Always annotate controller request payloads with `@Valid`.
- Validate business rules in service layer when cross-field checks are needed.
- API field names must match `project_docs/03-design/api-contracts.md` exactly.

## 4. Money and Numeric Handling

- Use `BigDecimal` for money and quantity persistence/calculation.
- Never use floating-point types for price math (`float`/`double`) in backend calculations.
- Define rounding mode explicitly where percentages are returned.

## 5. Repository and SQL Standards

- Use `JdbcTemplate` with parameterized queries only.
- Keep SQL readable; prefer multiline text blocks for complex queries.
- No string concatenation for user inputs in SQL.
- Centralize row mapping logic in dedicated mapper classes.
- Keep column names aligned to `database-schema.md`.

## 6. Error Handling Standards

- Use a global exception handler for API errors.
- Return standardized error shape from `api-contracts.md`.
- Map known scenarios:
  - validation -> `400`
  - not found -> `404`
  - external API failure -> `502`
  - unexpected -> `500`
- Never leak secrets, SQL, stack traces, or credentials in responses.

## 7. Logging Standards

- Use structured, concise logs with context (`id`, `type`, operation).
- Log at appropriate levels:
  - `INFO` for lifecycle/important business events
  - `WARN` for recoverable failures
  - `ERROR` for unexpected exceptions
- Do not log sensitive data.

## 8. Testing Standards

- Unit tests for services and pure mapping logic.
- `@WebMvcTest` for controllers.
- Repository tests for SQL behavior (slice test or Testcontainers).
- Minimum required coverage focus:
  - CRUD happy paths
  - validation failures
  - not-found behavior
  - external API fallback path

## 9. API Contract Discipline

- API contract changes require updates in the same PR:
  1. `project_docs/03-design/api-contracts.md`
  2. backend implementation
  3. automated tests
  4. changelog/release notes if used

## 10. Git and Review Guidelines

- Use short-lived feature branches.
- Keep PRs focused and small enough to review.
- Include test evidence in PR description.
- Request at least one teammate review before merge.

## 11. Definition of Done (Code Level)

A task is done when:

- implementation matches design docs,
- tests pass locally,
- no obvious lint/style violations,
- error handling paths are covered,
- required docs are updated.

