# Problem Statement - Portfolio Manager Training Project

## Context

The team must build a portfolio management application centered on a REST API, with a simple frontend for browsing and managing holdings. The portfolio is single-user and may include stocks, mutual funds, and bonds.

This project is intended for training on API design, backend/frontend integration, and incremental delivery. The first release should prioritize a small, working end-to-end slice before expanding features.

## Problem

The user needs one place to record holdings, view current portfolio value, and understand gains/losses across asset types. Without this, portfolio tracking is fragmented, manual, and difficult to monitor over time.

## Target User

- Primary user: one individual investor (single-user assumption)
- Typical usage: long-term holdings with regular updates and dashboard review
- No login or multi-user management required in v1

## In Scope (MVP)

- REST API to create, read, update, and delete portfolio items
- Support three asset types: STOCK, MUTUAL_FUND, BOND
- Dashboard summary with total value, gain/loss, and allocation by asset type
- Frontend tabs for Dashboard, Stocks, Mutual Funds, and Bonds
- Add and remove portfolio items from UI

## Out of Scope (MVP)

- Authentication/authorization
- Multi-user account management
- Advanced trading workflows
- Production-grade AI/Quantum features

## Constraints

- Start with a minimal data model and expand only after core flow works
- Keep API contracts consistent with `project_docs/07-documentation/API-contract.md`
- Integrate external stock price data with graceful fallback to last-known values

## Success Indicators

- User can browse holdings by asset type
- User can add and remove holdings successfully
- User can see a dashboard with portfolio totals and allocation chart
- Backend and frontend are integrated and stable for demo use

## References

- `project_docs/07-documentation/about.md`
- `project_docs/07-documentation/Backend-plan.md`
- `project_docs/07-documentation/Frontend-plan.md`

