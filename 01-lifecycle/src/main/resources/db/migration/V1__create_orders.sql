CREATE TABLE orders (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT ck_orders_status CHECK (
        status IN ('CREATED', 'CONFIRMED', 'SHIPPED', 'COMPLETED', 'CANCELLED')
    )
);
