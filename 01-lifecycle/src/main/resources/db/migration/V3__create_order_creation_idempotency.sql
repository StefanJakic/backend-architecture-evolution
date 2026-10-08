CREATE TABLE order_creation_idempotency (
    idempotency_key VARCHAR(100) PRIMARY KEY,
    status VARCHAR(20) NOT NULL,
    order_id VARCHAR(36),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,

    CONSTRAINT ck_order_creation_idempotency_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),

    CONSTRAINT fk_order_creation_idempotency_order
        FOREIGN KEY (order_id) REFERENCES orders(id),

    CONSTRAINT ck_order_creation_idempotency_completion
        CHECK (
            (status = 'IN_PROGRESS' AND order_id IS NULL AND completed_at IS NULL)
            OR
            (status = 'COMPLETED' AND order_id IS NOT NULL AND completed_at IS NOT NULL)
        )
);
