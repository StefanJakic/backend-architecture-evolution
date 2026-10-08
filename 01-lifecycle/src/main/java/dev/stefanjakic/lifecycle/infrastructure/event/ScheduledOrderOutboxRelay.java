package dev.stefanjakic.lifecycle.infrastructure.event;

import dev.stefanjakic.lifecycle.application.OrderOutboxRelay;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
    name = "outbox.relay.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class ScheduledOrderOutboxRelay {

    private final OrderOutboxRelay relay;

    public ScheduledOrderOutboxRelay(OrderOutboxRelay relay) {
        this.relay = relay;
    }

    @Scheduled(fixedDelayString = "${outbox.relay.fixed-delay-ms:500}")
    public void publishPendingEvents() {
        relay.publishNextBatch();
    }
}
