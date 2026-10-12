package dev.stefanjakic.aggregate.domain;

import java.util.ArrayList;
import java.util.List;

public final class Project {

    private final String id;
    private final long totalBudgetCents;
    private final List<BudgetAllocation> allocations =
        new ArrayList<>();

    public Project(
        String id,
        long totalBudgetCents
    ) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(
                "id must not be blank"
            );
        }

        if (totalBudgetCents <= 0) {
            throw new IllegalArgumentException(
                "totalBudgetCents must be positive"
            );
        }

        this.id = id;
        this.totalBudgetCents = totalBudgetCents;
    }

    public void addAllocation(
        String allocationId,
        long amountCents
    ) {
        BudgetAllocation allocation =
            new BudgetAllocation(
                allocationId,
                amountCents
            );

        if (containsAllocation(allocation.id())) {
            throw new IllegalArgumentException(
                "Allocation already exists: "
                    + allocation.id()
            );
        }

        if (amountCents > remainingBudgetCents()) {
            throw new IllegalStateException(
                "Allocation exceeds remaining project budget"
            );
        }

        allocations.add(allocation);
    }

    public long allocatedBudgetCents() {
        return allocations.stream()
            .mapToLong(BudgetAllocation::amountCents)
            .sum();
    }

    public long remainingBudgetCents() {
        return totalBudgetCents - allocatedBudgetCents();
    }

    public List<BudgetAllocation> allocations() {
        return List.copyOf(allocations);
    }

    public String id() {
        return id;
    }

    public long totalBudgetCents() {
        return totalBudgetCents;
    }

    private boolean containsAllocation(String allocationId) {
        return allocations.stream()
            .anyMatch(
                allocation ->
                    allocation.id().equals(allocationId)
            );
    }
}
