package dev.stefanjakic.workflow.application;

public interface InventoryReservation {

    String reserve(String orderId);

    void release(String reservationId);
}
