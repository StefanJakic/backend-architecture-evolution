package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.CancellationTerms;
import dev.stefanjakic.policy.domain.RefundDecision;

import java.util.Objects;

public final class CancellationService {

    private final CancellationPolicyResolver policyResolver;

    public CancellationService(
        CancellationPolicyResolver policyResolver
    ) {
        this.policyResolver = Objects.requireNonNull(
            policyResolver,
            "policyResolver must not be null"
        );
    }

    public RefundDecision refundFor(
        CancellationRequest request,
        CancellationTerms terms
    ) {
        Objects.requireNonNull(
            request,
            "request must not be null"
        );

        CancellationPolicy policy =
            policyResolver.resolve(terms);

        return policy.calculate(request);
    }
}
