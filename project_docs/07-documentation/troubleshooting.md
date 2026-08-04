# Troubleshooting Guide

## 1) Application Fails to Start
### Symptoms
- Startup exits with datasource or port errors.

### Checks
- Verify `SPRING_DATASOURCE_*` values.
- Confirm MySQL is reachable and schema user exists.
- Ensure port `8080` is not already in use.

## 2) Database Connection Errors
### Symptoms
- `Communications link failure` or authentication failures.

### Checks
- Validate JDBC URL format.
- Confirm DB user/password and privileges.
- If using Docker, confirm service name (`mysql`) matches compose network hostname.

## 3) Validation Errors on Create/Update
### Symptoms
- `400` with field errors.

### Checks
- `quantity` and `purchasePrice` must be > 0.
- `purchaseDate` cannot be future date.
- `type` must be one of `STOCK`, `BOND`, `MUTUAL_FUND`.

## 4) Stock Price Refresh Returns 502
### Symptoms
- Refresh endpoint fails while CRUD still works.

### Checks
- Verify `MARKET_API_BASE_URL`.
- Test sample API URL manually.
- Confirm fallback behavior uses last-known price.

## 5) Dashboard Totals Look Wrong
### Symptoms
- UI totals do not match manual calculations.

### Checks
- Re-check decimal precision and rounding in service logic.
- Confirm updated item values are persisted before summary call.
- Validate frontend is not formatting/parsing numbers as strings.

## 6) CORS Issues (Separate Frontend Server)
### Symptoms
- Browser blocks API calls from another origin.

### Checks
- Add frontend origin to backend CORS config.
- Confirm preflight `OPTIONS` requests are allowed.

## 7) Tests Passing Locally But Failing in CI
### Checks
- Ensure Java/Maven versions match CI config.
- Check test assumptions on timezone and locale.
- Remove hidden dependency on local DB state.

## Useful Commands
```powershell
.\mvnw test
.\mvnw spring-boot:run
```

