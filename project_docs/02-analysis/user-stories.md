# User Stories

## Scope

This backlog focuses on MVP first. AI/Quantum work is explicitly stretch-only.

## Story Format

As a [persona], I want [capability], so that [outcome].

## MVP Stories (Must Have)

### US-01 - View Dashboard Summary (High)
As an investor, I want to see total value, gain/loss, and item count on a dashboard, so that I can quickly assess portfolio health.
- **Maps to:** BR-03, FR-06

### US-02 - View Allocation Chart (High)
As an investor, I want a visual allocation chart by asset type, so that I can understand concentration risk at a glance.
- **Maps to:** BR-03, FR-07

### US-03 - Browse Stocks (High)
As an investor, I want a Stocks tab with my stock holdings, so that I can review stock positions separately.
- **Maps to:** BR-02, FR-02, FR-09, FR-10

### US-04 - Browse Mutual Funds (High)
As an investor, I want a Mutual Funds tab with my fund holdings, so that I can review fund positions separately.
- **Maps to:** BR-02, FR-02, FR-09, FR-10

### US-05 - Browse Bonds (High)
As an investor, I want a Bonds tab with my bond holdings, so that I can review bond positions separately.
- **Maps to:** BR-02, FR-02, FR-09, FR-10

### US-06 - Add Holding (High)
As an investor, I want to add a portfolio item, so that I can keep records current after new investments.
- **Maps to:** BR-04, FR-01, FR-11

### US-07 - Remove Holding (High)
As an investor, I want to remove a portfolio item, so that I can keep records current after exits.
- **Maps to:** BR-04, FR-05, FR-12

### US-08 - Update Holding Data (Medium)
As an investor, I want to update an existing holding, so that corrections and changes can be reflected accurately.
- **Maps to:** FR-04

### US-09 - Validate Input Errors Clearly (High)
As an investor, I want clear validation errors on invalid input, so that I can fix mistakes quickly.
- **Maps to:** FR-17, NFR-06

### US-10 - Graceful External Data Failure (High)
As an investor, I want portfolio views to continue working if market data is temporarily unavailable, so that I can still access my holdings.
- **Maps to:** BR-05, FR-15, NFR-04

### US-11 - Consistent API Errors (Medium)
As a frontend developer, I want consistent backend error responses, so that I can display reliable error messages.
- **Maps to:** FR-18, NFR-11

### US-12 - API Documentation (Medium)
As a team member, I want Swagger/OpenAPI docs for implemented endpoints, so that integration and testing are faster.
- **Maps to:** BR-06, FR-19

## Should Have Stories

### US-13 - Controlled Price Refresh Strategy (Medium)
As a backend developer, I want controlled stock price refresh behavior, so that normal reads are not slowed by third-party dependencies.
- **Maps to:** FR-16, NFR-03

### US-14 - Loading/Error States in UI (Medium)
As an investor, I want loading and error indicators in each panel, so that I always know what the app is doing.
- **Maps to:** FR-13, NFR-13

## Stretch Stories (Post-MVP Only)

### US-15 - Performance Over Time Chart (Stretch)
As an investor, I want a trend chart over time, so that I can evaluate portfolio trajectory.
- **Maps to:** FR-08

### US-16 - Edit Flow in Frontend (Stretch)
As an investor, I want to edit holdings from the UI, so that I can correct data without API tools.
- **Maps to:** FR-21

### US-17 - AI/Quantum Experimental Features (Stretch)
As an instructor/team, I want a small AI/Quantum proof-of-concept, so that we can demonstrate exploration beyond core requirements.
- **Maps to:** BR-07, FR-22, NFR-21

## Out of Scope for MVP

- Authentication and multi-user management
- Mandatory AI/Quantum feature delivery

## References

- `project_docs/01-requirements/business-requirements.md`
- `project_docs/01-requirements/functional-requirements.md`
- `project_docs/01-requirements/non-functional-requirements.md`

