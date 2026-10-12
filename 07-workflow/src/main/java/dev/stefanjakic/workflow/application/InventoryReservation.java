package dev.stefanjakic.workflow.application;

public interface InventoryReservation {

    String reserve(
        String orderId,
        String operationKey
    );

    void release(String reservationId);
}
