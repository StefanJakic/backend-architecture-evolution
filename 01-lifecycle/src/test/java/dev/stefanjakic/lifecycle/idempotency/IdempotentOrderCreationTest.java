package dev.stefanjakic.lifecycle.idempotency;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderCreationIdempotencyRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.infrastructure.persistence.PostgresOrderCreationIdempotencyRepository;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Import(IdempotentOrderCreationTest.IdempotencyTestConfiguration.class)
class IdempotentOrderCreationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Autowired
    private CoordinatingIdempotencyRepository idempotencyRepository;

    @Test
    void concurrentRequestsWithTheSameKeyReturnOneOrderAndOneCreateAudit() throws Exception {
        String idempotencyKey = "concurrent-" + UUID.randomUUID();
        idempotencyRepository.coordinateNextTwoClaimsOf(idempotencyKey);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(
                () -> service.createOrder(idempotencyKey)
            );
            Future<String> second = executor.submit(
                () -> service.createOrder(idempotencyKey)
            );

            assertTrue(idempotencyRepository.awaitBothClaims());
            idempotencyRepository.releaseBothRequests();

            String firstOrderId = first.get(30, TimeUnit.SECONDS);
            String secondOrderId = second.get(30, TimeUnit.SECONDS);

            assertEquals(firstOrderId, secondOrderId);

            var history = service.historyOf(firstOrderId);

            assertEquals(1, history.size());
            assertEquals(OrderAuditAction.CREATE, history.get(0).action());
        } finally {
            idempotencyRepository.stopCoordinating();
            executor.shutdownNow();
        }
    }

    @TestConfiguration
    static class IdempotencyTestConfiguration {

        @Bean
        @Primary
        CoordinatingIdempotencyRepository coordinatingIdempotencyRepository(
            PostgresOrderCreationIdempotencyRepository repository
        ) {
            return new CoordinatingIdempotencyRepository(repository);
        }
    }

    static final class CoordinatingIdempotencyRepository
        implements OrderCreationIdempotencyRepository {

        private final PostgresOrderCreationIdempotencyRepository delegate;

        private volatile String coordinatedKey;
        private volatile CountDownLatch bothAtClaim = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);

        CoordinatingIdempotencyRepository(
            PostgresOrderCreationIdempotencyRepository delegate
        ) {
            this.delegate = delegate;
        }

        void coordinateNextTwoClaimsOf(String idempotencyKey) {
            coordinatedKey = idempotencyKey;
            bothAtClaim = new CountDownLatch(2);
            release = new CountDownLatch(1);
        }

        boolean awaitBothClaims() throws InterruptedException {
            return bothAtClaim.await(10, TimeUnit.SECONDS);
        }

        void releaseBothRequests() {
            release.countDown();
        }

        void stopCoordinating() {
            coordinatedKey = null;
            release.countDown();
        }

        @Override
        public Optional<String> findCompletedOrderId(String idempotencyKey) {
            return delegate.findCompletedOrderId(idempotencyKey);
        }

        @Override
        public boolean tryClaim(String idempotencyKey) {
            if (idempotencyKey.equals(coordinatedKey)) {
                bothAtClaim.countDown();
                awaitRelease();
            }

            return delegate.tryClaim(idempotencyKey);
        }

        @Override
        public void complete(String idempotencyKey, String orderId) {
            delegate.complete(idempotencyKey, orderId);
        }

        private void awaitRelease() {
            try {
                if (!release.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release concurrent idempotency claims"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating idempotency claims",
                    exception
                );
            }
        }
    }
}
