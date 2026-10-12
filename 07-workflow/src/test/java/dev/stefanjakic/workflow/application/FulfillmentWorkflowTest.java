package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentPhase;
import dev.stefanjakic.workflow.domain.FulfillmentState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FulfillmentWorkflowTest {

    @Test
    void completedWorkflowReturnsStoredResultWithoutRepeatingSideEffects() {
        List<String> calls = new ArrayList<>();
        var repository =
            new InMemoryFulfillmentStateRepository();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    calls.add("release");
                }
            };

        PaymentCharging payment =
            new PaymentCharging() {
                @Override
                public String charge(
                    String orderId,
                    long amountCents
                ) {
                    calls.add("charge");
                    return "payment-1";
                }

                @Override
                public void refund(String paymentId) {
                    calls.add("refund");
                }
            };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("ship");
                return "shipment-1";
            };

        var workflow = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            repository
        );

        var command =
            new FulfillmentCommand(
                "order-1",
                10_000
            );

        var first = workflow.fulfill(command);
        var second = workflow.fulfill(command);

        assertEquals(first, second);

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "ship"
            ),
            calls
        );

        assertEquals(
            FulfillmentPhase.COMPLETED,
            repository.findByOrderId("order-1")
                .orElseThrow()
                .phase()
        );
    }

    @Test
    void paymentFailureCompensatesReservationAndCheckpointsOutcome() {
        List<String> calls = new ArrayList<>();
        var repository =
            new InMemoryFulfillmentStateRepository();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    calls.add("release");
                }
            };

        PaymentCharging payment =
            new PaymentCharging() {
                @Override
                public String charge(
                    String orderId,
                    long amountCents
                ) {
                    calls.add("charge");
                    throw new IllegalStateException(
                        "payment rejected"
                    );
                }

                @Override
                public void refund(String paymentId) {
                    calls.add("refund");
                }
            };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("ship");
                return "shipment-1";
            };

        var workflow = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            repository
        );

        assertThrows(
            IllegalStateException.class,
            () -> workflow.fulfill(
                new FulfillmentCommand(
                    "order-1",
                    10_000
                )
            )
        );

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "release"
            ),
            calls
        );

        assertEquals(
            FulfillmentPhase.COMPENSATED,
            repository.findByOrderId("order-1")
                .orElseThrow()
                .phase()
        );
    }

    @Test
    void shipmentFailureCompensatesInReverseOrder() {
        List<String> calls = new ArrayList<>();
        var repository =
            new InMemoryFulfillmentStateRepository();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    calls.add("release");
                }
            };

        PaymentCharging payment =
            new PaymentCharging() {
                @Override
                public String charge(
                    String orderId,
                    long amountCents
                ) {
                    calls.add("charge");
                    return "payment-1";
                }

                @Override
                public void refund(String paymentId) {
                    calls.add("refund");
                }
            };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("ship");
                throw new IllegalStateException(
                    "shipment unavailable"
                );
            };

        var workflow = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            repository
        );

        assertThrows(
            IllegalStateException.class,
            () -> workflow.fulfill(
                new FulfillmentCommand(
                    "order-1",
                    10_000
                )
            )
        );

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "ship",
                "refund",
                "release"
            ),
            calls
        );

        assertEquals(
            FulfillmentPhase.COMPENSATED,
            repository.findByOrderId("order-1")
                .orElseThrow()
                .phase()
        );
    }

    @Test
    void compensationFailureKeepsOriginalFailurePrimaryAndDurable() {
        var repository =
            new InMemoryFulfillmentStateRepository();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    throw new IllegalStateException(
                        "release failed"
                    );
                }
            };

        PaymentCharging payment =
            new PaymentCharging() {
                @Override
                public String charge(
                    String orderId,
                    long amountCents
                ) {
                    return "payment-1";
                }

                @Override
                public void refund(String paymentId) {
                    throw new IllegalStateException(
                        "refund failed"
                    );
                }
            };

        IllegalStateException shipmentFailure =
            new IllegalStateException(
                "shipment unavailable"
            );

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                throw shipmentFailure;
            };

        var workflow = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping,
            repository
        );

        var thrown = assertThrows(
            IllegalStateException.class,
            () -> workflow.fulfill(
                new FulfillmentCommand(
                    "order-1",
                    10_000
                )
            )
        );

        assertSame(
            shipmentFailure,
            thrown
        );

        assertEquals(
            2,
            thrown.getSuppressed().length
        );

        assertEquals(
            FulfillmentPhase.COMPENSATION_FAILED,
            repository.findByOrderId("order-1")
                .orElseThrow()
                .phase()
        );
    }

    private static final class InMemoryFulfillmentStateRepository
        implements FulfillmentStateRepository {

        private final Map<String, FulfillmentState> states =
            new HashMap<>();

        @Override
        public void createIfAbsent(FulfillmentCommand command) {
            states.putIfAbsent(
                command.orderId(),
                new FulfillmentState(
                    command.orderId(),
                    command.amountCents(),
                    FulfillmentPhase.STARTED,
                    null,
                    null,
                    null
                )
            );
        }

        @Override
        public Optional<FulfillmentState> findByOrderId(
            String orderId
        ) {
            return Optional.ofNullable(
                states.get(orderId)
            );
        }

        @Override
        public void markInventoryReserved(
            String orderId,
            String reservationId
        ) {
            FulfillmentState current =
                require(orderId, FulfillmentPhase.STARTED);

            states.put(
                orderId,
                new FulfillmentState(
                    current.orderId(),
                    current.amountCents(),
                    FulfillmentPhase.INVENTORY_RESERVED,
                    reservationId,
                    null,
                    null
                )
            );
        }

        @Override
        public void markPaymentCharged(
            String orderId,
            String paymentId
        ) {
            FulfillmentState current =
                require(
                    orderId,
                    FulfillmentPhase.INVENTORY_RESERVED
                );

            states.put(
                orderId,
                new FulfillmentState(
                    current.orderId(),
                    current.amountCents(),
                    FulfillmentPhase.PAYMENT_CHARGED,
                    current.reservationId(),
                    paymentId,
                    null
                )
            );
        }

        @Override
        public void markCompleted(
            String orderId,
            String shipmentId
        ) {
            FulfillmentState current =
                require(
                    orderId,
                    FulfillmentPhase.PAYMENT_CHARGED
                );

            states.put(
                orderId,
                new FulfillmentState(
                    current.orderId(),
                    current.amountCents(),
                    FulfillmentPhase.COMPLETED,
                    current.reservationId(),
                    current.paymentId(),
                    shipmentId
                )
            );
        }

        @Override
        public void markCompensated(String orderId) {
            FulfillmentState current =
                states.get(orderId);

            states.put(
                orderId,
                new FulfillmentState(
                    current.orderId(),
                    current.amountCents(),
                    FulfillmentPhase.COMPENSATED,
                    current.reservationId(),
                    current.paymentId(),
                    current.shipmentId()
                )
            );
        }

        @Override
        public void markCompensationFailed(String orderId) {
            FulfillmentState current =
                states.get(orderId);

            states.put(
                orderId,
                new FulfillmentState(
                    current.orderId(),
                    current.amountCents(),
                    FulfillmentPhase.COMPENSATION_FAILED,
                    current.reservationId(),
                    current.paymentId(),
                    current.shipmentId()
                )
            );
        }

        private FulfillmentState require(
            String orderId,
            FulfillmentPhase expected
        ) {
            FulfillmentState current = states.get(orderId);

            if (current == null || current.phase() != expected) {
                throw new IllegalStateException(
                    "Unexpected workflow state"
                );
            }

            return current;
        }
    }
}
