-- Seed data for local development profile.
-- This script is intentionally dev-only and loaded via application-dev.properties.
--
-- Purchase dates are deliberately spread across ~14 months so the dashboard's
-- Performance Over Time chart (US-15) has a meaningful date range to render for
-- every preset (1M/3M/6M/1Y/ALL), and so Top Movers / diversification insight
-- widgets have both gainers and losers to display across all three asset types.

DELETE FROM portfolio_item;
DELETE FROM wallet_transaction;
UPDATE user_data SET wallet_balance = 500000.0000;

INSERT INTO portfolio_item (type, symbol_or_name, quantity, purchase_price, purchase_date, current_price, created_at, updated_at)
VALUES
    -- Stocks — mix of long-held winners, a recent dip, and a fresh position
    ('STOCK', 'AAPL',          10.0000,  150.2500, DATE '2025-01-15',  195.4000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('STOCK', 'TSLA',           6.0000,  265.0000, DATE '2025-06-01',  221.7500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('STOCK', 'NVDA',           4.0000,  110.0000, DATE '2025-09-20',  128.4000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Bonds — one long-held, one recent addition
    ('BOND',  'GSEC-2033',      8.0000,  985.0000, DATE '2024-11-01', 1012.3000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('BOND',  'GSEC-2030',     12.0000,  970.0000, DATE '2025-07-18',  958.5000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

    -- Mutual Funds — spread across early, mid, and recent purchases
    ('MUTUAL_FUND', 'HDFC Mid-Cap Opportunities', 120.5000, 130.2000, DATE '2025-03-05', 142.8500, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MUTUAL_FUND', 'SBI Small Cap Fund',          90.0000,  95.0000, DATE '2025-04-20', 102.4000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MUTUAL_FUND', 'ICICI Prudential Bluechip',   60.0000, 112.0000, DATE '2024-10-08', 128.9000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('MUTUAL_FUND', 'Axis Long Term Equity',       45.0000,  88.5000, DATE '2025-11-12',  84.2000, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

