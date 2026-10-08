package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderOutboxRepository;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryOrderOutboxRepository implements OrderOutboxRepository {

    private final List<OrderDomainEvent> events = new ArrayList<>();

    @Override
    public void append(OrderDomainEvent event) {
        events.add(event);
    }

    public List<OrderDomainEvent> events() {
        return List.copyOf(events);
    }
}
