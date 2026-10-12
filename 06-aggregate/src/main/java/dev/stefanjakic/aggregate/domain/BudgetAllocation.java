package dev.stefanjakic.aggregate.domain;

public record BudgetAllocation(
    String id,
    long amountCents
) {

    public BudgetAllocation {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                "id must not be blank"
            );
        }

        if (amountCents <= 0) {
            throw new IllegalArgumentException(
                "amountCents must be positive"
            );
        }
    }
}
