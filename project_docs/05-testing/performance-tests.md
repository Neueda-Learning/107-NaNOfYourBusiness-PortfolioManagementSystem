# Performance Test Plan

## Goal
Ensure the MVP portfolio API remains responsive for expected training usage with frequent dashboard refreshes and moderate CRUD activity.

## Workload Assumptions
- Single-user app, but test with burst traffic to validate headroom.
- Portfolio size target: 1,000+ holdings (many stocks plus bonds/funds).
- Dashboard endpoint called repeatedly while CRUD operations occur.

## Performance Objectives (MVP)
- `GET /api/v1/portfolio-items`: p95 < 500 ms for 1,000 records.
- `GET /api/v1/portfolio/summary`: p95 < 700 ms.
- `POST/PUT /api/v1/portfolio-items`: p95 < 600 ms.
- Error rate under load: < 1% (excluding intentionally invalid requests).

## Test Scenarios

### Scenario A: Read-heavy dashboard usage
- 70% summary/list reads, 20% filtered reads, 10% writes.
- Duration: 10 minutes steady state.
- Validate chart data endpoints remain responsive.

### Scenario B: Write burst
- Rapid create/update/delete bursts on stock holdings.
- Verify no deadlocks or slow query escalation.

### Scenario C: External API degradation
- Simulate slow/failing stock quote source.
- Confirm CRUD and summary still respond using stored prices.

## Tooling Options
- JMeter (GUI and CLI)
- k6 (scriptable, CI-friendly)
- Gatling (if team already knows Scala-based tooling)

## Metrics to Capture
- p50/p90/p95 latency per endpoint.
- Throughput (requests/sec).
- Error rates by status code.
- JVM memory and CPU under load.
- DB slow queries and connection pool utilization.

## Data Preparation
- Seed database with:
  - 700 stocks
  - 200 bonds
  - 100 mutual funds
- Use realistic price/quantity ranges to stress aggregation math.

## Pass/Fail Criteria
- Meets latency targets above for Scenario A.
- No sustained memory leak trend during 15-minute run.
- No DB connection exhaustion.
- Service remains functional after load test completion.

## Follow-up Actions if Failing
1. Add/optimize DB indexes (`type`, possibly `symbol_or_name`).
2. Reduce expensive repeated calculations via summary caching.
3. Add pagination on list endpoints if full-list response becomes too large.
4. Profile JVM hotspots in aggregation and mapping logic.

