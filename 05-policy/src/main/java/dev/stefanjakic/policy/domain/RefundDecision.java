package dev.stefanjakic.policy.domain;

public record RefundDecision(
    long refundCents
) {

    public RefundDecision {
        if (refundCents < 0) {
            throw new IllegalArgumentException(
                "refundCents must not be negative"
            );
        }
    }
}
