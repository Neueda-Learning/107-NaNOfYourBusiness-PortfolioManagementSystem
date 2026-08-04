# Infrastructure Plan

## Objective
Describe the minimum infrastructure needed to run the single-user Portfolio Manager API reliably across local, test, and production-like environments.

## Environments
- **Local Dev**: developer machine, Spring Boot app + MySQL (or containerized DB).
- **Test/UAT**: shared environment for integration and acceptance testing.
- **Prod-like Demo**: stable environment for instructor/customer demos.

## Core Components
1. **Application Service**
   - Spring Boot REST API (Java 17+, Maven build output JAR).
2. **Database**
   - MySQL 8 schema for portfolio data.
3. **External Market Data Dependency**
   - Sample cached stock API for quote refresh operations.
4. **Static Frontend (optional in same app)**
   - Hosted from `src/main/resources/static` for simplest deployment.

## Suggested Topology (MVP)
- One VM/container host running:
  - `portfolio-api` container
  - `mysql` container
- Reverse proxy optional for local/demo environments.

## Configuration Matrix
| Variable | Purpose | Example |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | environment profile | `dev`, `test`, `prod` |
| `SPRING_DATASOURCE_URL` | DB connection string | `jdbc:mysql://mysql:3306/portfolio_db` |
| `SPRING_DATASOURCE_USERNAME` | DB user | `portfolio_user` |
| `SPRING_DATASOURCE_PASSWORD` | DB password | `<secret>` |
| `MARKET_API_BASE_URL` | quote source endpoint | sample API base URL |
| `SERVER_PORT` | app port | `8080` |

## Non-Functional Baseline
- Availability target (training): >= 95% during demo window.
- Daily DB backup for shared environments.
- Logs retained at least 7 days for troubleshooting.

## Monitoring Basics
- Health endpoint via Spring Actuator (`/actuator/health`) recommended.
- Log errors for:
  - quote refresh failures
  - DB connectivity issues
  - validation spikes

## Risks and Mitigations
- **External API downtime** -> keep last-known stock price and return graceful errors.
- **Schema drift across environments** -> keep `schema.sql` version-controlled and reviewed.
- **Credential leakage** -> use environment variables/secrets store, not committed files.

