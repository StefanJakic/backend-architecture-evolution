package dev.stefanjakic.inventory.concurrency;

import dev.stefanjakic.inventory.application.InventoryRepository;
import dev.stefanjakic.inventory.application.InventoryService;
import dev.stefanjakic.inventory.domain.InsufficientInventoryException;
import dev.stefanjakic.inventory.domain.InventoryItem;
import dev.stefanjakic.inventory.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class HighContentionInventoryTest extends PostgresIntegrationTest {

    private static final int INITIAL_STOCK = 10;
    private static final int CONSUMERS = 50;

    @Autowired
    private InventoryService service;

    @Autowired
    private InventoryRepository repository;

    @Test
    void successfulConsumesExactlyMatchAvailableStock()
        throws Exception {

        String sku = "sku-" + UUID.randomUUID();
        repository.insert(new InventoryItem(sku, INITIAL_STOCK));

        CountDownLatch ready = new CountDownLatch(CONSUMERS);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(CONSUMERS);

        try {
            List<Future<AttemptResult>> futures = new ArrayList<>();

            for (int i = 0; i < CONSUMERS; i++) {
                futures.add(
                    pool.submit(() -> attemptTake(sku, ready, start))
                );
            }

            assertTrue(
                ready.await(5, TimeUnit.SECONDS),
                "All consumers should be ready before the common start"
            );

            start.countDown();

            List<AttemptResult> results = new ArrayList<>();

            for (Future<AttemptResult> future : futures) {
                results.add(future.get(15, TimeUnit.SECONDS));
            }

            long successes = results.stream()
                .filter(result -> result == AttemptResult.SUCCESS)
                .count();

            long insufficient = results.stream()
                .filter(result -> result == AttemptResult.INSUFFICIENT)
                .count();

            int finalStock = repository.findBySku(sku)
                .orElseThrow()
                .availableQuantity();

            assertEquals(INITIAL_STOCK, successes);
            assertEquals(CONSUMERS - INITIAL_STOCK, insufficient);
            assertEquals(0, finalStock);

            assertEquals(
                successes,
                INITIAL_STOCK - finalStock,
                "Every successful take must correspond to stock that actually disappeared"
            );
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private AttemptResult attemptTake(
        String sku,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        ready.countDown();

        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                    "Timed out waiting for common start"
                );
            }

            service.take(sku, 1);
            return AttemptResult.SUCCESS;
        } catch (InsufficientInventoryException exception) {
            return AttemptResult.INSUFFICIENT;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for common start",
                exception
            );
        }
    }

    private enum AttemptResult {
        SUCCESS,
        INSUFFICIENT
    }
}
