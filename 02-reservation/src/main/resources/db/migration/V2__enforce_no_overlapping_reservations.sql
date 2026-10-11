CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE reservation
    ADD CONSTRAINT reservation_no_overlap
    EXCLUDE USING gist (
        resource_id WITH =,
        (tstzrange(starts_at, ends_at, '[)')) WITH &&
    );
