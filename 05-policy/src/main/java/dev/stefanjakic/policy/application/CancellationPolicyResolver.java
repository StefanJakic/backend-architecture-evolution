package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationTerms;
import dev.stefanjakic.policy.domain.FlexibleCancellationPolicy;
import dev.stefanjakic.policy.domain.StrictCancellationPolicy;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class CancellationPolicyResolver {

    private final Map<CancellationTerms, CancellationPolicy> policies;

    public CancellationPolicyResolver(
        Map<CancellationTerms, CancellationPolicy> policies
    ) {
        Objects.requireNonNull(
            policies,
            "policies must not be null"
        );

        this.policies = new EnumMap<>(CancellationTerms.class);
        this.policies.putAll(policies);
    }

    public static CancellationPolicyResolver standard() {
        return new CancellationPolicyResolver(
            Map.of(
                CancellationTerms.FLEXIBLE,
                new FlexibleCancellationPolicy(),
                CancellationTerms.STRICT,
                new StrictCancellationPolicy()
            )
        );
    }

    public CancellationPolicy resolve(
        CancellationTerms terms
    ) {
        Objects.requireNonNull(
            terms,
            "terms must not be null"
        );

        CancellationPolicy policy = policies.get(terms);

        if (policy == null) {
            throw new IllegalArgumentException(
                "No cancellation policy registered for terms: "
                    + terms
            );
        }

        return policy;
    }
}
