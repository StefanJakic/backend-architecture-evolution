package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FulfillmentWorkflowTest {

    @Test
    void executesStepsInBusinessOrderAndReturnsTheirIds() {
        List<String> calls = new ArrayList<>();

        InventoryReservation inventory = orderId -> {
            calls.add("inventory");
            return "reservation-1";
        };

        PaymentCharging payment = (orderId, amountCents) -> {
            calls.add("payment");
            return "payment-1";
        };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("shipment");
                assertEquals(
                    "reservation-1",
                    reservationId
                );
                assertEquals(
                    "payment-1",
                    paymentId
                );
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
                "inventory",
                "payment",
                "shipment"
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
    void stopsImmediatelyWhenInventoryReservationFails() {
        List<String> calls = new ArrayList<>();

        InventoryReservation inventory = orderId -> {
            calls.add("inventory");
            throw new IllegalStateException(
                "inventory unavailable"
            );
        };

        PaymentCharging payment = (orderId, amountCents) -> {
            calls.add("payment");
            return "payment-1";
        };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("shipment");
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
            List.of("inventory"),
            calls
        );
    }

    @Test
    void paymentFailureLeavesEarlierReservationSideEffect() {
        List<String> calls = new ArrayList<>();
        List<String> reservedOrders = new ArrayList<>();

        InventoryReservation inventory = orderId -> {
            calls.add("inventory");
            reservedOrders.add(orderId);
            return "reservation-1";
        };

        PaymentCharging payment = (orderId, amountCents) -> {
            calls.add("payment");
            throw new IllegalStateException(
                "payment rejected"
            );
        };

        ShipmentCreation shipping =
            (orderId, reservationId, paymentId) -> {
                calls.add("shipment");
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
            List.of(
                "inventory",
                "payment"
            ),
            calls
        );

        assertEquals(
            List.of("order-1"),
            reservedOrders,
            "A completed earlier side effect still exists after a later step fails"
        );
    }
}
