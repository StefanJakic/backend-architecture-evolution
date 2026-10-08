package dev.stefanjakic.lifecycle.outbox;

import dev.stefanjakic.lifecycle.application.OrderOutboxRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.domain.event.OrderDomainEvent;
import dev.stefanjakic.lifecycle.infrastructure.persistence.PostgresOrderOutboxRepository;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Import(TransactionalOutboxTest.OutboxTestConfiguration.class)
class TransactionalOutboxTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FailingOrderOutboxRepository outboxRepository;

    @Test
    void persistsReleasedDomainEventsAsDurableOutboxRows() {
        String orderId = service.createOrder("outbox-" + UUID.randomUUID());
        service.confirmOrder(orderId);

        List<String> eventTypes = jdbcTemplate.queryForList(
            """
            SELECT event_type
            FROM order_event_outbox
            WHERE order_id = ?
            """,
            String.class,
            orderId
        );

        assertEquals(2, eventTypes.size());
        assertTrue(eventTypes.contains("OrderCreated"));
        assertTrue(eventTypes.contains("OrderConfirmed"));

        String payload = jdbcTemplate.queryForObject(
            """
            SELECT payload::text
            FROM order_event_outbox
            WHERE order_id = ?
              AND event_type = 'OrderConfirmed'
            """,
            String.class,
            orderId
        );

        assertTrue(payload.contains(orderId));
    }

    @Test
    void idempotentRetryDoesNotDuplicateCreateOutboxEvent() {
        String idempotencyKey = "outbox-retry-" + UUID.randomUUID();

        String firstOrderId = service.createOrder(idempotencyKey);
        String retriedOrderId = service.createOrder(idempotencyKey);

        assertEquals(firstOrderId, retriedOrderId);
        assertEquals(1, outboxCount(firstOrderId));
    }

    @Test
    void invalidTransitionDoesNotAppendOutboxEvent() {
        String orderId = service.createOrder(
            "outbox-invalid-" + UUID.randomUUID()
        );

        assertThrows(
            InvalidOrderTransitionException.class,
            () -> service.shipOrder(orderId)
        );

        assertEquals(1, outboxCount(orderId));
    }

    @Test
    void outboxFailureRollsBackOrderStateAndAuditHistory() {
        String orderId = service.createOrder(
            "outbox-rollback-" + UUID.randomUUID()
        );

        outboxRepository.failNextAppend();

        assertThrows(
            SimulatedOutboxFailureException.class,
            () -> service.confirmOrder(orderId)
        );

        assertEquals(OrderStatus.CREATED, service.statusOf(orderId));
        assertEquals(1, service.historyOf(orderId).size());
        assertEquals(1, outboxCount(orderId));
    }

    @Test
    void failedCreateDoesNotConsumeIdempotencyKey() {
        String idempotencyKey =
            "outbox-create-rollback-" + UUID.randomUUID();

        outboxRepository.failNextAppend();

        assertThrows(
            SimulatedOutboxFailureException.class,
            () -> service.createOrder(idempotencyKey)
        );

        String retriedOrderId = service.createOrder(idempotencyKey);

        assertEquals(1, outboxCount(retriedOrderId));

        Integer idempotencyRecords = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM order_creation_idempotency
            WHERE idempotency_key = ?
            """,
            Integer.class,
            idempotencyKey
        );

        assertEquals(1, idempotencyRecords);
    }

    private int outboxCount(String orderId) {
        Integer count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM order_event_outbox
            WHERE order_id = ?
            """,
            Integer.class,
            orderId
        );

        return count == null ? 0 : count;
    }

    @TestConfiguration
    static class OutboxTestConfiguration {

        @Bean
        @Primary
        FailingOrderOutboxRepository failingOrderOutboxRepository(
            PostgresOrderOutboxRepository repository
        ) {
            return new FailingOrderOutboxRepository(repository);
        }
    }

    static final class FailingOrderOutboxRepository
        implements OrderOutboxRepository {

        private final PostgresOrderOutboxRepository delegate;
        private volatile boolean failNextAppend;

        FailingOrderOutboxRepository(PostgresOrderOutboxRepository delegate) {
            this.delegate = delegate;
        }

        void failNextAppend() {
            failNextAppend = true;
        }

        @Override
        public void append(OrderDomainEvent event) {
            if (failNextAppend) {
                failNextAppend = false;
                throw new SimulatedOutboxFailureException();
            }

            delegate.append(event);
        }
    }

    static final class SimulatedOutboxFailureException
        extends RuntimeException {
    }
}
