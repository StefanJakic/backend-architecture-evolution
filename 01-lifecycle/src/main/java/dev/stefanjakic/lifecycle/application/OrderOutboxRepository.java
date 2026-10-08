package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;

public interface OrderOutboxRepository {

    void append(OrderDomainEvent event);
}
