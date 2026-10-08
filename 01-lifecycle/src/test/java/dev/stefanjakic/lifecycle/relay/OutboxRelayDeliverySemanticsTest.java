package dev.stefanjakic.lifecycle.relay;

import dev.stefanjakic.lifecycle.application.OrderEventTransport;
import dev.stefanjakic.lifecycle.application.OrderOutboxMessage;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelay;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelayRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.infrastructure.persistence.PostgresOrderOutboxRelayRepository;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@SpringBootTest
@Import(OutboxRelayDeliverySemanticsTest.RelayTestConfiguration.class)
class OutboxRelayDeliverySemanticsTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Autowired
    private OrderOutboxRelay relay;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ControllableOrderEventTransport transport;

    @Autowired
    private FailingMarkOutboxRelayRepository relayRepository;

    @BeforeEach
    void reset() {
        jdbcTemplate.update("DELETE FROM order_event_outbox");
        transport.reset();
        relayRepository.reset();
    }

    @Test
    void failedPublicationReturnsEventToPendingForRetry() {
        String orderId = service.createOrder(
            "relay-failure-" + UUID.randomUUID()
        );

        transport.failNextPublish();

        assertEquals(0, relay.publishNextBatch());

        OutboxState failed = stateOf(orderId);

        assertEquals("PENDING", failed.status());
        assertEquals(1, failed.attemptCount());
        assertNotNull(failed.lastError());

        assertEquals(1, relay.publishNextBatch());

        OutboxState retried = stateOf(orderId);

        assertEquals("PUBLISHED", retried.status());
        assertEquals(2, retried.attemptCount());
        assertEquals(1, transport.successfulPublishes());
    }

    @Test
    void expiredClaimCanBeRecoveredByAnotherRelayRun() {
        String orderId = service.createOrder(
            "relay-lease-" + UUID.randomUUID()
        );

        jdbcTemplate.update(
            """
            UPDATE order_event_outbox
            SET status = 'IN_PROGRESS',
                claimed_at = NOW() - INTERVAL '2 minutes',
                attempt_count = 1
            WHERE order_id = ?
            """,
            orderId
        );

        assertEquals(1, relay.publishNextBatch());

        OutboxState state = stateOf(orderId);

        assertEquals("PUBLISHED", state.status());
        assertEquals(2, state.attemptCount());
    }

    @Test
    void markPublishedFailureMakesDuplicateDeliveryPossible() {
        String orderId = service.createOrder(
            "relay-duplicate-" + UUID.randomUUID()
        );

        relayRepository.failNextMarkPublished();

        assertEquals(0, relay.publishNextBatch());

        OutboxState afterFirstSend = stateOf(orderId);

        assertEquals("PENDING", afterFirstSend.status());
        assertEquals(1, afterFirstSend.attemptCount());
        assertEquals(1, transport.successfulPublishes());

        assertEquals(1, relay.publishNextBatch());

        OutboxState afterRetry = stateOf(orderId);

        assertEquals("PUBLISHED", afterRetry.status());
        assertEquals(2, afterRetry.attemptCount());
        assertEquals(2, transport.successfulPublishes());
    }

    private OutboxState stateOf(String orderId) {
        return jdbcTemplate.queryForObject(
            """
            SELECT status, attempt_count, last_error
            FROM order_event_outbox
            WHERE order_id = ?
            """,
            (resultSet, rowNumber) -> new OutboxState(
                resultSet.getString("status"),
                resultSet.getInt("attempt_count"),
                resultSet.getString("last_error")
            ),
            orderId
        );
    }

    record OutboxState(
        String status,
        int attemptCount,
        String lastError
    ) {
    }

    @TestConfiguration
    static class RelayTestConfiguration {

        @Bean
        @Primary
        ControllableOrderEventTransport controllableOrderEventTransport() {
            return new ControllableOrderEventTransport();
        }

        @Bean
        @Primary
        FailingMarkOutboxRelayRepository failingMarkOutboxRelayRepository(
            PostgresOrderOutboxRelayRepository repository
        ) {
            return new FailingMarkOutboxRelayRepository(repository);
        }
    }

    static final class ControllableOrderEventTransport
        implements OrderEventTransport {

        private boolean failNextPublish;
        private int successfulPublishes;

        void failNextPublish() {
            failNextPublish = true;
        }

        int successfulPublishes() {
            return successfulPublishes;
        }

        void reset() {
            failNextPublish = false;
            successfulPublishes = 0;
        }

        @Override
        public void publish(OrderOutboxMessage message) {
            if (failNextPublish) {
                failNextPublish = false;
                throw new SimulatedKafkaFailureException();
            }

            successfulPublishes++;
        }
    }

    static final class FailingMarkOutboxRelayRepository
        implements OrderOutboxRelayRepository {

        private final PostgresOrderOutboxRelayRepository delegate;
        private boolean failNextMarkPublished;

        FailingMarkOutboxRelayRepository(
            PostgresOrderOutboxRelayRepository delegate
        ) {
            this.delegate = delegate;
        }

        void failNextMarkPublished() {
            failNextMarkPublished = true;
        }

        void reset() {
            failNextMarkPublished = false;
        }

        @Override
        public List<OrderOutboxMessage> claimBatch(
            int batchSize,
            Duration leaseTimeout
        ) {
            return delegate.claimBatch(batchSize, leaseTimeout);
        }

        @Override
        public void markPublished(UUID eventId) {
            if (failNextMarkPublished) {
                failNextMarkPublished = false;
                throw new SimulatedMarkPublishedFailureException();
            }

            delegate.markPublished(eventId);
        }

        @Override
        public void releaseForRetry(
            UUID eventId,
            String errorMessage
        ) {
            delegate.releaseForRetry(eventId, errorMessage);
        }
    }

    static final class SimulatedKafkaFailureException
        extends RuntimeException {
    }

    static final class SimulatedMarkPublishedFailureException
        extends RuntimeException {
    }
}
