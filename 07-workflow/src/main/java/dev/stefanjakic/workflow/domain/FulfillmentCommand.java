package dev.stefanjakic.workflow.domain;

public record FulfillmentCommand(
    String orderId,
    long amountCents
) {

    public FulfillmentCommand {
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
    }
}
