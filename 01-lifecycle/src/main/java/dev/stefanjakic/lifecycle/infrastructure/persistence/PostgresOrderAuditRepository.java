package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderAuditEntry;
import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PostgresOrderAuditRepository implements OrderAuditRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresOrderAuditRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void append(
        String orderId,
        OrderAuditAction action,
        OrderStatus fromStatus,
        OrderStatus toStatus
    ) {
        jdbcTemplate.update(
            """
            INSERT INTO order_lifecycle_audit (
                order_id,
                action,
                from_status,
                to_status
            )
            VALUES (?, ?, ?, ?)
            """,
            orderId,
            action.name(),
            fromStatus == null ? null : fromStatus.name(),
            toStatus.name()
        );
    }

    @Override
    public List<OrderAuditEntry> findByOrderId(String orderId) {
        return jdbcTemplate.query(
            """
            SELECT id, order_id, action, from_status, to_status, occurred_at
            FROM order_lifecycle_audit
            WHERE order_id = ?
            ORDER BY id
            """,
            (resultSet, rowNumber) -> new OrderAuditEntry(
                resultSet.getLong("id"),
                resultSet.getString("order_id"),
                OrderAuditAction.valueOf(resultSet.getString("action")),
                nullableStatus(resultSet.getString("from_status")),
                OrderStatus.valueOf(resultSet.getString("to_status")),
                resultSet.getTimestamp("occurred_at").toInstant()
            ),
            orderId
        );
    }

    private OrderStatus nullableStatus(String status) {
        return status == null ? null : OrderStatus.valueOf(status);
    }
}
