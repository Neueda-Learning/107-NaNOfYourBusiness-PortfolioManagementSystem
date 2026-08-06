# Database Schema - Portfolio Management System

This document defines the relational schema for the backend API.
The MVP uses one core table with a type discriminator to keep implementation simple.

## 1. Schema Principles

- Start with minimum viable fields.
- Keep one table for all asset types in Phase 1.
- Use `type` as discriminator (`STOCK`, `BOND`, `MUTUAL_FUND`).
- Keep schema and API contract synchronized.

## 2. Phase 1 (MVP) Table Design

## `portfolio_item`

| Column | Type | Null | Key | Default | Notes |
|---|---|---|---|---|---|
| `id` | BIGINT | NO | PK | auto increment | item id |
| `type` | VARCHAR(20) | NO |  |  | enum string |
| `symbol_or_name` | VARCHAR(100) | NO |  |  | ticker or instrument name |
| `quantity` | DECIMAL(19,4) | NO |  |  | must be > 0 |
| `purchase_price` | DECIMAL(19,4) | NO |  |  | must be > 0 |
| `purchase_date` | DATE | NO |  |  | must not be future |
| `current_price` | DECIMAL(19,4) | YES |  |  | last known value |
| `created_at` | TIMESTAMP | NO |  | current timestamp | record created time |
| `updated_at` | TIMESTAMP | NO |  | current timestamp | record updated time |

## 3. Suggested DDL (MySQL)

```sql
CREATE TABLE portfolio_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    symbol_or_name VARCHAR(100) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    purchase_price DECIMAL(19,4) NOT NULL,
    purchase_date DATE NOT NULL,
    current_price DECIMAL(19,4) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_type CHECK (type IN ('STOCK', 'BOND', 'MUTUAL_FUND')),
    CONSTRAINT chk_quantity CHECK (quantity > 0),
    CONSTRAINT chk_purchase_price CHECK (purchase_price > 0),
    CONSTRAINT chk_current_price CHECK (current_price IS NULL OR current_price > 0)
);
```

If the target MySQL variant ignores `CHECK`, enforce the same rules in service validation.

## 4. Indexing Strategy

Create indexes that match common access patterns.

```sql
CREATE INDEX idx_portfolio_item_type ON portfolio_item(type);
CREATE INDEX idx_portfolio_item_symbol ON portfolio_item(symbol_or_name);
CREATE INDEX idx_portfolio_item_purchase_date ON portfolio_item(purchase_date);
```

## 5. Field Mapping (DB <-> API)

| DB Column | API Field |
|---|---|
| `id` | `id` |
| `type` | `type` |
| `symbol_or_name` | `symbolOrName` |
| `quantity` | `quantity` |
| `purchase_price` | `purchasePrice` |
| `purchase_date` | `purchaseDate` |
| `current_price` | `currentPrice` |
| `created_at` | `createdAt` |
| `updated_at` | `updatedAt` |

Computed response fields are not stored:

- `currentValue`
- `gainLoss`
- `gainLossPercent`

## 6. Wallet Tables (MVP)

### `user_data`

| Column | Type | Null | Key | Default | Notes |
|---|---|---|---|---|---|
| `id` | BIGINT | NO | PK | auto increment | single-user row |
| `username` | VARCHAR(100) | NO |  | `default_user` | MVP single investor |
| `wallet_balance` | DECIMAL(19,4) | NO |  | `0.0000` | available cash balance |
| `created_at` | TIMESTAMP | NO |  | current timestamp | record created time |
| `updated_at` | TIMESTAMP | NO |  | current timestamp | record updated time |

### `wallet_transaction`

| Column | Type | Null | Key | Default | Notes |
|---|---|---|---|---|---|
| `id` | BIGINT | NO | PK | auto increment | wallet transaction id |
| `user_data_id` | BIGINT | NO | FK |  | references `user_data.id` |
| `transaction_type` | VARCHAR(20) | NO |  |  | enum: `DEPOSIT`, `BUY_DEBIT`, `SELL_CREDIT` |
| `amount` | DECIMAL(19,4) | NO |  |  | positive transaction amount |
| `balance_after` | DECIMAL(19,4) | NO |  |  | wallet balance after transaction |
| `asset_type` | VARCHAR(20) | YES |  |  | optional asset context |
| `portfolio_item_id` | BIGINT | YES |  |  | optional holding reference |
| `symbol_or_name` | VARCHAR(100) | YES |  |  | optional asset symbol/name |
| `created_at` | TIMESTAMP | NO |  | current timestamp | transaction timestamp |

## 7. Phase 2 Optional Columns

Add only after MVP CRUD and summary are complete.

### Bond-specific

- `coupon_rate` DECIMAL(8,4)
- `maturity_date` DATE
- `face_value` DECIMAL(19,4)
- `issuer` VARCHAR(120)

### Mutual-fund-specific

- `expense_ratio` DECIMAL(8,4)
- `fund_manager` VARCHAR(120)
- `category` VARCHAR(80)

### Stock-specific

- `sector` VARCHAR(80)
- `exchange` VARCHAR(40)

## 8. Data Integrity Rules

- `type` must be valid enum.
- `quantity`, `purchase_price`, and provided `current_price` must be positive.
- `purchase_date` cannot be future date.
- `symbol_or_name` must be present and trimmed.

## 9. Initialization and Migration Notes

- Keep schema SQL in `src/main/resources/schema.sql`.
- For non-embedded DB startup initialization, set `spring.sql.init.mode=always`.
- Every schema change should include:
  1. DDL update,
  2. repository SQL update,
  3. API contract update (if relevant),
  4. regression tests.

## 10. Example Seed Data (Optional for Local Dev)

```sql
INSERT INTO portfolio_item (type, symbol_or_name, quantity, purchase_price, purchase_date, current_price)
VALUES
('STOCK', 'AAPL', 10.0000, 150.2500, '2025-01-15', 195.4000),
('BOND', 'US-TREASURY-10Y', 5.0000, 980.0000, '2024-06-10', 1002.5000),
('MUTUAL_FUND', 'VTSAX', 20.0000, 110.5000, '2023-11-01', 125.7500);
```

## 11. Related Documents

- `project_docs/03-design/api-contracts.md`
- `project_docs/03-design/architecture.md`
- `project_docs/07-documentation/Backend-plan.md`

