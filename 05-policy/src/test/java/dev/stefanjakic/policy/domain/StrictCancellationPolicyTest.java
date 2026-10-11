package dev.stefanjakic.policy.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrictCancellationPolicyTest {

    private final CancellationPolicy policy =
        new StrictCancellationPolicy();

    @Test
    void refundsFullyFromFourteenDays() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 336)
        );

        assertEquals(10_000, decision.refundCents());
    }

    @Test
    void refundsHalfFromSevenDays() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 168)
        );

        assertEquals(5_000, decision.refundCents());
    }

    @Test
    void refundsNothingInsideSevenDays() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 167)
        );

        assertEquals(0, decision.refundCents());
    }
}
