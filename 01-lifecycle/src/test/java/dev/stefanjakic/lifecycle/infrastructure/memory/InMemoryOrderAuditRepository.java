package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderAuditEntry;
import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public final class InMemoryOrderAuditRepository implements OrderAuditRepository {

    private final List<OrderAuditEntry> entries = new ArrayList<>();
    private long nextId = 1;

    @Override
    public void append(
        String orderId,
        OrderAuditAction action,
        OrderStatus fromStatus,
        OrderStatus toStatus
    ) {
        entries.add(
            new OrderAuditEntry(
                nextId++,
                orderId,
                action,
                fromStatus,
                toStatus,
                Instant.now()
            )
        );
    }

    @Override
    public List<OrderAuditEntry> findByOrderId(String orderId) {
        return entries.stream()
            .filter(entry -> entry.orderId().equals(orderId))
            .toList();
    }
}
