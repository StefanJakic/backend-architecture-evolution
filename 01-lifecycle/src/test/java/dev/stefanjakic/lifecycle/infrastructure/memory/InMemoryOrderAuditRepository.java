package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderAuditEntry;
import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.domain.event.OrderCancelled;
import dev.stefanjakic.lifecycle.domain.event.OrderCompleted;
import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.domain.event.OrderShipped;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryOrderAuditRepository implements OrderAuditRepository {

    private final List<OrderAuditEntry> entries = new ArrayList<>();
    private long nextId = 1;

    @Override
    public void append(OrderDomainEvent event) {
        entries.add(
            new OrderAuditEntry(
                nextId++,
                event.orderId(),
                actionOf(event),
                event.fromStatus(),
                event.toStatus(),
                event.occurredAt()
            )
        );
    }

    @Override
    public List<OrderAuditEntry> findByOrderId(String orderId) {
        return entries.stream()
            .filter(entry -> entry.orderId().equals(orderId))
            .toList();
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
}
