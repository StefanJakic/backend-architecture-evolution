package dev.stefanjakic.policy.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlexibleCancellationPolicyTest {

    private final CancellationPolicy policy =
        new FlexibleCancellationPolicy();

    @Test
    void refundsFullyFromSeventyTwoHours() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 72)
        );

        assertEquals(10_000, decision.refundCents());
    }

    @Test
    void refundsSeventyFivePercentFromTwentyFourHours() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 24)
        );

        assertEquals(7_500, decision.refundCents());
    }

    @Test
    void refundsHalfInsideTwentyFourHours() {
        var decision = policy.calculate(
            new CancellationRequest(10_000, 23)
        );

        assertEquals(5_000, decision.refundCents());
    }
}
