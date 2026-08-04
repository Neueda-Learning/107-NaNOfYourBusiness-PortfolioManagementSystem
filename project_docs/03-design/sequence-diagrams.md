# Sequence Diagrams - Portfolio Management System

This document captures the main runtime interaction flows for v1.
Participants used below:

- `Client` = frontend or API consumer
- `Controller` = REST controller
- `Service` = business logic layer
- `Repository` = JDBC persistence layer
- `DB` = relational database
- `Market API` = external quote provider

## 1. Create Portfolio Item

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB

    Client->>Controller: POST /api/v1/portfolio-items (request body)
    Controller->>Service: validate + create(request)
    Service->>Repository: insert(item)
    Repository->>DB: INSERT portfolio_item
    DB-->>Repository: generated id + row
    Repository-->>Service: saved item
    Service-->>Controller: response DTO (computed fields)
    Controller-->>Client: 201 Created + Location header
```

## 2. List Portfolio Items (Optional Type Filter)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB

    Client->>Controller: GET /api/v1/portfolio-items?type=STOCK
    Controller->>Service: list(type)
    Service->>Repository: findAll(type)
    Repository->>DB: SELECT ... WHERE type = ?
    DB-->>Repository: rows
    Repository-->>Service: models
    Service-->>Controller: mapped response list
    Controller-->>Client: 200 OK + JSON array
```

## 3. Delete Portfolio Item

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB

    Client->>Controller: DELETE /api/v1/portfolio-items/{id}
    Controller->>Service: delete(id)
    Service->>Repository: existsById(id)
    Repository->>DB: SELECT id FROM portfolio_item WHERE id=?
    DB-->>Repository: found/not found

    alt found
        Service->>Repository: deleteById(id)
        Repository->>DB: DELETE FROM portfolio_item WHERE id=?
        DB-->>Repository: rows affected
        Repository-->>Service: deleted
        Service-->>Controller: success
        Controller-->>Client: 204 No Content
    else not found
        Service-->>Controller: ResourceNotFoundException
        Controller-->>Client: 404 Not Found
    end
```

## 4. Refresh Stock Price (Success + Failure)

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB
    participant MarketAPI as Market API

    Client->>Controller: POST /api/v1/portfolio-items/{id}/refresh-price
    Controller->>Service: refreshPrice(id)
    Service->>Repository: findById(id)
    Repository->>DB: SELECT * FROM portfolio_item WHERE id=?
    DB-->>Repository: item row
    Repository-->>Service: item model

    alt quote fetch succeeds
        Service->>MarketAPI: GET price(symbol)
        MarketAPI-->>Service: latest price
        Service->>Repository: updateCurrentPrice(id, price)
        Repository->>DB: UPDATE portfolio_item SET current_price=?, updated_at=NOW()
        DB-->>Repository: updated row
        Repository-->>Service: updated item
        Service-->>Controller: response DTO
        Controller-->>Client: 200 OK
    else quote fetch fails
        Service->>MarketAPI: GET price(symbol)
        MarketAPI--xService: timeout/error
        Service-->>Controller: ExternalApiException
        Controller-->>Client: 502 Bad Gateway (standard error body)
    end
```

## 5. Portfolio Summary Endpoint

```mermaid
sequenceDiagram
    participant Client
    participant Controller
    participant Service
    participant Repository
    participant DB

    Client->>Controller: GET /api/v1/portfolio/summary
    Controller->>Service: getSummary()
    Service->>Repository: fetchAllForSummary()
    Repository->>DB: SELECT type, quantity, purchase_price, current_price FROM portfolio_item
    DB-->>Repository: rows
    Repository-->>Service: summary input models
    Service-->>Service: compute totals + allocation percentages
    Service-->>Controller: summary response DTO
    Controller-->>Client: 200 OK + summary JSON
```

## 6. Notes

- Validation failures are handled before service logic and return `400`.
- Global exception handling maps domain/infra errors to standardized response JSON.
- Normal list/get operations use stored prices, not live market fetches, to keep latency stable.

