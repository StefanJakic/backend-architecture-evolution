package dev.stefanjakic.lifecycle.application;

import java.time.Duration;

public class OrderOutboxRelay {

    private final OrderOutboxRelayRepository repository;
    private final OrderEventTransport transport;
    private final int batchSize;
    private final Duration leaseTimeout;

    public OrderOutboxRelay(
        OrderOutboxRelayRepository repository,
        OrderEventTransport transport,
        int batchSize,
        Duration leaseTimeout
    ) {
        this.repository = repository;
        this.transport = transport;
        this.batchSize = batchSize;
        this.leaseTimeout = leaseTimeout;
    }

    public int publishNextBatch() {
        var messages = repository.claimBatch(batchSize, leaseTimeout);
        int published = 0;

        for (OrderOutboxMessage message : messages) {
            try {
                transport.publish(message);
                repository.markPublished(message.eventId());
                published++;
            } catch (RuntimeException exception) {
                repository.releaseForRetry(
                    message.eventId(),
                    failureMessage(exception)
                );
            }
        }

        return published;
    }

    private String failureMessage(RuntimeException exception) {
        String message = exception.getMessage();

        return message == null || message.isBlank()
            ? exception.getClass().getSimpleName()
            : message;
    }
}
