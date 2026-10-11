package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationTerms;
import dev.stefanjakic.policy.domain.RefundDecision;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CancellationPolicyResolverTest {

    @Test
    void resolvesRegisteredPolicyByBusinessTerms() {
        CancellationPolicy flexible =
            request -> new RefundDecision(111);

        CancellationPolicy strict =
            request -> new RefundDecision(222);

        var resolver = new CancellationPolicyResolver(
            Map.of(
                CancellationTerms.FLEXIBLE,
                flexible,
                CancellationTerms.STRICT,
                strict
            )
        );

        assertSame(
            flexible,
            resolver.resolve(CancellationTerms.FLEXIBLE)
        );

        assertSame(
            strict,
            resolver.resolve(CancellationTerms.STRICT)
        );
    }

    @Test
    void failsClearlyWhenPolicyIsNotRegistered() {
        var resolver = new CancellationPolicyResolver(
            Map.of()
        );

        var exception = assertThrows(
            IllegalArgumentException.class,
            () -> resolver.resolve(CancellationTerms.FLEXIBLE)
        );

        assertEquals(
            "No cancellation policy registered for terms: FLEXIBLE",
            exception.getMessage()
        );
    }
}
