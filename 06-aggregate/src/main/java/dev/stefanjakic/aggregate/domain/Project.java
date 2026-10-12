package dev.stefanjakic.aggregate.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Project {

    private final String id;
    private final long totalBudgetCents;
    private final long version;
    private final List<BudgetAllocation> allocations =
        new ArrayList<>();

    public Project(
        String id,
        long totalBudgetCents
    ) {
        this(
            id,
            totalBudgetCents,
            0
        );
    }

    private Project(
        String id,
        long totalBudgetCents,
        long version
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

        if (version < 0) {
            throw new IllegalArgumentException(
                "version must not be negative"
            );
        }

        this.id = id;
        this.totalBudgetCents = totalBudgetCents;
        this.version = version;
    }

    public static Project restore(
        String id,
        long totalBudgetCents,
        long version,
        List<BudgetAllocation> allocations
    ) {
        Objects.requireNonNull(
            allocations,
            "allocations must not be null"
        );

        Project project = new Project(
            id,
            totalBudgetCents,
            version
        );

        for (BudgetAllocation allocation : allocations) {
            Objects.requireNonNull(
                allocation,
                "allocation must not be null"
            );

            project.addAllocation(
                allocation.id(),
                allocation.amountCents()
            );
        }

        return project;
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

    public long version() {
        return version;
    }

    private boolean containsAllocation(String allocationId) {
        return allocations.stream()
            .anyMatch(
                allocation ->
                    allocation.id().equals(allocationId)
            );
    }
}
