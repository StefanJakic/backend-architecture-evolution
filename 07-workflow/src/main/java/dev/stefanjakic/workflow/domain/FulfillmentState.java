package dev.stefanjakic.workflow.domain;

import java.util.Objects;

public record FulfillmentState(
    String orderId,
    long amountCents,
    FulfillmentPhase phase,
    String reservationId,
    String paymentId,
    String shipmentId
) {

    public FulfillmentState {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException(
                "orderId must not be blank"
            );
        }

        if (amountCents <= 0) {
            throw new IllegalArgumentException(
                "amountCents must be positive"
            );
        }

        Objects.requireNonNull(
            phase,
            "phase must not be null"
        );

        switch (phase) {
            case STARTED -> {
            }
            case INVENTORY_RESERVED ->
                requireId(
                    reservationId,
                    "reservationId"
                );
            case PAYMENT_CHARGED -> {
                requireId(
                    reservationId,
                    "reservationId"
                );
                requireId(
                    paymentId,
                    "paymentId"
                );
            }
            case COMPLETED -> {
                requireId(
                    reservationId,
                    "reservationId"
                );
                requireId(
                    paymentId,
                    "paymentId"
                );
                requireId(
                    shipmentId,
                    "shipmentId"
                );
            }
            case COMPENSATED, COMPENSATION_FAILED -> {
            }
        }
    }

    public FulfillmentResult result() {
        if (phase != FulfillmentPhase.COMPLETED) {
            throw new IllegalStateException(
                "Workflow is not completed: " + phase
            );
        }

        return new FulfillmentResult(
            reservationId,
            paymentId,
            shipmentId
        );
    }

    private static void requireId(
        String value,
        String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be blank"
            );
        }
    }
}
