CREATE TABLE assignment_job (
    id VARCHAR(100) PRIMARY KEY,
    required_capability VARCHAR(100) NOT NULL,
    priority INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    claimed_by VARCHAR(100),

    CONSTRAINT assignment_priority_non_negative
        CHECK (priority >= 0),

    CONSTRAINT assignment_status_valid
        CHECK (status IN ('PENDING', 'CLAIMED')),

    CONSTRAINT assignment_claim_state_valid
        CHECK (
            (status = 'PENDING' AND claimed_by IS NULL)
            OR
            (status = 'CLAIMED' AND claimed_by IS NOT NULL)
        )
);

CREATE INDEX assignment_pending_selection_idx
    ON assignment_job (
        required_capability,
        status,
        priority DESC,
        id ASC
    );
