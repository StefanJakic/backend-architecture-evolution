package dev.stefanjakic.policy.application;

import dev.stefanjakic.policy.domain.CancellationRequest;
import dev.stefanjakic.policy.domain.CancellationTerms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CancellationServiceTest {

    private final CancellationService service =
        new CancellationService();

    @Test
    void flexibleTermsRefundFullyAtTwentyFourHours() {
        var request = new CancellationRequest(
            10_000,
            24
        );

        var decision = service.refundFor(
            request,
            CancellationTerms.FLEXIBLE
        );

        assertEquals(10_000, decision.refundCents());
    }

    @Test
    void flexibleTermsRefundHalfInsideTwentyFourHours() {
        var request = new CancellationRequest(
            10_000,
            23
        );

        var decision = service.refundFor(
            request,
            CancellationTerms.FLEXIBLE
        );

        assertEquals(5_000, decision.refundCents());
    }

    @Test
    void strictTermsRefundFullyAtSevenDays() {
        var request = new CancellationRequest(
            10_000,
            168
        );

        var decision = service.refundFor(
            request,
            CancellationTerms.STRICT
        );

        assertEquals(10_000, decision.refundCents());
    }

    @Test
    void strictTermsRefundNothingInsideSevenDays() {
        var request = new CancellationRequest(
            10_000,
            167
        );

        var decision = service.refundFor(
            request,
            CancellationTerms.STRICT
        );

        assertEquals(0, decision.refundCents());
    }
}
