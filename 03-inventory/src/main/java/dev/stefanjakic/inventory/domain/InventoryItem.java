package dev.stefanjakic.inventory.domain;

public final class InventoryItem {

    private final String sku;
    private int availableQuantity;

    public InventoryItem(
        String sku,
        int availableQuantity
    ) {
        if (sku == null || sku.isBlank()) {
            throw new IllegalArgumentException(
                "sku must not be blank"
            );
        }

        if (availableQuantity < 0) {
            throw new IllegalArgumentException(
                "availableQuantity must not be negative"
            );
        }

        this.sku = sku;
        this.availableQuantity = availableQuantity;
    }

    public void take(int quantity) {
        requirePositive(quantity);

        if (quantity > availableQuantity) {
            throw new InsufficientInventoryException(
                sku,
                quantity,
                availableQuantity
            );
        }

        availableQuantity -= quantity;
    }

    public void restock(int quantity) {
        requirePositive(quantity);
        availableQuantity = Math.addExact(availableQuantity, quantity);
    }

    public String sku() {
        return sku;
    }

    public int availableQuantity() {
        return availableQuantity;
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "quantity must be positive"
            );
        }
    }
}
