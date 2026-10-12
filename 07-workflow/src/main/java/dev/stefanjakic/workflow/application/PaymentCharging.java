package dev.stefanjakic.workflow.application;

public interface PaymentCharging {

    String charge(
        String orderId,
        long amountCents
    );
}
