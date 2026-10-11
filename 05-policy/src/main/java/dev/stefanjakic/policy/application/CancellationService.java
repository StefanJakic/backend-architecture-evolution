package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.CancellationTerms;
import dev.stefanjakic.policy.domain.RefundDecision;

import java.util.Objects;

public final class CancellationService {

    public RefundDecision refundFor(
        CancellationRequest request,
        CancellationTerms terms
    ) {
        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        Objects.requireNonNull(
            terms,
            "terms must not be null"
        );

        long refundCents = switch (terms) {
            case FLEXIBLE -> flexibleRefund(request);
            case STRICT -> strictRefund(request);
        };

        return new RefundDecision(refundCents);
    }

    private long flexibleRefund(
        CancellationRequest request
    ) {
        if (request.hoursBeforeStart() >= 24) {
            return request.priceCents();
        }

        return request.priceCents() / 2;
    }

    private long strictRefund(
        CancellationRequest request
    ) {
        if (request.hoursBeforeStart() >= 168) {
            return request.priceCents();
        }

        return 0;
    }
}
