package dev.stefanjakic.lifecycle.infrastructure.cache;

import dev.stefanjakic.lifecycle.application.OrderStatusCache;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(
    name = "order.status-cache.enabled",
    havingValue = "true",
    matchIfMissing = true
)
public class RedisOrderStatusCache implements OrderStatusCache {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(RedisOrderStatusCache.class);

    private static final String KEY_PREFIX = "order:status:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;
    private final Counter hitCounter;
    private final Counter missCounter;
    private final Counter readErrorCounter;
    private final Counter writeErrorCounter;
    private final Counter invalidationErrorCounter;

    public RedisOrderStatusCache(
        StringRedisTemplate redisTemplate,
        MeterRegistry registry,
        @Value("${order.status-cache.ttl:5m}") Duration ttl
    ) {
        this.redisTemplate = redisTemplate;
        this.ttl = ttl;

        this.hitCounter = requestCounter(registry, "hit");
        this.missCounter = requestCounter(registry, "miss");
        this.readErrorCounter = errorCounter(registry, "read");
        this.writeErrorCounter = errorCounter(registry, "write");
        this.invalidationErrorCounter =
            errorCounter(registry, "invalidate");
    }

    @Override
    public Optional<OrderStatus> find(String orderId) {
        try {
            String value = redisTemplate.opsForValue().get(key(orderId));

            if (value == null) {
                missCounter.increment();
                return Optional.empty();
            }

            try {
                OrderStatus status = OrderStatus.valueOf(value);
                hitCounter.increment();
                return Optional.of(status);
            } catch (IllegalArgumentException exception) {
                readErrorCounter.increment();
                deleteSafely(orderId);
                return Optional.empty();
            }
        } catch (DataAccessException exception) {
            readErrorCounter.increment();

            LOGGER.atWarn()
                .addKeyValue("orderId", orderId)
                .setCause(exception)
                .log("Order status cache read failed; falling back to PostgreSQL");

            return Optional.empty();
        }
    }

    @Override
    public void put(String orderId, OrderStatus status) {
        try {
            redisTemplate.opsForValue().set(
                key(orderId),
                status.name(),
                ttl
            );
        } catch (DataAccessException exception) {
            writeErrorCounter.increment();

            LOGGER.atWarn()
                .addKeyValue("orderId", orderId)
                .setCause(exception)
                .log("Order status cache write failed");
        }
    }

    @Override
    public void invalidate(String orderId) {
        if (
            TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()
        ) {
            TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        deleteSafely(orderId);
                    }
                }
            );
            return;
        }

        deleteSafely(orderId);
    }

    private void deleteSafely(String orderId) {
        try {
            redisTemplate.delete(key(orderId));
        } catch (DataAccessException exception) {
            invalidationErrorCounter.increment();

            LOGGER.atWarn()
                .addKeyValue("orderId", orderId)
                .setCause(exception)
                .log("Order status cache invalidation failed");
        }
    }

    private String key(String orderId) {
        return KEY_PREFIX + orderId;
    }

    private Counter requestCounter(
        MeterRegistry registry,
        String result
    ) {
        return Counter.builder("order.status.cache.requests")
            .description("Order status cache lookups")
            .tag("result", result)
            .register(registry);
    }

    private Counter errorCounter(
        MeterRegistry registry,
        String operation
    ) {
        return Counter.builder("order.status.cache.errors")
            .description("Order status cache operation failures")
            .tag("operation", operation)
            .register(registry);
    }
}
