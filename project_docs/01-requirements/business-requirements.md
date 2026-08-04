# Business Requirements - Portfolio Manager

## Product Vision
Build a single-user portfolio management application that gives complete visibility and control of financial holdings, with a dashboard-first experience.

## Primary Business Goals
1. Let the user manage a large portfolio with full create/read/update/delete access.
2. Show portfolio health clearly through profit/loss indicators and graphs.
3. Organize holdings by asset class to reduce complexity.
4. Include customer support visibility inside the app.
5. Support long-term investing workflows (especially stocks, plus regular mutual fund/SIP activity).

## MVP Scope
- Single user (no login/auth required).
- Asset classes in MVP: Stocks, Bonds, Mutual Funds.
- Core capabilities:
  - Add, view, edit, remove holdings.
  - Dashboard with total value and profit/loss.
  - Separate tabs for Stocks, Bonds, and Mutual Funds.
  - Stock list/lookup support in the app (where feasible).
  - Integration with sample stock API for quote data.
  - In-app support/help section.

## Phase 2 Scope (Customer-Requested Extensions)
- SIP as first-class recurring investment workflow.
- Real estate holdings.
- Advanced AI/RAG-assisted insights and query workflows.
- Additional complexity through guided dropdowns and recommendation helpers.

## Non-Functional Expectations
- Responsive, easy-to-understand UI.
- Stable data handling for larger portfolios.
- Graceful behavior when external stock API is unavailable.
- Clear documentation for testing, deployment, and usage.

## Success Criteria
- User can fully manage holdings without backend errors in normal use.
- Dashboard accurately reflects portfolio gain/loss.
- Asset tabs make it easy to inspect each category.
- Team can deploy and demo the system reliably.

