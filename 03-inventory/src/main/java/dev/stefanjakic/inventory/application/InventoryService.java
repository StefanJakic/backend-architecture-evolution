package dev.stefanjakic.inventory.application;

import dev.stefanjakic.inventory.domain.InsufficientInventoryException;
import dev.stefanjakic.inventory.domain.InventoryItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InventoryService {

    private final InventoryRepository repository;

    public InventoryService(InventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryItem take(
        String sku,
        int quantity
    ) {
        requirePositive(quantity);

        return repository.takeIfAvailable(sku, quantity)
            .orElseGet(() -> classifyFailedTake(sku, quantity));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryItem restock(
        String sku,
        int quantity
    ) {
        InventoryItem item = repository.findBySku(sku)
            .orElseThrow(() -> new InventoryNotFoundException(sku));

        item.restock(quantity);

        repository.update(item);

        return item;
    }

    private InventoryItem classifyFailedTake(
        String sku,
        int quantity
    ) {
        InventoryItem current = repository.findBySku(sku)
            .orElseThrow(() -> new InventoryNotFoundException(sku));

        throw new InsufficientInventoryException(
            sku,
            quantity,
            current.availableQuantity()
        );
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException(
                "quantity must be positive"
            );
        }
    }
}
