package dev.stefanjakic.inventory.concurrency;

import dev.stefanjakic.inventory.application.InventoryRepository;
import dev.stefanjakic.inventory.application.InventoryService;
import dev.stefanjakic.inventory.domain.InsufficientInventoryException;
import dev.stefanjakic.inventory.domain.InventoryItem;
import dev.stefanjakic.inventory.infrastructure.persistence.PostgresInventoryRepository;
import dev.stefanjakic.inventory.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ConcurrentInventoryRaceTest.RaceTestConfiguration.class)
class ConcurrentInventoryRaceTest extends PostgresIntegrationTest {

    @Autowired
    private InventoryService service;

    @Autowired
    private CoordinatedInventoryRepository coordinatedRepository;

    @Autowired
    private PostgresInventoryRepository postgresRepository;

    @Test
    void atomicDecrementAllowsOnlyOneConsumerForOneUnit()
        throws Exception {

        String sku = "sku-" + UUID.randomUUID();
        postgresRepository.insert(new InventoryItem(sku, 1));

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            var firstAttempt = pool.submit(
                () -> attemptTake(sku)
            );

            var secondAttempt = pool.submit(
                () -> attemptTake(sku)
            );

            assertTrue(
                coordinatedRepository.awaitBothAttempts(
                    Duration.ofSeconds(5)
                ),
                "Both consumers should reach the atomic decrement"
            );

            coordinatedRepository.releaseAttempts();

            AttemptResult firstResult =
                firstAttempt.get(5, TimeUnit.SECONDS);
            AttemptResult secondResult =
                secondAttempt.get(5, TimeUnit.SECONDS);

            List<AttemptResult> results = List.of(
                firstResult,
                secondResult
            );

            assertEquals(
                1,
                results.stream()
                    .filter(result -> result == AttemptResult.SUCCESS)
                    .count()
            );

            assertEquals(
                1,
                results.stream()
                    .filter(result -> result == AttemptResult.INSUFFICIENT)
                    .count()
            );

            assertEquals(
                0,
                postgresRepository.findBySku(sku)
                    .orElseThrow()
                    .availableQuantity()
            );
        } finally {
            coordinatedRepository.releaseAttempts();
            pool.shutdownNow();
        }
    }

    private AttemptResult attemptTake(String sku) {
        try {
            service.take(sku, 1);
            return AttemptResult.SUCCESS;
        } catch (InsufficientInventoryException exception) {
            return AttemptResult.INSUFFICIENT;
        }
    }

    private enum AttemptResult {
        SUCCESS,
        INSUFFICIENT
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RaceTestConfiguration {

        @Bean
        @Primary
        CoordinatedInventoryRepository coordinatedInventoryRepository(
            PostgresInventoryRepository delegate
        ) {
            return new CoordinatedInventoryRepository(delegate);
        }
    }

    static final class CoordinatedInventoryRepository
        implements InventoryRepository {

        private final InventoryRepository delegate;
        private final CountDownLatch bothAttemptsReached =
            new CountDownLatch(2);
        private final CountDownLatch releaseAttempts =
            new CountDownLatch(1);

        CoordinatedInventoryRepository(
            InventoryRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public Optional<InventoryItem> findBySku(String sku) {
            return delegate.findBySku(sku);
        }

        @Override
        public Optional<InventoryItem> takeIfAvailable(
            String sku,
            int quantity
        ) {
            bothAttemptsReached.countDown();

            try {
                if (!releaseAttempts.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release inventory attempts"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating inventory race",
                    exception
                );
            }

            return delegate.takeIfAvailable(sku, quantity);
        }

        @Override
        public void insert(InventoryItem item) {
            delegate.insert(item);
        }

        @Override
        public void update(InventoryItem item) {
            delegate.update(item);
        }

        boolean awaitBothAttempts(Duration timeout)
            throws InterruptedException {

            return bothAttemptsReached.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        void releaseAttempts() {
            releaseAttempts.countDown();
        }
    }
}
