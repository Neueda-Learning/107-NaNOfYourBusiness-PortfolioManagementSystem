CREATE TABLE IF NOT EXISTS portfolio_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    type            VARCHAR(20)    NOT NULL,
    symbol_or_name  VARCHAR(100)   NOT NULL,
    quantity        DECIMAL(19,4)  NOT NULL,
    purchase_price  DECIMAL(19,4)  NOT NULL,
    purchase_date   DATE           NOT NULL,
    current_price   DECIMAL(19,4),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS portfolio_trade (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    portfolio_item_id BIGINT         NOT NULL,
    asset_type        VARCHAR(20)    NOT NULL,
    symbol_or_name    VARCHAR(100)   NOT NULL,
    side              VARCHAR(10)    NOT NULL,
    quantity          DECIMAL(19,4)  NOT NULL,
    execution_price   DECIMAL(19,4)  NOT NULL,
    executed_at       TIMESTAMP      NOT NULL,
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

