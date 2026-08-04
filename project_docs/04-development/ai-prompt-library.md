# AI Prompt Library - Portfolio Management System

Use these prompts to keep generated code aligned with project requirements.
Always provide relevant source docs in context when prompting.

## 1. Prompting Rules

- Include `project_docs/03-design/api-contracts.md` for any endpoint work.
- Include `project_docs/03-design/database-schema.md` for repository/SQL work.
- Ask for one layer at a time (model, repository, service, controller, tests).
- Require generated tests with each code change.
- Reject added fields/endpoints not defined in design docs.

## 2. Backend Implementation Prompts

### 2.1 Generate Model + DTOs

```text
Using `project_docs/03-design/api-contracts.md` and `project_docs/03-design/database-schema.md`, generate Java classes for AssetType enum, PortfolioItem model, PortfolioItemRequest DTO, and PortfolioItemResponse DTO.
Constraints:
- Java 17, Spring Boot 3 style
- Bean Validation on request DTO
- BigDecimal for numeric money values
- No JPA annotations
- Keep field names exactly aligned with API contract
Also generate unit tests for DTO validation where applicable.
```

### 2.2 Generate JDBC Repository

```text
Create a JdbcTemplate-based PortfolioItemRepository for CRUD operations and optional type filter.
Use SQL matching `project_docs/03-design/database-schema.md`.
Include:
- RowMapper
- findAll(type), findById(id), insert, update, deleteById
- not-found handling strategy
- repository tests
Do not use JPA/Hibernate.
```

### 2.3 Generate Service Layer

```text
Generate PortfolioItemService and PortfolioSummaryService based on `project_docs/03-design/api-contracts.md`.
Requirements:
- compute currentValue, gainLoss, gainLossPercent
- enforce business validation beyond annotations if needed
- integrate MarketDataService for refresh-price endpoint only
- graceful fallback when external quote fetch fails
Include JUnit + Mockito tests.
```

### 2.4 Generate REST Controllers + Exception Handler

```text
Using `project_docs/03-design/api-contracts.md`, generate controllers and a global exception handler.
Requirements:
- endpoint paths under /api/v1
- exact status codes and JSON response shapes from contract
- 400 validation errors include fieldErrors
- 404 for missing resource
- 502 for external API failure on refresh endpoint
Include WebMvc tests.
```

## 3. Debugging and Refactor Prompts

### 3.1 Debug failing endpoint

```text
Analyze why this endpoint fails contract compliance.
Inputs:
- controller/service/repository code snippets
- failing test output
- `project_docs/03-design/api-contracts.md`
Return:
1) root cause,
2) minimal code fix,
3) test updates,
4) regression risk notes.
```

### 3.2 Refactor safely

```text
Refactor this service method for readability and maintainability without changing behavior.
Keep method signature, API contract, and response fields unchanged.
Also generate/adjust unit tests to prove behavior did not change.
```

## 4. Documentation Update Prompts

### 4.1 Keep docs and code in sync

```text
Given this merged PR diff, identify contract or schema changes.
If changes exist, propose exact updates for:
- `project_docs/03-design/api-contracts.md`
- `project_docs/03-design/database-schema.md`
- `project_docs/03-design/sequence-diagrams.md` (only if flow changed)
Output only minimal markdown patches.
```

## 5. AI-Assisted Stretch Goal Prompts

### 5.1 Simple AI insights endpoint (exploratory)

```text
Design a small proof-of-concept endpoint `/api/v1/portfolio/predictions` that returns experimental trend hints.
Constraints:
- clearly marked as non-production
- deterministic fallback response if model/data unavailable
- no impact on core CRUD endpoints
Provide DTOs, controller, service interface, and risk notes.
```

### 5.2 Quantum research summary prompt

```text
Create a short technical brief describing how portfolio optimization maps to QAOA.
Include assumptions, toy-problem setup (4-6 assets), and limitations for this project scale.
No unsupported performance claims.
```

## 6. Prompt Anti-Patterns to Avoid

- "Build everything" requests with no file-level constraints.
- Prompts without API contract context.
- Accepting generated fields/endpoints that are not approved.
- Skipping test generation.
- Copying code without checking status code and error-shape compliance.

