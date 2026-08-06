# Use Cases

## Scope

This document captures system behavior for MVP user journeys and clearly separates stretch cases.

## Actors

- **Primary Actor:** Investor (single user)
- **Supporting Systems:** Portfolio Manager API, Database, External Market Data API
- **Secondary Actor:** Instructor (review/acceptance stakeholder)

## Preconditions

- Application is available
- Database is configured and reachable
- No authentication required for MVP

## UC-01: View Portfolio Dashboard (MVP)

- **Goal:** See total portfolio value, gain/loss, item count, and allocation chart
- **Trigger:** User opens app or selects Dashboard tab
- **Main Flow:**
  1. User opens Dashboard tab
  2. Frontend requests portfolio summary from backend
  3. Backend calculates summary from stored portfolio items
  4. Frontend renders summary cards and allocation chart
- **Alternate Flow:** If external stock data is unavailable, backend uses last-known stored prices
- **Postcondition:** User sees current portfolio summary without system failure

## UC-02: Browse Holdings by Asset Type (MVP)

- **Goal:** Review holdings separately for Stocks, Mutual Funds, and Bonds
- **Trigger:** User selects an asset tab
- **Main Flow:**
  1. User opens a specific asset tab
  2. Frontend requests portfolio items filtered by `type`
  3. Backend returns matching items
  4. Frontend renders item list/table
- **Alternate Flow:** If no items exist for a type, frontend shows an empty-state message
- **Postcondition:** User understands current holdings for selected type

## UC-03: Add Portfolio Item (MVP)

- **Goal:** Add a new holding
- **Trigger:** User clicks Add action in asset tab
- **Main Flow:**
  1. User opens add form
  2. User enters required fields and submits
  3. Frontend validates basic input and sends create request
  4. Backend validates and persists item
  5. Frontend refreshes the asset list and dashboard summary
- **Alternate Flow:** Validation fails; frontend shows field-specific errors
- **Postcondition:** New portfolio item is stored and visible in UI

## UC-04: Update Portfolio Item (MVP API, optional UI)

- **Goal:** Modify existing holding details
- **Trigger:** User or API client submits update for item ID
- **Main Flow:**
  1. Client submits updated data
  2. Backend validates and updates stored item
  3. Updated item is returned or retrievable via follow-up read
- **Alternate Flow:** Item ID not found; backend returns not-found response
- **Postcondition:** Stored data reflects latest valid values

## UC-05: Remove Portfolio Item (MVP)

- **Goal:** Delete an existing holding
- **Trigger:** User clicks Remove and confirms
- **Main Flow:**
  1. User selects Remove on an item
  2. Frontend confirms intent
  3. Frontend sends delete request
  4. Backend deletes item
  5. Frontend refreshes list and dashboard summary
- **Alternate Flow:** Item already missing; backend returns not-found response
- **Postcondition:** Item is no longer present in portfolio views

## UC-06: Refresh Stock Price (MVP behavior)

- **Goal:** Keep stock `currentPrice` reasonably up to date without blocking all reads
- **Trigger:** Scheduled job, explicit refresh endpoint, or controlled refresh strategy
- **Main Flow:**
  1. Backend requests latest price from external source
  2. On success, backend stores updated `currentPrice`
  3. Normal read endpoints return stored values
- **Alternate Flow:** Timeout/failure; backend keeps prior value and logs failure path
- **Postcondition:** Portfolio remains usable even during market API issues

## UC-07: Manage Wallet and Fund Trades (MVP)

- **Goal:** Let the investor deposit funds into a wallet and use that balance to buy assets, with sell proceeds credited back
- **Trigger:** User deposits funds, or initiates a buy/sell action from a Stocks, Mutual Funds, or Bonds tab
- **Main Flow:**
  1. User deposits funds into the wallet
  2. Backend validates the amount and increases the wallet balance
  3. User initiates a buy for a stock, mutual fund, or bond
  4. Backend checks wallet balance covers the purchase amount
  5. Backend debits the wallet, records the transaction, and persists the holding
  6. On a sell action, backend credits sale proceeds back to the wallet and records the transaction
- **Alternate Flow:** If wallet balance is insufficient for a buy, backend rejects the transaction with a clear error and no holding/wallet changes occur
- **Postcondition:** Wallet balance and holdings stay consistent with executed buy/sell transactions

## UC-08: AI/Quantum Experimentation (Stretch Only)

- **Goal:** Demonstrate learning experiments after MVP completion
- **Trigger:** Team has completed MVP and stabilization goals
- **Examples:** Prediction endpoint, optimization demo, natural language portfolio query
- **Constraint:** Not part of MVP acceptance criteria

## References

- `project_docs/01-requirements/functional-requirements.md`
- `project_docs/07-documentation/Backend-plan.md`
- `project_docs/07-documentation/Frontend-plan.md`

