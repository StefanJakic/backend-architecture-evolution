package dev.stefanjakic.workflow.persistence;

import dev.stefanjakic.workflow.application.FulfillmentStateRepository;
import dev.stefanjakic.workflow.application.FulfillmentWorkflow;
import dev.stefanjakic.workflow.application.InventoryReservation;
import dev.stefanjakic.workflow.application.PaymentCharging;
import dev.stefanjakic.workflow.application.ShipmentCreation;
import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentPhase;
import dev.stefanjakic.workflow.infrastructure.persistence.PostgresFulfillmentStateRepository;
import dev.stefanjakic.workflow.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(FulfillmentResumeIntegrationTest.TestDependencies.class)
class FulfillmentResumeIntegrationTest
    extends PostgresIntegrationTest {

    @Autowired
    private PostgresFulfillmentStateRepository repository;

    @Test
    void resumesFromPersistedPaymentCheckpointAfterProcessCrash() {
        String orderId =
            "order-" + UUID.randomUUID();

        var inventory = new CountingInventory();
        var payment = new CountingPayment();
        var shipping = new CountingShipping();

        var crashingRepository =
            new CrashAfterPaymentCheckpointRepository(
                repository
            );

        var firstProcess = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            crashingRepository
        );

        assertThrows(
            SimulatedProcessCrash.class,
            () -> firstProcess.fulfill(
                new FulfillmentCommand(
                    orderId,
                    10_000
                )
            )
        );

        var durableAfterCrash =
            repository.findByOrderId(orderId)
                .orElseThrow();

        assertEquals(
            FulfillmentPhase.PAYMENT_CHARGED,
            durableAfterCrash.phase()
        );

        assertEquals(
            1,
            inventory.reserveCalls.get()
        );

        assertEquals(
            1,
            payment.chargeCalls.get()
        );

        assertEquals(
            0,
            shipping.createCalls.get()
        );

        var restartedProcess = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            repository
        );

        var result = restartedProcess.resume(orderId);

        assertEquals(
            "reservation-" + orderId,
            result.reservationId()
        );

        assertEquals(
            "payment-" + orderId,
            result.paymentId()
        );

        assertEquals(
            "shipment-" + orderId,
            result.shipmentId()
        );

        assertEquals(
            1,
            inventory.reserveCalls.get(),
            "Resume must not reserve inventory again"
        );

        assertEquals(
            1,
            payment.chargeCalls.get(),
            "Resume must not charge payment again"
        );

        assertEquals(
            1,
            shipping.createCalls.get()
        );

        assertEquals(
            FulfillmentPhase.COMPLETED,
            repository.findByOrderId(orderId)
                .orElseThrow()
                .phase()
        );
    }

    static final class CrashAfterPaymentCheckpointRepository
        implements FulfillmentStateRepository {

        private final FulfillmentStateRepository delegate;

        CrashAfterPaymentCheckpointRepository(
            FulfillmentStateRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public void createIfAbsent(
            FulfillmentCommand command
        ) {
            delegate.createIfAbsent(command);
        }

        @Override
        public Optional<dev.stefanjakic.workflow.domain.FulfillmentState>
            findByOrderId(String orderId) {

            return delegate.findByOrderId(orderId);
        }

        @Override
        public void markInventoryReserved(
            String orderId,
            String reservationId
        ) {
            delegate.markInventoryReserved(
                orderId,
                reservationId
            );
        }

        @Override
        public void markPaymentCharged(
            String orderId,
            String paymentId
        ) {
            delegate.markPaymentCharged(
                orderId,
                paymentId
            );

            throw new SimulatedProcessCrash();
        }

        @Override
        public void markCompleted(
            String orderId,
            String shipmentId
        ) {
            delegate.markCompleted(
                orderId,
                shipmentId
            );
        }

        @Override
        public void markCompensated(String orderId) {
            delegate.markCompensated(orderId);
        }

        @Override
        public void markCompensationFailed(
            String orderId
        ) {
            delegate.markCompensationFailed(orderId);
        }
    }

    static final class CountingInventory
        implements InventoryReservation {

        private final AtomicInteger reserveCalls =
            new AtomicInteger();

        @Override
        public String reserve(String orderId) {
            reserveCalls.incrementAndGet();
            return "reservation-" + orderId;
        }

        @Override
        public void release(String reservationId) {
        }
    }

    static final class CountingPayment
        implements PaymentCharging {

        private final AtomicInteger chargeCalls =
            new AtomicInteger();

        @Override
        public String charge(
            String orderId,
            long amountCents
        ) {
            chargeCalls.incrementAndGet();
            return "payment-" + orderId;
        }

        @Override
        public void refund(String paymentId) {
        }
    }

    static final class CountingShipping
        implements ShipmentCreation {

        private final AtomicInteger createCalls =
            new AtomicInteger();

        @Override
        public String createShipment(
            String orderId,
            String reservationId,
            String paymentId
        ) {
            createCalls.incrementAndGet();
            return "shipment-" + orderId;
        }
    }

    static final class SimulatedProcessCrash
        extends RuntimeException {
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestDependencies {

        @Bean
        InventoryReservation inventoryReservation() {
            return new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    return "unused-reservation";
                }

                @Override
                public void release(String reservationId) {
                }
            };
        }

        @Bean
        PaymentCharging paymentCharging() {
            return new PaymentCharging() {
                @Override
                public String charge(
                    String orderId,
                    long amountCents
                ) {
                    return "unused-payment";
                }

                @Override
                public void refund(String paymentId) {
                }
            };
        }

        @Bean
        ShipmentCreation shipmentCreation() {
            return (
                orderId,
                reservationId,
                paymentId
            ) -> "unused-shipment";
        }
    }
}
