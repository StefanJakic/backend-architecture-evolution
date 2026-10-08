package dev.stefanjakic.lifecycle.concurrency;

import dev.stefanjakic.lifecycle.application.ConcurrentOrderModificationException;
import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.infrastructure.persistence.PostgresOrderRepository;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
@Import(ConcurrentOrderTransitionTest.ConcurrencyTestConfiguration.class)
class ConcurrentOrderTransitionTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Autowired
    private CoordinatingOrderRepository repository;

    @Test
    void onlyOneTransitionCommitsWhenTwoRequestsLoadedTheSameVersion() throws Exception {
        String orderId = service.createOrder();
        repository.coordinateNextTwoLoadsOf(orderId);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Throwable> confirm = executor.submit(
                () -> captureFailure(() -> service.confirmOrder(orderId))
            );
            Future<Throwable> cancel = executor.submit(
                () -> captureFailure(() -> service.cancelOrder(orderId))
            );

            assertTrue(repository.awaitBothLoads());
            repository.releaseBothRequests();

            Throwable confirmFailure = confirm.get(10, TimeUnit.SECONDS);
            Throwable cancelFailure = cancel.get(10, TimeUnit.SECONDS);

            long successfulRequests = successfulCount(confirmFailure, cancelFailure);
            Throwable rejectedRequest = rejectedFailure(confirmFailure, cancelFailure);

            assertEquals(1, successfulRequests);
            assertInstanceOf(ConcurrentOrderModificationException.class, rejectedRequest);

            OrderStatus finalStatus = service.statusOf(orderId);
            assertTrue(
                finalStatus == OrderStatus.CONFIRMED || finalStatus == OrderStatus.CANCELLED
            );
        } finally {
            repository.stopCoordinating();
            executor.shutdownNow();
        }
    }

    private static Throwable captureFailure(Runnable operation) {
        try {
            operation.run();
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private static long successfulCount(Throwable first, Throwable second) {
        return java.util.stream.Stream.of(first, second)
            .filter(failure -> failure == null)
            .count();
    }

    private static Throwable rejectedFailure(Throwable first, Throwable second) {
        return first != null ? first : second;
    }

    @TestConfiguration
    static class ConcurrencyTestConfiguration {

        @Bean
        @Primary
        CoordinatingOrderRepository coordinatingOrderRepository(
            PostgresOrderRepository repository
        ) {
            return new CoordinatingOrderRepository(repository);
        }
    }

    static final class CoordinatingOrderRepository implements OrderRepository {

        private final PostgresOrderRepository delegate;

        private volatile String coordinatedOrderId;
        private volatile CountDownLatch bothLoaded = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);

        CoordinatingOrderRepository(PostgresOrderRepository delegate) {
            this.delegate = delegate;
        }

        void coordinateNextTwoLoadsOf(String orderId) {
            coordinatedOrderId = orderId;
            bothLoaded = new CountDownLatch(2);
            release = new CountDownLatch(1);
        }

        boolean awaitBothLoads() throws InterruptedException {
            return bothLoaded.await(5, TimeUnit.SECONDS);
        }

        void releaseBothRequests() {
            release.countDown();
        }

        void stopCoordinating() {
            coordinatedOrderId = null;
            release.countDown();
        }

        @Override
        public Order save(Order order) {
            return delegate.save(order);
        }

        @Override
        public Optional<Order> findById(String id) {
            Optional<Order> order = delegate.findById(id);

            if (id.equals(coordinatedOrderId)) {
                bothLoaded.countDown();
                awaitRelease();
            }

            return order;
        }

        private void awaitRelease() {
            try {
                if (!release.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to release concurrent requests");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while coordinating concurrent requests", exception);
            }
        }
    }
}
