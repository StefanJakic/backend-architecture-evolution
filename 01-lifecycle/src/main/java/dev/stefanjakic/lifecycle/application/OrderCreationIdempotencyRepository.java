package dev.stefanjakic.lifecycle.application;

import java.util.Optional;

public interface OrderCreationIdempotencyRepository {

    Optional<String> findCompletedOrderId(String idempotencyKey);

    boolean tryClaim(String idempotencyKey);

    void complete(String idempotencyKey, String orderId);
}
