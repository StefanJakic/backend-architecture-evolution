package dev.stefanjakic.lifecycle.domain;

import dev.stefanjakic.lifecycle.domain.event.OrderCancelled;
import dev.stefanjakic.lifecycle.domain.event.OrderCompleted;
import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.domain.event.OrderShipped;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class Order {

    private final String id;
    private OrderStatus status;
    private final List<OrderDomainEvent> pendingEvents = new ArrayList<>();

    public Order(String id) {
        this(id, OrderStatus.CREATED);
        pendingEvents.add(
            new OrderCreated(
                UUID.randomUUID(),
                id,
                null,
                OrderStatus.CREATED,
                Instant.now()
            )
        );
    }

    private Order(String id, OrderStatus status) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = Objects.requireNonNull(status, "status");
    }

    public static Order restore(String id, OrderStatus status) {
        return new Order(id, status);
    }

    public String id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public void confirm() {
        require(OrderStatus.CREATED, "confirm");

        OrderStatus previousStatus = status;
        status = OrderStatus.CONFIRMED;

        pendingEvents.add(
            new OrderConfirmed(
                UUID.randomUUID(),
                id,
                previousStatus,
                status,
                Instant.now()
            )
        );
    }

    public void ship() {
        require(OrderStatus.CONFIRMED, "ship");

        OrderStatus previousStatus = status;
        status = OrderStatus.SHIPPED;

        pendingEvents.add(
            new OrderShipped(
                UUID.randomUUID(),
                id,
                previousStatus,
                status,
                Instant.now()
            )
        );
    }

    public void complete() {
        require(OrderStatus.SHIPPED, "complete");

        OrderStatus previousStatus = status;
        status = OrderStatus.COMPLETED;

        pendingEvents.add(
            new OrderCompleted(
                UUID.randomUUID(),
                id,
                previousStatus,
                status,
                Instant.now()
            )
        );
    }

    public void cancel() {
        if (status != OrderStatus.CREATED && status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderTransitionException(status, "cancel");
        }

        OrderStatus previousStatus = status;
        status = OrderStatus.CANCELLED;

        pendingEvents.add(
            new OrderCancelled(
                UUID.randomUUID(),
                id,
                previousStatus,
                status,
                Instant.now()
            )
        );
    }

    public List<OrderDomainEvent> releaseEvents() {
        List<OrderDomainEvent> events = List.copyOf(pendingEvents);
        pendingEvents.clear();
        return events;
    }

    private void require(OrderStatus expected, String action) {
        if (status != expected) {
            throw new InvalidOrderTransitionException(status, action);
        }
    }
}
