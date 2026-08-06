# 📈 Portfolio Management System

A single-user financial portfolio management application built with **Spring Boot 4** and a vanilla HTML/CSS/JS frontend. It exposes a REST API to manage holdings across three asset types — **Stocks**, **Bonds**, and **Mutual Funds** — and provides a live dashboard with portfolio totals, gain/loss metrics, allocation breakdowns, and performance charts.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Getting Started](#getting-started)
  - [Prerequisites](#prerequisites)
  - [Run Locally (Dev Mode — H2)](#run-locally-dev-mode--h2)
  - [Run with Docker (Production — MySQL)](#run-with-docker-production--mysql)
- [Configuration](#configuration)
- [API Reference](#api-reference)
  - [Portfolio Items](#portfolio-items)
  - [Portfolio Summary & Performance](#portfolio-summary--performance)
  - [Mutual Funds](#mutual-funds)
  - [Market Data](#market-data)
- [Error Handling](#error-handling)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Documentation](#documentation)

---

## Features

- **CRUD operations** for portfolio items (Stocks, Bonds, Mutual Funds)
- **Dashboard summary** — total value, total cost, gain/loss (absolute & %), asset allocation
- **Performance chart** — time-series portfolio value over selectable ranges (1M, 3M, 6M, 1Y, ALL)
- **Real-time stock price refresh** via external market data API (with graceful fallback to last-known price)
- **Buy / Sell** actions for stock holdings — execution price and timestamp recorded server-side
- **Mutual fund NAV** lookup via the mfapi.in public API
- **Swagger UI** for interactive API exploration
- **H2 in-memory database** for fast local development (no MySQL setup needed)
- **MySQL** for production/Docker deployments
- **JaCoCo** code coverage reports

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1 |
| Web | Spring MVC (spring-boot-starter-webmvc) |
| Validation | Spring Validation |
| Persistence | Spring JDBC (`JdbcTemplate`) |
| Database (dev) | H2 (in-memory) |
| Database (prod) | MySQL 8.0 |
| ORM / Mapping | Manual row mappers + Lombok |
| API Docs | SpringDoc OpenAPI (Swagger UI) |
| Build | Maven (Maven Wrapper included) |
| Containerisation | Docker + Docker Compose |
| CI/CD | Jenkins (`Jenkinsfile` included) |
| Testing | JUnit 5, Spring Boot Test, JaCoCo |

---

## Architecture

```
[Web Frontend (static HTML/JS/CSS)]
             │
             ▼
[Portfolio REST API — Spring Boot :8080]
  ┌──────────┬──────────┬────────────┐
  │Controller│ Service  │ Repository │
  └──────────┴──────────┴────────────┘
                              │
                              ▼
                        [MySQL / H2 DB]

  Service ──► [External Market Data API]  (stock price refresh)
  Service ──► [mfapi.in]                  (mutual fund NAV)
  Service ──► [Twelve Data API]           (historical OHLCV)
```

- **Controller layer** — validates requests, maps HTTP ↔ DTOs, returns standard status codes.
- **Service layer** — business logic, computed fields (`currentValue`, `gainLoss`, `gainLossPercent`), external API coordination.
- **Repository layer** — plain `JdbcTemplate` SQL queries against a single `portfolio_item` table.

---

## Getting Started

### Prerequisites

| Requirement | Minimum version |
|---|---|
| Java JDK | 21 |
| Maven | 3.9+ (or use the included `mvnw`) |
| Docker & Docker Compose | For the production/MySQL path only |

### Run Locally (Dev Mode — H2)

The default `dev` profile uses an **in-memory H2 database** — no external database required.

```bash
# Clone the repository
git clone <your-repo-url>
cd 107-NaNOfYourBusiness-PortfolioManagementSystem

# Build and run
./mvnw spring-boot:run
# Windows:
mvnw.cmd spring-boot:run
```

The app starts on **http://localhost:8080**.

| URL | Description |
|---|---|
| `http://localhost:8080` | Frontend UI |
| `http://localhost:8080/swagger-ui.html` | Swagger / OpenAPI UI |
| `http://localhost:8080/api-docs` | Raw OpenAPI JSON |
| `http://localhost:8080/h2-console` | H2 web console (dev only) |

The H2 console JDBC URL is `jdbc:h2:mem:portfolio_db`, username `sa`, password *(empty)*.

### Run with Docker (Production — MySQL)

```bash
# Build the JAR first
./mvnw clean package -DskipTests

# Start MySQL + API containers
docker-compose up --build
```

The API is available at **http://localhost:8082** and MySQL is exposed on port **3307**.

Override defaults via environment variables or a `.env` file in the project root:

```env
MYSQL_DATABASE=portfolio_db
MYSQL_USER=portfolio_user
MYSQL_PASSWORD=portfolio_pass
MYSQL_ROOT_PASSWORD=root_pass
MARKET_API_BASE_URL=https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default
```

---

## Configuration

Key properties (see `src/main/resources/application.properties`):

| Property | Default | Description |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `spring.profiles.default` | `dev` | Active profile (`dev` or `prod`) |
| `market.api.base-url` | AWS endpoint | Cached stock price API base URL |
| `market.supported-tickers` | AAPL, MSFT, … | Comma-separated tickers to poll |
| `market.poll.interval-ms` | `10000` | Price-poll scheduler interval (ms) |
| `marketdata.batch-size` | `8` | Tickers refreshed per scheduler tick |
| `mfapi.base-url` | `https://api.mfapi.in` | Mutual fund NAV API |
| `twelvedata.api-key` | *(empty)* | Twelve Data API key for historical OHLCV |
| `finnhub.api-key` | *(empty)* | Finnhub API key (optional enrichment) |
| `springdoc.swagger-ui.path` | `/swagger-ui.html` | Swagger UI path |

For production secrets (API keys, DB passwords) use environment variables or a `.env` file — they are automatically picked up via `spring.config.import=optional:file:.env[.properties]`.

---

## API Reference

Base path: `/api/v1` · Format: `application/json` · Auth: none (single-user v1)

### Portfolio Items

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/portfolio-items` | List all items (optional `?type=STOCK\|BOND\|MUTUAL_FUND`) |
| `GET` | `/api/v1/portfolio-items/{id}` | Get a single item |
| `POST` | `/api/v1/portfolio-items` | Create a new item |
| `PUT` | `/api/v1/portfolio-items/{id}` | Update an existing item |
| `DELETE` | `/api/v1/portfolio-items/{id}` | Delete an item |
| `POST` | `/api/v1/portfolio-items/{id}/refresh-price` | Refresh stock price from market data API |
| `POST` | `/api/v1/portfolio-items/{id}/buy` | Buy additional quantity (`{ "quantity": 2.5 }`) |
| `POST` | `/api/v1/portfolio-items/{id}/sell` | Sell quantity — auto-removes holding at zero |

**Create/Update request body:**

```json
{
  "type": "STOCK",
  "symbolOrName": "AAPL",
  "quantity": 10,
  "purchasePrice": 150.25,
  "purchaseDate": "2025-01-15",
  "currentPrice": 195.40
}
```

**Response (includes computed fields):**

```json
{
  "id": 12,
  "type": "STOCK",
  "symbolOrName": "AAPL",
  "quantity": 10,
  "purchasePrice": 150.25,
  "purchaseDate": "2025-01-15",
  "currentPrice": 195.40,
  "currentValue": 1954.00,
  "gainLoss": 451.50,
  "gainLossPercent": 30.05,
  "createdAt": "2025-01-15T09:00:00",
  "updatedAt": "2026-08-01T07:30:00"
}
```

### Portfolio Summary & Performance

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/portfolio/summary` | Totals and allocation by asset type |
| `GET` | `/api/v1/portfolio/performance?range=1M` | Time-series value/cost (`1M`, `3M`, `6M`, `1Y`, `ALL`) |

**Summary response:**

```json
{
  "totalValue": 45230.75,
  "totalCost": 41000.00,
  "totalGainLoss": 4230.75,
  "totalGainLossPercent": 10.32,
  "itemCount": 14,
  "allocationByType": [
    { "type": "STOCK",       "value": 30250.00, "percent": 66.9, "count": 9 },
    { "type": "BOND",        "value":  8000.00, "percent": 17.7, "count": 3 },
    { "type": "MUTUAL_FUND", "value":  6980.75, "percent": 15.4, "count": 2 }
  ]
}
```

### Mutual Funds

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/mutual-funds/search?q=hdfc` | Search for mutual funds by name |
| `GET` | `/api/v1/mutual-funds/{schemeCode}/nav` | Get current NAV for a scheme |

### Market Data

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/api/v1/market-data/{ticker}` | Get latest cached price for a ticker |

---

## Error Handling

All non-2xx responses follow a consistent shape:

```json
{
  "status": 400,
  "error": "VALIDATION_ERROR",
  "message": "quantity must be greater than 0",
  "timestamp": "2026-08-06T10:15:30",
  "fieldErrors": [
    { "field": "quantity", "message": "must be greater than 0" }
  ]
}
```

| Status | Meaning |
|---|---|
| `200` | Successful GET / PUT / refresh |
| `201` | Resource created |
| `204` | Resource deleted |
| `400` | Validation error |
| `404` | Resource not found |
| `502` | External price service unavailable |
| `500` | Unexpected server error |

When the external market data API is unavailable, `refresh-price` returns `502` but all other CRUD operations continue normally using the last-known price.

---

## Testing

```bash
# Run all tests
./mvnw test

# Run tests + generate JaCoCo coverage report
./mvnw verify

# Coverage report location
target/site/jacoco/index.html
```

Test suite includes:

- **Unit tests** — service and repository layer (mocked dependencies)
- **Integration tests** — `@SpringBootTest` with H2
- **Controller tests** — `MockMvc` slice tests

---

## Project Structure

```
src/
├── main/
│   ├── java/com/example/portfolio/
│   │   ├── PortfolioApplication.java     # Entry point
│   │   ├── controller/                   # REST controllers
│   │   ├── service/                      # Business logic
│   │   ├── repository/                   # JdbcTemplate DAOs
│   │   ├── dto/                          # Request / Response objects
│   │   ├── model/                        # Domain models
│   │   ├── client/                       # External API clients
│   │   ├── mapper/                       # Row mappers
│   │   ├── exception/                    # Exception classes & handler
│   │   ├── config/                       # Spring configuration beans
│   │   └── util/                         # Shared utilities
│   └── resources/
│       ├── application.properties        # Common config
│       ├── application-dev.properties    # Dev (H2) config
│       ├── application-prod.properties   # Prod (MySQL) config
│       ├── schema.sql                    # DDL — auto-applied on startup
│       ├── data-dev.sql                  # Seed data for dev profile
│       └── static/                       # Frontend (HTML/CSS/JS)
└── test/
    └── java/com/example/portfolio/       # Test classes
```

---

## Documentation

Full project documentation lives in the [`project_docs/`](project_docs/) directory:

| Folder | Contents |
|---|---|
| `01-requirements/` | Problem statement, business & functional requirements |
| `02-analysis/` | User stories, use cases, personas, acceptance criteria |
| `03-design/` | API contracts, architecture, database schema, sequence diagrams |
| `04-development/` | Sprint plan, task breakdown, coding standards, AI prompt library |
| `05-testing/` | Unit & integration test plans, performance tests, security checklist |
| `06-deployment/` | Docker guide, CI/CD, infrastructure, rollback procedure |
| `07-documentation/` | Developer guide, API guide, architecture overview, troubleshooting |

---

## Team

**NaN Of Your Business** — Training project, August 2026.

