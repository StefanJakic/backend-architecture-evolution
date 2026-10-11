package dev.stefanjakic.inventory.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryItemTest {

    @Test
    void takeReducesAvailableQuantity() {
        InventoryItem item = new InventoryItem("sku-1", 5);

        item.take(3);

        assertEquals(2, item.availableQuantity());
    }

    @Test
    void cannotTakeMoreThanAvailable() {
        InventoryItem item = new InventoryItem("sku-1", 2);

        assertThrows(
            InsufficientInventoryException.class,
            () -> item.take(3)
        );

        assertEquals(2, item.availableQuantity());
    }

    @Test
    void restockIncreasesAvailableQuantity() {
        InventoryItem item = new InventoryItem("sku-1", 2);

        item.restock(4);

        assertEquals(6, item.availableQuantity());
    }

    @Test
    void quantityOperationsMustBePositive() {
        InventoryItem item = new InventoryItem("sku-1", 2);

        assertThrows(
            IllegalArgumentException.class,
            () -> item.take(0)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> item.restock(-1)
        );
    }

    @Test
    void initialQuantityCannotBeNegative() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new InventoryItem("sku-1", -1)
        );
    }
}
