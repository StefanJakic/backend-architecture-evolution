ALTER TABLE order_event_outbox
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN claimed_at TIMESTAMPTZ,
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_error TEXT,

    ADD CONSTRAINT ck_order_event_outbox_status
        CHECK (status IN ('PENDING', 'IN_PROGRESS', 'PUBLISHED')),

    ADD CONSTRAINT ck_order_event_outbox_attempt_count
        CHECK (attempt_count >= 0),

    ADD CONSTRAINT ck_order_event_outbox_delivery_state
        CHECK (
            (status = 'PENDING'
                AND claimed_at IS NULL
                AND published_at IS NULL)
            OR
            (status = 'IN_PROGRESS'
                AND claimed_at IS NOT NULL
                AND published_at IS NULL)
            OR
            (status = 'PUBLISHED'
                AND published_at IS NOT NULL)
        );

CREATE INDEX idx_order_event_outbox_delivery
    ON order_event_outbox (status, recorded_at)
    WHERE status <> 'PUBLISHED';
