package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderOutboxRepository;
import dev.stefanjakic.lifecycle.domain.event.OrderCancelled;
import dev.stefanjakic.lifecycle.domain.event.OrderCompleted;
import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.domain.event.OrderShipped;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.sql.Timestamp;

@Repository
public class PostgresOrderOutboxRepository implements OrderOutboxRepository {

    private final JdbcTemplate jdbcTemplate;
    private final JsonMapper jsonMapper;

    public PostgresOrderOutboxRepository(
        JdbcTemplate jdbcTemplate,
        JsonMapper jsonMapper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void append(OrderDomainEvent event) {
        jdbcTemplate.update(
            """
            INSERT INTO order_event_outbox (
                event_id,
                order_id,
                event_type,
                payload,
                occurred_at
            )
            VALUES (?, ?, ?, CAST(? AS JSONB), ?)
            """,
            event.eventId(),
            event.orderId(),
            eventTypeOf(event),
            serialize(event),
            Timestamp.from(event.occurredAt())
        );
    }

    private String serialize(OrderDomainEvent event) {
        try {
            return jsonMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new IllegalStateException(
                "Failed to serialize Order domain event " + event.eventId(),
                exception
            );
        }
    }

    private String eventTypeOf(OrderDomainEvent event) {
        return switch (event) {
            case OrderCreated ignored -> "OrderCreated";
            case OrderConfirmed ignored -> "OrderConfirmed";
            case OrderShipped ignored -> "OrderShipped";
            case OrderCompleted ignored -> "OrderCompleted";
            case OrderCancelled ignored -> "OrderCancelled";
        };
    }
}
