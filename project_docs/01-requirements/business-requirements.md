# Business Requirements - Portfolio Manager

## Objective

Deliver a working portfolio management product increment that enables a single user to track and manage investments across stocks, mutual funds, and bonds.

## Business Goals

- Provide a single source of truth for portfolio holdings and current values
- Improve user visibility into portfolio performance through dashboard metrics and charts
- Reduce manual effort for maintaining and reviewing holdings
- Demonstrate a complete backend-frontend delivery aligned to training outcomes

## Stakeholders

- Customer proxy: instructor
- Delivery team: project team (backend, frontend, QA/documentation)
- End user: single investor persona

## Prioritized Requirements

### BR-01 (High): Core Portfolio Tracking
The system shall allow the user to store and view holdings for stocks, mutual funds, and bonds.

### BR-02 (High): Portfolio Browsing Experience
The system shall provide clear asset-type views so the user can browse holdings by Stocks, Mutual Funds, and Bonds.

### BR-03 (High): Performance Visibility
The system shall provide a dashboard summarizing portfolio value and gain/loss, including a graphical allocation view.

### BR-04 (High): Data Maintenance
The system shall allow the user to add and remove portfolio items through the frontend, with changes persisted in the backend database.

### BR-05 (Medium): Reliable Market Data Enrichment
The system should enrich stock prices from an external source and continue operating with last-known prices if the external API is unavailable.

### BR-06 (Medium): Delivery Quality and Documentation
The team shall maintain API documentation and project documentation to support demo, handover, and continued iteration.

### BR-07 (Low, Stretch): Advanced Innovation
The team may explore AI and/or Quantum proof-of-concepts after MVP completion, documented as experimental features.

### BR-08 (High): Wallet-Funded Trading
The system shall provide a cash wallet so the user can fund buy transactions and receive proceeds from sell transactions across stocks, mutual funds, and bonds, preventing purchases that exceed available wallet balance.

## Business Success Criteria

- MVP features (browse, dashboard, add, remove) are demo-ready end-to-end
- Core API endpoints are documented and testable
- Portfolio updates are reflected accurately in dashboard totals
- External data outages do not block basic portfolio usage

## Release Scope

- **Release 1 (MVP):** BR-01 to BR-06, BR-08
- **Stretch:** BR-07 only after MVP stabilization

## References

- `project_docs/07-documentation/about.md`
- `project_docs/07-documentation/Backend-plan.md`
- `project_docs/07-documentation/Frontend-plan.md`

