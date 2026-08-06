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

