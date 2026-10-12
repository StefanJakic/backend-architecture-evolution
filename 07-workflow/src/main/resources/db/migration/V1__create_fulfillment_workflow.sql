CREATE TABLE fulfillment_workflow (
    order_id VARCHAR(100) PRIMARY KEY,
    amount_cents BIGINT NOT NULL,
    phase VARCHAR(40) NOT NULL,
    reservation_id VARCHAR(100),
    payment_id VARCHAR(100),
    shipment_id VARCHAR(100),

    CONSTRAINT fulfillment_amount_positive
        CHECK (amount_cents > 0),

    CONSTRAINT fulfillment_phase_valid
        CHECK (
            phase IN (
                'STARTED',
                'INVENTORY_RESERVED',
                'PAYMENT_CHARGED',
                'COMPLETED',
                'COMPENSATED',
                'COMPENSATION_FAILED'
            )
        ),

    CONSTRAINT fulfillment_phase_data_consistent
        CHECK (
            phase IN (
                'STARTED',
                'COMPENSATED',
                'COMPENSATION_FAILED'
            )
            OR (
                phase = 'INVENTORY_RESERVED'
                AND reservation_id IS NOT NULL
            )
            OR (
                phase = 'PAYMENT_CHARGED'
                AND reservation_id IS NOT NULL
                AND payment_id IS NOT NULL
            )
            OR (
                phase = 'COMPLETED'
                AND reservation_id IS NOT NULL
                AND payment_id IS NOT NULL
                AND shipment_id IS NOT NULL
            )
        )
);
