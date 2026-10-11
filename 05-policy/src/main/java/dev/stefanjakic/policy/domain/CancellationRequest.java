package dev.stefanjakic.policy.domain;

public record CancellationRequest(
    long priceCents,
    long hoursBeforeStart
) {

    public CancellationRequest {
        if (priceCents < 0) {
            throw new IllegalArgumentException(
                "priceCents must not be negative"
            );
        }

        if (hoursBeforeStart < 0) {
            throw new IllegalArgumentException(
                "hoursBeforeStart must not be negative"
            );
        }
    }
}
