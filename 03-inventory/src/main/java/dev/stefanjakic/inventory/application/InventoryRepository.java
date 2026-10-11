package dev.stefanjakic.inventory.application;

import dev.stefanjakic.inventory.domain.InventoryItem;

import java.util.Optional;

public interface InventoryRepository {

    Optional<InventoryItem> findBySku(String sku);

    void insert(InventoryItem item);

    void update(InventoryItem item);
}
