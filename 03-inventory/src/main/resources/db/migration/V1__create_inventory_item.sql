CREATE TABLE inventory_item (
    sku VARCHAR(100) PRIMARY KEY,
    available_quantity INTEGER NOT NULL,

    CONSTRAINT inventory_available_non_negative
        CHECK (available_quantity >= 0)
);
