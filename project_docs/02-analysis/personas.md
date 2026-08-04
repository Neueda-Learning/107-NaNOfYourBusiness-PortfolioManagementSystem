# Personas

## Purpose

Define who this product is for, what they need, and what should be prioritized in MVP decisions.

## Primary Persona: Priya Sharma (Individual Investor)

- **Role:** Single end user of the portfolio manager
- **Experience:** Basic to intermediate investing knowledge
- **Devices:** Laptop (primary), occasional tablet
- **Usage Frequency:** Daily/weekly portfolio checks, periodic updates

### Goals

- Track holdings across stocks, mutual funds, and bonds in one place
- See total portfolio value and gain/loss quickly
- Add and remove holdings without complex workflows
- Understand allocation by asset type through visual charts

### Pain Points

- Tracking holdings across spreadsheets/apps is fragmented
- Manual calculations for gain/loss are error-prone
- Hard to get a quick summary across different asset types

### Behaviors

- Reviews dashboard first, then drills into asset-specific tabs
- Makes occasional updates after buy/sell events
- Prefers simple, predictable UI and clear feedback on actions

### MVP Needs

- Reliable CRUD operations
- Clear tabbed navigation
- Fast dashboard with meaningful summary metrics
- Graceful behavior when external market data is unavailable

## Secondary Persona: Instructor (Customer Proxy)

- **Role:** Defines/validates requirements and evaluates delivery quality
- **Needs:** Demonstrable end-to-end functionality, good documentation, incremental progress
- **Success Signal:** Team can explain requirements traceability and trade-offs

## Non-Goals for MVP

- Multi-user account management
- Authentication/authorization
- Advanced trading automation

## Stretch Boundary

AI/Quantum features are **stretch-only**. They are not part of MVP acceptance and should be treated as experimental additions after core functionality is stable.

## References

- `project_docs/01-requirements/problem-statement.md`
- `project_docs/01-requirements/business-requirements.md`
- `project_docs/07-documentation/about.md`

