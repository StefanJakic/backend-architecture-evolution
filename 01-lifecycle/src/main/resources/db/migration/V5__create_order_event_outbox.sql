CREATE TABLE order_event_outbox (
    event_id UUID PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_event_outbox_order
        FOREIGN KEY (order_id) REFERENCES orders(id)
);
