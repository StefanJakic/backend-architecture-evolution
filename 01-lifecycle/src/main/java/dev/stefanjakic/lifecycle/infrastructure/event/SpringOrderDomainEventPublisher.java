package dev.stefanjakic.lifecycle.infrastructure.event;

import dev.stefanjakic.lifecycle.application.OrderDomainEventPublisher;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Component
public class SpringOrderDomainEventPublisher implements OrderDomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringOrderDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publishAfterCommit(List<OrderDomainEvent> events) {
        if (events.isEmpty()) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            events.forEach(publisher::publishEvent);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    events.forEach(publisher::publishEvent);
                }
            }
        );
    }
}
