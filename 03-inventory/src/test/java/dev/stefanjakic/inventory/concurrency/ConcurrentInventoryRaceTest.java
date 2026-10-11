package dev.stefanjakic.inventory.concurrency;

import dev.stefanjakic.inventory.application.InventoryRepository;
import dev.stefanjakic.inventory.application.InventoryService;
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
    void readThenUpdateOversellsWithoutMakingStoredQuantityNegative()
        throws Exception {

        String sku = "sku-" + UUID.randomUUID();
        postgresRepository.insert(new InventoryItem(sku, 1));

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            var firstAttempt = pool.submit(
                () -> service.take(sku, 1)
            );

            var secondAttempt = pool.submit(
                () -> service.take(sku, 1)
            );

            assertTrue(
                coordinatedRepository.awaitBothReads(
                    Duration.ofSeconds(5)
                ),
                "Both transactions should read the original stock"
            );

            coordinatedRepository.releaseReads();

            firstAttempt.get(5, TimeUnit.SECONDS);
            secondAttempt.get(5, TimeUnit.SECONDS);

            int storedQuantity = postgresRepository.findBySku(sku)
                .orElseThrow()
                .availableQuantity();

            assertEquals(0, storedQuantity);
        } finally {
            coordinatedRepository.releaseReads();
            pool.shutdownNow();
        }
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
        private final CountDownLatch bothReadsCompleted =
            new CountDownLatch(2);
        private final CountDownLatch releaseReads =
            new CountDownLatch(1);

        CoordinatedInventoryRepository(
            InventoryRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public Optional<InventoryItem> findBySku(String sku) {
            Optional<InventoryItem> item = delegate.findBySku(sku);

            bothReadsCompleted.countDown();

            try {
                if (!releaseReads.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release inventory reads"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating inventory race",
                    exception
                );
            }

            return item;
        }

        @Override
        public void insert(InventoryItem item) {
            delegate.insert(item);
        }

        @Override
        public void update(InventoryItem item) {
            delegate.update(item);
        }

        boolean awaitBothReads(Duration timeout)
            throws InterruptedException {

            return bothReadsCompleted.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        void releaseReads() {
            releaseReads.countDown();
        }
    }
}
