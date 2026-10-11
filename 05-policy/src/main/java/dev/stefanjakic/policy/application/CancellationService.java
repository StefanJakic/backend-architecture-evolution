package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.RefundDecision;

import java.util.Objects;

public final class CancellationService {

    public RefundDecision refundFor(
        CancellationRequest request,
        CancellationPolicy policy
    ) {
        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        Objects.requireNonNull(
            policy,
            "policy must not be null"
        );

        return policy.calculate(request);
    }
}
