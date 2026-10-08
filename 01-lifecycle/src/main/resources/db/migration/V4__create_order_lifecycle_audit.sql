CREATE TABLE order_lifecycle_audit (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    action VARCHAR(20) NOT NULL,
    from_status VARCHAR(32),
    to_status VARCHAR(32) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_order_lifecycle_audit_order
        FOREIGN KEY (order_id) REFERENCES orders(id),

    CONSTRAINT ck_order_lifecycle_audit_action
        CHECK (action IN ('CREATE', 'CONFIRM', 'SHIP', 'COMPLETE', 'CANCEL')),

    CONSTRAINT ck_order_lifecycle_audit_from_status
        CHECK (
            from_status IS NULL
            OR from_status IN ('CREATED', 'CONFIRMED', 'SHIPPED', 'COMPLETED', 'CANCELLED')
        ),

    CONSTRAINT ck_order_lifecycle_audit_to_status
        CHECK (
            to_status IN ('CREATED', 'CONFIRMED', 'SHIPPED', 'COMPLETED', 'CANCELLED')
        )
);

CREATE INDEX idx_order_lifecycle_audit_order_id_id
    ON order_lifecycle_audit (order_id, id);
