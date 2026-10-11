ALTER TABLE reservation
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'CONFIRMED';

ALTER TABLE reservation
    ADD COLUMN expires_at TIMESTAMPTZ;

ALTER TABLE reservation
    ALTER COLUMN status DROP DEFAULT;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_status_valid
    CHECK (status IN ('HELD', 'CONFIRMED', 'EXPIRED'));

ALTER TABLE reservation
    ADD CONSTRAINT reservation_expiry_valid
    CHECK (
        (status = 'CONFIRMED' AND expires_at IS NULL)
        OR
        (status IN ('HELD', 'EXPIRED') AND expires_at IS NOT NULL)
    );

ALTER TABLE reservation
    DROP CONSTRAINT reservation_no_overlap;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_active_overlap
    EXCLUDE USING gist (
        resource_id WITH =,
        (tstzrange(starts_at, ends_at, '[)')) WITH &&
    )
    WHERE (status IN ('HELD', 'CONFIRMED'));
