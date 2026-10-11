package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationPolicy;
import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.RefundDecision;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationServiceTest {

    private final CancellationService service =
        new CancellationService();

    @Test
    void delegatesRefundCalculationToProvidedPolicy() {
        CancellationPolicy policy =
            request -> new RefundDecision(1_234);

        var decision = service.refundFor(
            new CancellationRequest(10_000, 12),
            policy
        );

        assertEquals(1_234, decision.refundCents());
    }
}
