package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderDomainEventPublisher;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;

import java.util.ArrayList;
import java.util.List;

public final class InMemoryOrderDomainEventPublisher
    implements OrderDomainEventPublisher {

    private final List<OrderDomainEvent> publishedEvents = new ArrayList<>();

    @Override
    public void publishAfterCommit(List<OrderDomainEvent> events) {
        publishedEvents.addAll(events);
    }

    public List<OrderDomainEvent> publishedEvents() {
        return List.copyOf(publishedEvents);
    }
}
