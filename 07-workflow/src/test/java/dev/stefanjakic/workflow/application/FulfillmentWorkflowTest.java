package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FulfillmentWorkflowTest {

    @Test
    void successfulWorkflowNeedsNoCompensation() {
        List<String> calls = new ArrayList<>();

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
            shipping
        );

        var result = workflow.fulfill(
            new FulfillmentCommand(
                "order-1",
                10_000
            )
        );

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "ship"
            ),
            calls
        );

        assertEquals(
            "reservation-1",
            result.reservationId()
        );

        assertEquals(
            "payment-1",
            result.paymentId()
        );

        assertEquals(
            "shipment-1",
            result.shipmentId()
        );
    }

    @Test
    void inventoryFailureStopsWithoutCompensation() {
        List<String> calls = new ArrayList<>();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    throw new IllegalStateException(
                        "inventory unavailable"
                    );
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
            shipping
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
            List.of("reserve"),
            calls
        );
    }

    @Test
    void paymentFailureReleasesCompletedReservation() {
        List<String> calls = new ArrayList<>();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    calls.add(
                        "release:" + reservationId
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
            shipping
        );

        var failure = assertThrows(
            IllegalStateException.class,
            () -> workflow.fulfill(
                new FulfillmentCommand(
                    "order-1",
                    10_000
                )
            )
        );

        assertEquals(
            "payment rejected",
            failure.getMessage()
        );

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "release:reservation-1"
            ),
            calls
        );
    }

    @Test
    void shipmentFailureCompensatesInReverseOrder() {
        List<String> calls = new ArrayList<>();

        InventoryReservation inventory =
            new InventoryReservation() {
                @Override
                public String reserve(String orderId) {
                    calls.add("reserve");
                    return "reservation-1";
                }

                @Override
                public void release(String reservationId) {
                    calls.add(
                        "release:" + reservationId
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
                    calls.add("charge");
                    return "payment-1";
                }

                @Override
                public void refund(String paymentId) {
                    calls.add(
                        "refund:" + paymentId
                    );
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
            shipping
        );

        var failure = assertThrows(
            IllegalStateException.class,
            () -> workflow.fulfill(
                new FulfillmentCommand(
                    "order-1",
                    10_000
                )
            )
        );

        assertEquals(
            "shipment unavailable",
            failure.getMessage()
        );

        assertEquals(
            List.of(
                "reserve",
                "charge",
                "ship",
                "refund:payment-1",
                "release:reservation-1"
            ),
            calls
        );
    }

    @Test
    void compensationFailuresAreSuppressedOnOriginalFailure() {
        List<String> calls = new ArrayList<>();

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
                    throw new IllegalStateException(
                        "inventory release failed"
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
                    calls.add("charge");
                    return "payment-1";
                }

                @Override
                public void refund(String paymentId) {
                    calls.add("refund");
                    throw new IllegalStateException(
                        "payment refund failed"
                    );
                }
            };

        IllegalStateException shipmentFailure =
            new IllegalStateException(
                "shipment unavailable"
            );

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("ship");
                throw shipmentFailure;
            };

        var workflow = new FulfillmentWorkflow(
            inventory,
            payment,
            shipping
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
            2,
            thrown.getSuppressed().length
        );

        assertEquals(
            "payment refund failed",
            thrown.getSuppressed()[0].getMessage()
        );

        assertEquals(
            "inventory release failed",
            thrown.getSuppressed()[1].getMessage()
        );
    }
}
