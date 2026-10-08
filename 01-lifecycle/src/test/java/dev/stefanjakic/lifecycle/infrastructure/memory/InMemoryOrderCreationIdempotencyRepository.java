package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderCreationIdempotencyRepository;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class InMemoryOrderCreationIdempotencyRepository
    implements OrderCreationIdempotencyRepository {

    private final Set<String> claimedKeys = new HashSet<>();
    private final Map<String, String> completedOrders = new HashMap<>();

    @Override
    public Optional<String> findCompletedOrderId(String idempotencyKey) {
        return Optional.ofNullable(completedOrders.get(idempotencyKey));
    }

    @Override
    public boolean tryClaim(String idempotencyKey) {
        if (claimedKeys.contains(idempotencyKey)
            || completedOrders.containsKey(idempotencyKey)) {
            return false;
        }

        claimedKeys.add(idempotencyKey);
        return true;
    }

    @Override
    public void complete(String idempotencyKey, String orderId) {
        claimedKeys.remove(idempotencyKey);
        completedOrders.put(idempotencyKey, orderId);
    }
}
