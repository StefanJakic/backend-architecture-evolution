package dev.stefanjakic.lifecycle.application;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

public interface OrderOutboxRelayRepository {

    List<OrderOutboxMessage> claimBatch(
        int batchSize,
        Duration leaseTimeout
    );

    void markPublished(UUID eventId);

    void releaseForRetry(UUID eventId, String errorMessage);
}
