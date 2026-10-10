package dev.stefanjakic.lifecycle.cache;

import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.infrastructure.cache.RedisOrderStatusCache;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedisOrderStatusCacheTest {

    private static final GenericContainer<?> REDIS =
        new GenericContainer<>("redis:8-alpine")
            .withExposedPorts(6379);

    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redisTemplate;

    @BeforeAll
    static void startRedis() {
        REDIS.start();

        connectionFactory = new LettuceConnectionFactory(
            REDIS.getHost(),
            REDIS.getMappedPort(6379)
        );
        connectionFactory.afterPropertiesSet();

        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
    }

    @AfterAll
    static void stopRedis() {
        connectionFactory.destroy();
        REDIS.stop();
    }

    @Test
    void storesStatusWithFiniteTtl() {
        var cache = new RedisOrderStatusCache(
            redisTemplate,
            new SimpleMeterRegistry(),
            Duration.ofMinutes(5)
        );

        String orderId = UUID.randomUUID().toString();

        cache.put(orderId, OrderStatus.CREATED);

        assertEquals(
            OrderStatus.CREATED,
            cache.find(orderId).orElseThrow()
        );

        Long ttlSeconds = redisTemplate.getExpire(
            "order:status:" + orderId
        );

        assertTrue(ttlSeconds != null && ttlSeconds > 0);
    }

    @Test
    void redisFailureDoesNotEscapeCacheBoundary() {
        var unavailableFactory =
            new LettuceConnectionFactory("localhost", 1);
        unavailableFactory.afterPropertiesSet();

        try {
            var unavailableTemplate =
                new StringRedisTemplate(unavailableFactory);
            unavailableTemplate.afterPropertiesSet();

            var cache = new RedisOrderStatusCache(
                unavailableTemplate,
                new SimpleMeterRegistry(),
                Duration.ofMinutes(5)
            );

            assertTrue(cache.find("missing").isEmpty());

            assertDoesNotThrow(
                () -> cache.put("missing", OrderStatus.CREATED)
            );
            assertDoesNotThrow(
                () -> cache.invalidate("missing")
            );
        } finally {
            unavailableFactory.destroy();
        }
    }
}
