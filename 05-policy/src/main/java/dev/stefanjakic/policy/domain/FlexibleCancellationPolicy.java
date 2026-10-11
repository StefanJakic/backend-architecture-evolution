package dev.stefanjakic.policy.domain;

import java.util.Objects;

public final class FlexibleCancellationPolicy
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

        if (request.hoursBeforeStart() >= 72) {
            refundCents = request.priceCents();
        } else if (request.hoursBeforeStart() >= 24) {
            refundCents =
                Math.multiplyExact(request.priceCents(), 3) / 4;
        } else {
            refundCents = request.priceCents() / 2;
        }

        return new RefundDecision(refundCents);
    }
}
