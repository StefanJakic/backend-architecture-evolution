package dev.stefanjakic.lifecycle.domain.event;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public sealed interface OrderDomainEvent
    permits OrderCreated, OrderConfirmed, OrderShipped, OrderCompleted, OrderCancelled {

    UUID eventId();

    String orderId();

    OrderStatus fromStatus();

    OrderStatus toStatus();

    Instant occurredAt();
}
