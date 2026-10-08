package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderAuditEntry;
import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.domain.event.OrderCancelled;
import dev.stefanjakic.lifecycle.domain.event.OrderCompleted;
import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.domain.event.OrderShipped;
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
    public void append(OrderDomainEvent event) {
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
            event.orderId(),
            actionOf(event).name(),
            event.fromStatus() == null ? null : event.fromStatus().name(),
            event.toStatus().name()
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

    private OrderAuditAction actionOf(OrderDomainEvent event) {
        return switch (event) {
            case OrderCreated ignored -> OrderAuditAction.CREATE;
            case OrderConfirmed ignored -> OrderAuditAction.CONFIRM;
            case OrderShipped ignored -> OrderAuditAction.SHIP;
            case OrderCompleted ignored -> OrderAuditAction.COMPLETE;
            case OrderCancelled ignored -> OrderAuditAction.CANCEL;
        };
    }

    private OrderStatus nullableStatus(String status) {
        return status == null ? null : OrderStatus.valueOf(status);
    }
}
