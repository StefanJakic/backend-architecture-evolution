package dev.stefanjakic.inventory.infrastructure.persistence;

import dev.stefanjakic.inventory.application.InventoryRepository;
import dev.stefanjakic.inventory.domain.InventoryItem;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresInventoryRepository implements InventoryRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresInventoryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<InventoryItem> findBySku(String sku) {
        return jdbcTemplate.query(
            """
            SELECT sku, available_quantity
            FROM inventory_item
            WHERE sku = ?
            """,
            (resultSet, rowNumber) -> new InventoryItem(
                resultSet.getString("sku"),
                resultSet.getInt("available_quantity")
            ),
            sku
        ).stream().findFirst();
    }

    @Override
    public void insert(InventoryItem item) {
        jdbcTemplate.update(
            """
            INSERT INTO inventory_item (
                sku,
                available_quantity
            )
            VALUES (?, ?)
            """,
            item.sku(),
            item.availableQuantity()
        );
    }

    @Override
    public void update(InventoryItem item) {
        jdbcTemplate.update(
            """
            UPDATE inventory_item
            SET available_quantity = ?
            WHERE sku = ?
            """,
            item.availableQuantity(),
            item.sku()
        );
    }
}
