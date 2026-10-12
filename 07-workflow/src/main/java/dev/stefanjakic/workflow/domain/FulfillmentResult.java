package dev.stefanjakic.workflow.domain;

public record FulfillmentResult(
    String reservationId,
    String paymentId,
    String shipmentId
) {
}
