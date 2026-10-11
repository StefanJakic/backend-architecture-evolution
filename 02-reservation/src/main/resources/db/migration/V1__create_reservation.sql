CREATE TABLE reservation (
    id VARCHAR(36) PRIMARY KEY,
    resource_id VARCHAR(100) NOT NULL,
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT reservation_resource_not_blank
        CHECK (BTRIM(resource_id) <> ''),

    CONSTRAINT reservation_valid_window
        CHECK (starts_at < ends_at)
);

CREATE INDEX idx_reservation_resource_window
    ON reservation (resource_id, starts_at, ends_at);
