package dev.stefanjakic.inventory.application;

import dev.stefanjakic.inventory.domain.InventoryItem;

import java.util.Optional;

public interface InventoryRepository {

    Optional<InventoryItem> findBySku(String sku);

    Optional<InventoryItem> takeIfAvailable(
        String sku,
        int quantity
    );

    void insert(InventoryItem item);

    void update(InventoryItem item);
}
