package dev.stefanjakic.policy.domain;

import java.util.Objects;

public final class StrictCancellationPolicy
    implements CancellationPolicy {

    @Override
    public RefundDecision calculate(
        CancellationRequest request
    ) {
        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        long refundCents;

        if (request.hoursBeforeStart() >= 336) {
            refundCents = request.priceCents();
        } else if (request.hoursBeforeStart() >= 168) {
            refundCents = request.priceCents() / 2;
        } else {
            refundCents = 0;
        }

        return new RefundDecision(refundCents);
    }
}
