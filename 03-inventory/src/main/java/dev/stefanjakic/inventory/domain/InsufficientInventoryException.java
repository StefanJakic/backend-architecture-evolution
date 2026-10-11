package dev.stefanjakic.inventory.domain;

public class InsufficientInventoryException extends RuntimeException {

    public InsufficientInventoryException(
        String sku,
        int requested,
        int available
    ) {
        super(
            "Insufficient inventory for %s: requested=%d, available=%d"
                .formatted(sku, requested, available)
        );
    }
}
