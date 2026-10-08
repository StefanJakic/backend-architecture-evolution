package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;

import java.util.List;

public interface OrderDomainEventPublisher {

    void publishAfterCommit(List<OrderDomainEvent> events);
}
