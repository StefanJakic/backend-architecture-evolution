package dev.stefanjakic.lifecycle.infrastructure.observability;

import dev.stefanjakic.lifecycle.application.OrderOutboxMessage;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelayObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MicrometerOrderOutboxRelayObserver
    implements OrderOutboxRelayObserver {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(MicrometerOrderOutboxRelayObserver.class);

    private final Counter publishedCounter;
    private final Counter failedCounter;

    public MicrometerOrderOutboxRelayObserver(MeterRegistry registry) {
        this.publishedCounter = Counter.builder("order.outbox.relay.published")
            .description("Outbox events successfully published to Kafka")
            .register(registry);

        this.failedCounter = Counter.builder("order.outbox.relay.failed")
            .description("Outbox delivery attempts that failed")
            .register(registry);
    }

    @Override
    public void published(OrderOutboxMessage message) {
        publishedCounter.increment();

        LOGGER.atDebug()
            .addKeyValue("eventId", message.eventId())
            .addKeyValue("orderId", message.orderId())
            .addKeyValue("eventType", message.eventType())
            .addKeyValue("attemptCount", message.attemptCount())
            .log("Outbox event published");
    }

    @Override
    public void failed(
        OrderOutboxMessage message,
        RuntimeException exception
    ) {
        failedCounter.increment();

        LOGGER.atWarn()
            .addKeyValue("eventId", message.eventId())
            .addKeyValue("orderId", message.orderId())
            .addKeyValue("eventType", message.eventType())
            .addKeyValue("attemptCount", message.attemptCount())
            .setCause(exception)
            .log("Outbox event publication failed");
    }
}
