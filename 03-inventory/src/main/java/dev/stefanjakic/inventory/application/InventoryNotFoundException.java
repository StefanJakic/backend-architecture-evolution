package dev.stefanjakic.inventory.application;

public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(String sku) {
        super("Inventory item not found: " + sku);
    }
}
