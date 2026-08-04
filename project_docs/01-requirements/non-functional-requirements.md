# Non-Functional Requirements - Portfolio Manager

## Scope

These NFRs apply to the MVP implementation unless marked as stretch.

## Performance

### NFR-01: API Response Targets
For typical local/demo data volumes, core CRUD and summary endpoints should respond within 2 seconds under normal conditions.

### NFR-02: External API Timeout
External stock price calls shall use a bounded timeout (target 3-5 seconds) to prevent UI blocking.

### NFR-03: Efficient Reads
The backend shall avoid blocking all list reads on live third-party fetches for each request.

## Availability and Resilience

### NFR-04: Graceful Degradation
If external market data is unavailable, the application shall continue serving portfolio data using last-known values.

### NFR-05: Error Isolation
External API failures shall not crash the application or prevent non-dependent operations (e.g., local CRUD).

## Data Quality and Integrity

### NFR-06: Validation Enforcement
Invalid inputs (missing required fields, non-positive numbers, invalid dates) shall be rejected with clear validation messages.

### NFR-07: Persistence Consistency
Successful create/update/delete operations shall be durably persisted to the configured database.

### NFR-08: Timestamp Tracking
Created and updated timestamps shall be stored for portfolio items.

## Security and Privacy (MVP Level)

### NFR-09: Authentication Scope
No authentication is required for MVP; this is an explicit product boundary, not an omission.

### NFR-10: Sensitive Configuration Handling
Database credentials and API keys shall be externalized via environment/config properties and not hardcoded in source.

### NFR-11: Safe Error Messages
Error responses shall avoid leaking stack traces, credentials, or internal infrastructure details.

## Usability

### NFR-12: Clear Navigation
The UI shall provide clear tab-based navigation between dashboard and asset views.

### NFR-13: User Feedback States
Every data-driven view shall provide loading and error feedback states.

### NFR-14: Readability
The UI should maintain clear visual distinction for gain/loss and present key values in a scannable format.

## Compatibility and Architecture

### NFR-15: Technology Baseline
Backend should target Java 17+ and Spring Boot 3.x; frontend should use plain HTML/CSS/JavaScript with no framework requirement.

### NFR-16: API Contract Compliance
Implemented endpoints and payloads shall match `project_docs/07-documentation/API-contract.md`.

### NFR-17: Local Development Support
The solution shall support local development for backend and frontend integration (same-origin static hosting or CORS-enabled split setup).

## Maintainability and Documentation

### NFR-18: Layered Structure
Backend code shall follow a clear controller-service-repository separation.

### NFR-19: Documentation Currency
Architecture/API/requirements documents shall be kept in sync with implemented behavior.

### NFR-20: Testability
The codebase shall include automated tests for critical service/controller/repository behavior in line with project scope.

## Stretch NFRs (Optional)

### NFR-21: Experimental Feature Labeling
AI/Quantum features, if implemented, shall be explicitly marked as experimental with limitations documented.

## References

- `project_docs/07-documentation/about.md`
- `project_docs/07-documentation/Backend-plan.md`
- `project_docs/07-documentation/Frontend-plan.md`

