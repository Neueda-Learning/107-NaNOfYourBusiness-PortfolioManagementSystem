# Acceptance Criteria

## Scope

This document defines MVP acceptance gates and testable criteria. Stretch items are tracked separately and do not block MVP sign-off.

## MVP Release Gate

MVP is accepted when all criteria AC-01 to AC-12 pass and no critical defects remain in core flows.

## Core Acceptance Criteria

### AC-01: Dashboard Summary Loads
- Given at least one portfolio item exists
- When the user opens the Dashboard
- Then total value, total gain/loss, and item count are displayed
- And values are computed from persisted data
- **Traceability:** US-01, FR-06

### AC-02: Allocation Chart Displays
- Given portfolio items exist across one or more asset types
- When Dashboard data is loaded
- Then an allocation chart is rendered by asset type
- **Traceability:** US-02, FR-07

### AC-03: Browse by Asset Type
- Given stored holdings exist
- When the user opens Stocks, Mutual Funds, or Bonds tabs
- Then only items matching the selected asset type are listed
- **Traceability:** US-03/04/05, FR-02, FR-09, FR-10

### AC-04: Add Holding Success
- Given valid form input
- When the user submits Add Item
- Then the item is persisted
- And appears in the relevant asset tab
- And dashboard summary updates accordingly
- **Traceability:** US-06, FR-01, FR-11

### AC-05: Remove Holding Success
- Given an existing portfolio item
- When the user confirms removal
- Then the item is deleted from persistence
- And no longer shown in list views
- And dashboard summary updates accordingly
- **Traceability:** US-07, FR-05, FR-12

### AC-06: Update Holding via API
- Given an existing portfolio item ID and valid payload
- When update is requested
- Then the stored item is updated and retrievable with new values
- **Traceability:** US-08, FR-04

### AC-07: Validation Errors are Returned Clearly
- Given invalid input (missing required fields, non-positive values, invalid date)
- When create/update is requested
- Then request is rejected with clear error details
- **Traceability:** US-09, FR-17, NFR-06

### AC-08: Not Found Handling
- Given a non-existent item ID
- When read/update/delete is requested
- Then backend returns a not-found response with standard error shape
- **Traceability:** FR-18

### AC-09: Graceful External Data Failure
- Given external market API is unavailable or times out
- When user loads portfolio reads/dashboard
- Then the system responds successfully using last-known stored prices where applicable
- **Traceability:** US-10, FR-15, NFR-04

### AC-10: Controlled Stock Price Refresh Behavior
- Given normal list/dashboard read operations
- When data is requested repeatedly
- Then the system does not require live third-party fetch on every read
- **Traceability:** US-13, FR-16, NFR-03

### AC-11: UI Loading and Error Feedback
- Given API request latency or failure
- When user interacts with data-driven tabs
- Then loading and error states are shown clearly
- **Traceability:** US-14, FR-13, NFR-13

### AC-12: API Documentation Availability
- Given the backend is running
- When a developer opens Swagger/OpenAPI UI
- Then implemented endpoints are visible and testable
- **Traceability:** US-12, FR-19

### AC-13: Wallet Deposit Increases Balance
- Given a wallet with a known balance
- When the investor deposits a valid positive amount
- Then the wallet balance increases by that amount
- And the transaction is recorded in wallet history
- **Traceability:** US-18, FR-24, FR-27

### AC-14: Buy Rejected on Insufficient Wallet Balance
- Given a wallet balance lower than the requested purchase amount
- When the investor attempts to buy a stock, mutual fund, or bond
- Then the buy request is rejected with a clear error
- And neither the wallet balance nor holdings are changed
- **Traceability:** US-20, FR-25

### AC-15: Buy Debits and Sell Credits Wallet
- Given a wallet balance sufficient for a purchase
- When the investor buys a stock, mutual fund, or bond
- Then the wallet balance is debited by the purchase amount and the transaction is recorded
- And when the investor subsequently sells that holding
- Then the wallet balance is credited by the sale proceeds and the transaction is recorded
- **Traceability:** US-19, US-21, FR-25, FR-26, FR-27

## Out of MVP Acceptance Scope

- Performance-over-time chart
- Frontend edit flow
- AI/Quantum features

These are stretch-only and can be accepted separately after MVP sign-off.

## Stretch Acceptance (Optional)

### SAC-01: Experimental Features Are Labeled
- If any AI/Quantum feature is delivered
- Then it is clearly marked experimental and includes limitations/disclaimers
- **Traceability:** FR-22, NFR-21

## References

- `project_docs/02-analysis/user-stories.md`
- `project_docs/01-requirements/functional-requirements.md`
- `project_docs/01-requirements/non-functional-requirements.md`

