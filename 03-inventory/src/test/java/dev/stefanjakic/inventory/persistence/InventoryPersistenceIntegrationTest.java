package dev.stefanjakic.inventory.persistence;

import dev.stefanjakic.inventory.application.InventoryRepository;
import dev.stefanjakic.inventory.application.InventoryService;
import dev.stefanjakic.inventory.domain.InsufficientInventoryException;
import dev.stefanjakic.inventory.domain.InventoryItem;
import dev.stefanjakic.inventory.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class InventoryPersistenceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private InventoryService service;

    @Autowired
    private InventoryRepository repository;

    @Test
    void sequentialTakePersistsNewQuantity() {
        String sku = sku();
        repository.insert(new InventoryItem(sku, 5));

        service.take(sku, 3);

        assertEquals(
            2,
            repository.findBySku(sku).orElseThrow().availableQuantity()
        );
    }

    @Test
    void sequentialTakeRejectsInsufficientInventory() {
        String sku = sku();
        repository.insert(new InventoryItem(sku, 2));

        assertThrows(
            InsufficientInventoryException.class,
            () -> service.take(sku, 3)
        );

        assertEquals(
            2,
            repository.findBySku(sku).orElseThrow().availableQuantity()
        );
    }

    private String sku() {
        return "sku-" + UUID.randomUUID();
    }
}
