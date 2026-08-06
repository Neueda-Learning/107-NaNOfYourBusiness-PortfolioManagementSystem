-- Seed data for local development profile.
-- This script is intentionally dev-only and loaded via application-dev.properties.

DELETE FROM portfolio_item;

INSERT INTO portfolio_item (type, symbol_or_name, quantity, purchase_price, purchase_date, current_price, created_at, updated_at)
VALUES
    ('STOCK', 'TCS.NS',        15.0000, 3450.0000, DATE '2025-01-10', 4120.2500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('STOCK', 'INFY.NS',       10.0000, 1500.0000, DATE '2025-02-12', 1845.5000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BOND',  'GSEC-2033',      8.0000,  985.0000, DATE '2024-11-01', 1012.3000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MUTUAL_FUND', 'HDFC Mid-Cap Opportunities', 120.5000, 130.2000, DATE '2025-03-05', 142.8500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MUTUAL_FUND', 'SBI Small Cap Fund',          90.0000,  95.0000, DATE '2025-04-20', 102.4000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

