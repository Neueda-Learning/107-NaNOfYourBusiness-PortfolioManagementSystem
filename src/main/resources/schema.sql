--- SQL script to create the portfolio_item table with various fields for different asset types
CREATE TABLE IF NOT EXISTS portfolio_item (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    -- Common fields
    type VARCHAR(20) NOT NULL,
    symbol_or_name VARCHAR(100) NOT NULL,

    quantity DECIMAL(19,4) NOT NULL,
    purchase_price DECIMAL(19,4) NOT NULL,
    purchase_date DATE NOT NULL,

    current_price DECIMAL(19,4),

    -- Common market information
    currency VARCHAR(10) DEFAULT 'INR',
    exchange VARCHAR(50),

    -- Stock specific
    company_name VARCHAR(100),
    sector VARCHAR(100),
    market_cap DECIMAL(19,4),
    dividend_yield DECIMAL(5,2),

    -- Bond specific
    issuer VARCHAR(100),
    face_value DECIMAL(19,4),
    coupon_rate DECIMAL(5,2),
    coupon_frequency VARCHAR(20),
    maturity_date DATE,
    credit_rating VARCHAR(10),
    yield_rate DECIMAL(5,2),
    -- Bond lifecycle
    status VARCHAR(20) DEFAULT 'ACTIVE',
    redemption_date DATE,
    redemption_value DECIMAL(19,4),

    -- Mutual Fund specific
    fund_house VARCHAR(100),
    category VARCHAR(100),
    expense_ratio DECIMAL(5,2),
    risk_level VARCHAR(20),
    nav DECIMAL(19,4),

    -- Tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

--- SQL script to create the user_data table. The MVP is single-user only, so this
--- table is expected to hold exactly one row representing the sole investor,
--- with wallet_balance tracking their available cash for buy/sell trades.
CREATE TABLE IF NOT EXISTS user_data (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,

    username VARCHAR(100) NOT NULL DEFAULT 'default_user',

    wallet_balance DECIMAL(19,4) NOT NULL DEFAULT 0.0000,

    -- Tracking
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP
);

-- Seed the single user row if it doesn't already exist (safe to re-run on every startup).
INSERT INTO user_data (username, wallet_balance)
SELECT 'default_user', 0.0000
WHERE NOT EXISTS (SELECT 1 FROM user_data);

-- Wallet transaction history for deposits, buy debits, and sell credits.
CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_data_id BIGINT NOT NULL,
    transaction_type VARCHAR(20) NOT NULL,
    amount DECIMAL(19,4) NOT NULL,
    balance_after DECIMAL(19,4) NOT NULL,
    asset_type VARCHAR(20),
    portfolio_item_id BIGINT,
    symbol_or_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wallet_transaction_user
        FOREIGN KEY (user_data_id) REFERENCES user_data(id),
    CONSTRAINT chk_wallet_transaction_amount CHECK (amount > 0),
    CONSTRAINT chk_wallet_transaction_type
        CHECK (transaction_type IN ('DEPOSIT', 'BUY_DEBIT', 'SELL_CREDIT')),
    INDEX idx_wallet_transaction_user_created (user_data_id, created_at, id)
);

-- Buy/sell trade history, shared across stock/mutual fund/bond holdings. Rows are kept
-- even after the related portfolio_item is deleted (e.g. a fully-sold holding), so no
-- FK to portfolio_item — portfolio_item_id is a soft reference for traceability only.
CREATE TABLE IF NOT EXISTS portfolio_trade (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_item_id BIGINT,
    asset_type VARCHAR(20) NOT NULL,
    symbol_or_name VARCHAR(100) NOT NULL,
    side VARCHAR(10) NOT NULL,
    quantity DECIMAL(19,4) NOT NULL,
    execution_price DECIMAL(19,4) NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_portfolio_trade_side CHECK (side IN ('BUY', 'SELL')),
    INDEX idx_portfolio_trade_symbol_type (symbol_or_name, asset_type, executed_at, id)
);

