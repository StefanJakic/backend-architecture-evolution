package dev.stefanjakic.lifecycle.domain.event;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderShipped(
    UUID eventId,
    String orderId,
    OrderStatus fromStatus,
    OrderStatus toStatus,
    Instant occurredAt
) implements OrderDomainEvent {
}
