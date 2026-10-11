package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.CancellationTerms;
import dev.stefanjakic.policy.domain.RefundDecision;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationServiceTest {

    @Test
    void resolvesTermsAndDelegatesToSelectedPolicy() {
        CancellationPolicy flexible =
            request -> new RefundDecision(1_234);

        var resolver = new CancellationPolicyResolver(
            Map.of(
                CancellationTerms.FLEXIBLE,
                flexible
            )
        );

        var service = new CancellationService(resolver);

        var decision = service.refundFor(
            new CancellationRequest(10_000, 12),
            CancellationTerms.FLEXIBLE
        );

        assertEquals(1_234, decision.refundCents());
    }

    @Test
    void standardResolverRunsConcretePolicies() {
        var service = new CancellationService(
            CancellationPolicyResolver.standard()
        );

        var flexibleDecision = service.refundFor(
            new CancellationRequest(10_000, 24),
            CancellationTerms.FLEXIBLE
        );

        var strictDecision = service.refundFor(
            new CancellationRequest(10_000, 168),
            CancellationTerms.STRICT
        );

        assertEquals(
            7_500,
            flexibleDecision.refundCents()
        );

        assertEquals(
            5_000,
            strictDecision.refundCents()
        );
    }
}
