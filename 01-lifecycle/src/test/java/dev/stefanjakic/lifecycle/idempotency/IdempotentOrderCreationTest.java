package dev.stefanjakic.lifecycle.idempotency;

import dev.stefanjakic.lifecycle.application.OrderAuditAction;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
class IdempotentOrderCreationTest extends PostgresIntegrationTest {

    @Autowired
    private OrderService service;

    @Test
    void concurrentRequestsWithTheSameKeyReturnOneOrderAndOneCreateAudit() throws Exception {
        String idempotencyKey = "concurrent-" + UUID.randomUUID();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<String> first = executor.submit(
                () -> createAfterSignal(start, idempotencyKey)
            );
            Future<String> second = executor.submit(
                () -> createAfterSignal(start, idempotencyKey)
            );

            start.countDown();

            String firstOrderId = first.get(10, TimeUnit.SECONDS);
            String secondOrderId = second.get(10, TimeUnit.SECONDS);

            assertEquals(firstOrderId, secondOrderId);

            var history = service.historyOf(firstOrderId);

            assertEquals(1, history.size());
            assertEquals(OrderAuditAction.CREATE, history.get(0).action());
        } finally {
            executor.shutdownNow();
        }
    }

    private String createAfterSignal(
        CountDownLatch start,
        String idempotencyKey
    ) throws InterruptedException {
        start.await();
        return service.createOrder(idempotencyKey);
    }
}
