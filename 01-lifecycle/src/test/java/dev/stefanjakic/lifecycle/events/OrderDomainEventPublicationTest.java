package dev.stefanjakic.lifecycle.events;

import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Import(OrderDomainEventPublicationTest.EventTestConfiguration.class)
class OrderDomainEventPublicationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Autowired
    private CapturingOrderEventListener listener;

    @BeforeEach
    void clearEvents() {
        listener.clear();
    }

    @Test
    void publishesCommittedLifecycleEvents() {
        String orderId = service.createOrder("event-" + UUID.randomUUID());
        service.confirmOrder(orderId);

        List<OrderDomainEvent> events = listener.events();

        assertEquals(2, events.size());
        assertInstanceOf(OrderCreated.class, events.get(0));
        assertInstanceOf(OrderConfirmed.class, events.get(1));
        assertEquals(orderId, events.get(0).orderId());
        assertEquals(orderId, events.get(1).orderId());
    }

    @Test
    void idempotentRetryDoesNotRepublishOrderCreated() {
        String idempotencyKey = "event-retry-" + UUID.randomUUID();

        String orderId = service.createOrder(idempotencyKey);
        listener.clear();

        String retriedOrderId = service.createOrder(idempotencyKey);

        assertEquals(orderId, retriedOrderId);
        assertTrue(listener.events().isEmpty());
    }

    @Test
    void failedTransitionPublishesNothing() {
        String orderId = service.createOrder("event-failure-" + UUID.randomUUID());
        listener.clear();

        assertThrows(
            InvalidOrderTransitionException.class,
            () -> service.shipOrder(orderId)
        );

        assertTrue(listener.events().isEmpty());
    }

    @TestConfiguration
    static class EventTestConfiguration {

        @Bean
        CapturingOrderEventListener capturingOrderEventListener() {
            return new CapturingOrderEventListener();
        }
    }

    static final class CapturingOrderEventListener {

        private final List<OrderDomainEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void on(OrderDomainEvent event) {
            events.add(event);
        }

        List<OrderDomainEvent> events() {
            return List.copyOf(events);
        }

        void clear() {
            events.clear();
        }
    }
}
