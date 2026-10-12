package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentResult;

import java.util.Objects;

public final class FulfillmentWorkflow {

    private final InventoryReservation inventoryReservation;
    private final PaymentCharging paymentCharging;
    private final ShipmentCreation shipmentCreation;

    public FulfillmentWorkflow(
        InventoryReservation inventoryReservation,
        PaymentCharging paymentCharging,
        ShipmentCreation shipmentCreation
    ) {
        this.inventoryReservation = Objects.requireNonNull(
            inventoryReservation,
            "inventoryReservation must not be null"
        );

        this.paymentCharging = Objects.requireNonNull(
            paymentCharging,
            "paymentCharging must not be null"
        );

        this.shipmentCreation = Objects.requireNonNull(
            shipmentCreation,
            "shipmentCreation must not be null"
        );
    }

    public FulfillmentResult fulfill(
        FulfillmentCommand command
    ) {
        Objects.requireNonNull(
            command,
            "command must not be null"
        );

        String reservationId =
            inventoryReservation.reserve(
                command.orderId()
            );

        String paymentId =
            paymentCharging.charge(
                command.orderId(),
                command.amountCents()
            );

        String shipmentId =
            shipmentCreation.createShipment(
                command.orderId(),
                reservationId,
                paymentId
            );

        return new FulfillmentResult(
            reservationId,
            paymentId,
            shipmentId
        );
    }
}
