package dev.stefanjakic.lifecycle.infrastructure.cache;

import dev.stefanjakic.lifecycle.application.OrderStatusCache;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(
    name = "order.status-cache.enabled",
    havingValue = "false"
)
public class NoOpOrderStatusCache implements OrderStatusCache {

    @Override
    public Optional<OrderStatus> find(String orderId) {
        return Optional.empty();
    }

    @Override
    public void put(String orderId, OrderStatus status) {
    }

    @Override
    public void invalidate(String orderId) {
    }
}
