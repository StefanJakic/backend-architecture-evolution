package dev.stefanjakic.workflow.application;

public interface ShipmentCreation {

    String createShipment(
        String orderId,
        String reservationId,
        String paymentId,
        String operationKey
    );
}
