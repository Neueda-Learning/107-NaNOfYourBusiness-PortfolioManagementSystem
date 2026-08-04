# Backend Plan — Portfolio Manager

> Give this file to Copilot as context before you start writing backend code.
> It defines the architecture, data model, and build order for the Spring Boot backend.
> Keep this file in the repo root (or `/docs`) and update it whenever a real design decision changes.

## 1. Project Context

- Single-user portfolio management REST API (no authentication/login required).
- Portfolio MVP holds three asset types: **Stocks**, **Mutual Funds**, **Bonds**.
- Stock prices are enriched from an **external market data API** (see §7).
- Customer asks for stock list/search in the app and customer support visibility; support is frontend-first in MVP unless an explicit backend endpoint is added later.
- SIP and Real Estate are treated as **Phase 2** extensions unless promoted by the instructor.
- A separate frontend (HTML/CSS/JS) consumes this API — see `API-contract.md`, which is the single source of truth for request/response shapes. **Any endpoint change must be reflected there first.**

## 2. Assumptions (adjust if wrong)

- Base package: `com.portfoliomanager` — rename in the plan below to match your actual `groupId`/`artifactId` if different.
- Build tool: Maven (swap instructions for Gradle if that's what your skeleton uses).
- Java 17+, Spring Boot 3.x.
- Database: MySQL, one schema, no multi-tenancy.
- No auth/security layer needed for v1 (Spring Security is **not** required yet).
- The frontend will call the API directly (CORS must be enabled for local dev, e.g. `http://localhost:5500` or wherever the static site is served from).

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Language/Framework | Java 17+, Spring Boot 3.x |
| Web | Spring Web (REST controllers) |
| Persistence | Spring JDBC (`JdbcTemplate`) — no JPA/Hibernate |
| Database | MySQL 8 |
| Validation | Jakarta Bean Validation (`spring-boot-starter-validation`) |
| API Docs | springdoc-openapi (Swagger UI) |
| External HTTP calls | Spring `RestClient` (or `WebClient` if reactive is preferred) |
| Testing | JUnit 5, Mockito, `spring-boot-starter-test`, Testcontainers (optional, for repository tests) |
| Build | Maven |

## 4. Package Structure

Keep it flat and conventional — this is a training project, not a microservice, so don't over-engineer with hexagonal/DDD layering.

```
com.portfoliomanager
├── PortfolioManagerApplication.java
├── config/
│   ├── CorsConfig.java
│   ├── OpenApiConfig.java
│   └── RestClientConfig.java        # bean for calling external stock price API
├── controller/
│   ├── PortfolioItemController.java # CRUD for stocks/bonds/mutual funds
│   ├── PortfolioSummaryController.java # dashboard/aggregate endpoints
│   └── MarketDataController.java       # optional: stock ticker list + quote lookup
├── service/
│   ├── PortfolioItemService.java
│   ├── PortfolioSummaryService.java
│   └── MarketDataService.java       # wraps external stock price API + caching
├── repository/
│   ├── PortfolioItemRepository.java # JdbcTemplate queries, hand-written SQL
│   └── PortfolioItemRowMapper.java  # RowMapper<PortfolioItem>
├── model/
│   ├── PortfolioItem.java           # plain POJO, not a JPA entity
│   └── AssetType.java               # enum (MVP): STOCK, BOND, MUTUAL_FUND
├── dto/
│   ├── PortfolioItemRequest.java    # inbound (create/update)
│   ├── PortfolioItemResponse.java   # outbound
│   ├── PortfolioSummaryResponse.java
│   └── PerformancePointResponse.java
├── mapper/
│   └── PortfolioItemMapper.java     # PortfolioItem <-> DTO (plain methods or MapStruct)
├── exception/
│   ├── ResourceNotFoundException.java
│   ├── ExternalApiException.java
│   └── GlobalExceptionHandler.java  # @ControllerAdvice
└── util/
    └── (helpers as needed)
```

## 5. Data Model

**Start minimal. Do not add fields "just in case."** The single biggest risk on this project is an over-complicated data model before anything works end to end.

### Phase 1 — MVP model (build this first)

A single table, `portfolio_item`, covering all three asset types with a `type` discriminator column and only the fields every asset type needs. `PortfolioItem` is a **plain POJO** (no JPA annotations) — the table/column mapping lives entirely in the SQL inside `PortfolioItemRepository`, not on the model class.

| Field | Type | Notes |
|---|---|---|
| `id` | `Long` (PK, auto) | populated from the generated key after `INSERT` |
| `type` | `AssetType` enum: `STOCK`, `BOND`, `MUTUAL_FUND` | stored as `VARCHAR`; convert `enum.name()` ↔ `AssetType.valueOf(...)` manually in the `RowMapper`/repository |
| `symbolOrName` | `String` | ticker for stocks, name/ISIN for bonds & funds |
| `quantity` | `BigDecimal` | shares/units held |
| `purchasePrice` | `BigDecimal` | price per unit at purchase |
| `purchaseDate` | `LocalDate` | |
| `currentPrice` | `BigDecimal` | for stocks: refreshed from external API; for bonds/funds: manually entered or last-known value in Phase 1 |
| `createdAt` / `updatedAt` | `LocalDateTime` | set explicitly by the repository on insert/update (no auditing framework) |

This single-table-with-discriminator approach keeps the three tabs on the frontend backed by one API resource filtered by `type`, which is simpler than three separate tables for a first version.

### Phase 2 — enhancements (only after Phase 1 works end-to-end)

Add type-specific optional columns (nullable, only populated for the relevant type):

- Bonds: `couponRate`, `maturityDate`, `faceValue`, `issuer`
- Mutual Funds: `expenseRatio`, `fundManager`, `category`
- Stocks: `sector`, `exchange`
- SIP (if first-class): `sipAmount`, `sipFrequency`, `sipStartDate`
- Real Estate (if first-class): `propertyName`, `location`, `estimatedValue`, `rentalIncome`

If this grows unwieldy, consider splitting into separate `stock`, `bond`, `mutual_fund` tables (each with its own `RowMapper` and repository methods) instead of one wide table with lots of nullable columns — but only refactor to this once you feel that pain, not before.

### Suggested DDL sketch (Phase 1)

```sql
CREATE TABLE portfolio_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)    NOT NULL,
    symbol_or_name  VARCHAR(100)   NOT NULL,
    quantity        DECIMAL(19,4)  NOT NULL,
    purchase_price  DECIMAL(19,4)  NOT NULL,
    purchase_date   DATE           NOT NULL,
    current_price   DECIMAL(19,4),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

There's no ORM to auto-generate the schema, so this DDL is the actual thing you run. Put it in `src/main/resources/schema.sql` (Spring Boot runs it automatically on startup against the configured datasource), and keep it in sync with this file whenever a column changes — this document and `schema.sql` should never drift apart.

## 6. REST Layer — Build Order

Build and test each step before moving to the next:

1. **CRUD for portfolio items** (no external API yet)
   - `GET /api/v1/portfolio-items` (supports `?type=STOCK`)
   - `GET /api/v1/portfolio-items/{id}`
   - `POST /api/v1/portfolio-items`
   - `PUT /api/v1/portfolio-items/{id}`
   - `DELETE /api/v1/portfolio-items/{id}`
2. **Validation & error handling** — `@Valid` on request DTOs, `GlobalExceptionHandler` returning a consistent error shape (see `API-contract.md`).
3. **Swagger/OpenAPI** — verify every endpoint above is visible and testable at `/swagger-ui.html`.
4. **External stock price integration** (`MarketDataService`) — see §7.
5. **Dashboard/summary endpoints** — aggregate totals, allocation breakdown, gain/loss, for the charts on the frontend.
6. **(Optional)** stock discovery endpoints (`/market/supported-tickers`, `/market/quote`) for list/search UX.
7. **(Stretch)** performance-over-time endpoint, AI/quantum experiments per the assignment's Appendix E.

Exact endpoint names, request/response fields, and status codes must match `API-contract.md`.

## 7. External Stock Data Integration

- Use a dedicated `MarketDataService` so controllers/other services never call the external API directly.
- Candidate sources (per the assignment): Yahoo Finance-based libraries (e.g. `yahoofinance-api` for Java), or the provided sample cached-price API.
- **Resilience is required**: the external API can be slow, rate-limited, or down. Wrap calls in try/catch, set an HTTP client timeout (e.g. 3–5s), and fall back to the last-known `currentPrice` stored in the DB rather than failing the whole request.
- Consider a simple in-memory cache (`ConcurrentHashMap<String, PriceQuote>` with a TTL, or Spring `@Cacheable`) so you don't hit the external API on every dashboard load.
- Do not fetch external prices synchronously inside every `GET` of the CRUD list — that couples read latency to a third-party API. A reasonable pattern: refresh prices periodically (`@Scheduled`) or on-demand via a dedicated `POST /api/v1/portfolio-items/{id}/refresh-price` endpoint, and always serve the last-stored `currentPrice` for normal reads.

## 8. Validation & Error Handling

- Validate all inbound DTOs: `@NotNull`, `@Positive`, `@PastOrPresent` (purchase date), etc.
- `GlobalExceptionHandler` (`@RestControllerAdvice`) should catch:
  - `MethodArgumentNotValidException` → `400` with field-level errors
  - `ResourceNotFoundException` (custom) → `404`
  - `ExternalApiException` (custom) → `502` or `200` with a degraded/stale-data flag, your call — document whichever you choose in `API-contract.md`
  - generic `Exception` → `500` with a safe, non-leaking message
- Error response body shape must match `API-contract.md` exactly so the frontend can render errors consistently.

## 9. Configuration

- `application.properties` / `application.yml`:
  - `spring.datasource.url/username/password` (use environment variables or a local `application-local.properties`, don't commit real credentials)
  - `spring.sql.init.mode=always` so Spring Boot runs `schema.sql` against MySQL on startup (default is `embedded`-only, so this must be set explicitly for a real MySQL datasource)
  - External API base URL/key as externalized config properties, not hardcoded
- `CorsConfig`: allow the frontend's origin(s) for local development.

## 10. Testing Strategy

- Unit test services with Mockito (mock the repository and `MarketDataService`).
- `@WebMvcTest` for controllers (mock the service layer).
- `@JdbcTest` (Spring Boot's slice test for JDBC, backed by an embedded/in-memory DB) — or Testcontainers with real MySQL if you want higher-fidelity SQL testing — for repository queries once you have hand-written SQL to verify.
- At minimum, cover: create/read/update/delete happy paths, validation failures, not-found cases, and external API fallback behaviour.

## 11. Milestones Checklist

- [ ] `PortfolioItem` model + `schema.sql` + `JdbcTemplate` repository created
- [ ] CRUD endpoints working, verified via Swagger UI or `curl`/Postman
- [ ] Validation + global error handling in place
- [ ] Frontend can list/add/remove items against real endpoints
- [ ] External stock price service integrated with graceful fallback
- [ ] Dashboard summary + allocation endpoints powering frontend charts
- [ ] (Optional) stock discovery endpoints for ticker list/search
- [ ] Basic test coverage on services and controllers
- [ ] (Stretch) performance-over-time endpoint, AI/quantum proof-of-concept per assignment Appendix E

## 12. Notes for Using Copilot

- Ask Copilot to generate one layer at a time (model → repository → service → controller → tests), referencing this file and `API-contract.md` in your prompt/context so field names stay consistent.
- When asking Copilot to add a new endpoint, paste the relevant section of `API-contract.md` into the prompt so the generated method signature matches exactly.
- Reject Copilot suggestions that introduce new fields/entities not listed here without first updating this file — keep the docs and the code in sync.
