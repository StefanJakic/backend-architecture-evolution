CREATE TABLE project (
    id VARCHAR(100) PRIMARY KEY,
    total_budget_cents BIGINT NOT NULL,

    CONSTRAINT project_budget_positive
        CHECK (total_budget_cents > 0)
);

CREATE TABLE budget_allocation (
    project_id VARCHAR(100) NOT NULL,
    allocation_id VARCHAR(100) NOT NULL,
    amount_cents BIGINT NOT NULL,

    PRIMARY KEY (project_id, allocation_id),

    CONSTRAINT budget_allocation_project_fk
        FOREIGN KEY (project_id)
        REFERENCES project(id)
        ON DELETE CASCADE,

    CONSTRAINT budget_allocation_amount_positive
        CHECK (amount_cents > 0)
);
