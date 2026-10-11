package dev.stefanjakic.policy.domain;

public interface CancellationPolicy {

    RefundDecision calculate(CancellationRequest request);
}
