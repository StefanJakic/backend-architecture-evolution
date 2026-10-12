package dev.stefanjakic.aggregate.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectTest {

    @Test
    void addsAllocationWhenProjectHasEnoughBudget() {
        Project project = new Project(
            "project-1",
            10_000
        );

        project.addAllocation(
            "allocation-1",
            4_000
        );

        assertEquals(
            4_000,
            project.allocatedBudgetCents()
        );

        assertEquals(
            6_000,
            project.remainingBudgetCents()
        );
    }

    @Test
    void rejectsAllocationThatWouldExceedProjectBudget() {
        Project project = new Project(
            "project-1",
            10_000
        );

        project.addAllocation(
            "allocation-1",
            7_000
        );

        assertThrows(
            IllegalStateException.class,
            () -> project.addAllocation(
                "allocation-2",
                4_000
            )
        );

        assertEquals(
            7_000,
            project.allocatedBudgetCents()
        );

        assertEquals(
            3_000,
            project.remainingBudgetCents()
        );
    }

    @Test
    void rejectsDuplicateAllocationId() {
        Project project = new Project(
            "project-1",
            10_000
        );

        project.addAllocation(
            "allocation-1",
            2_000
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> project.addAllocation(
                "allocation-1",
                1_000
            )
        );

        assertEquals(
            1,
            project.allocations().size()
        );
    }

    @Test
    void exposedAllocationsCannotMutateAggregate() {
        Project project = new Project(
            "project-1",
            10_000
        );

        project.addAllocation(
            "allocation-1",
            2_000
        );

        var snapshot = project.allocations();

        assertThrows(
            UnsupportedOperationException.class,
            () -> snapshot.add(
                new BudgetAllocation(
                    "allocation-2",
                    9_000
                )
            )
        );

        assertEquals(
            2_000,
            project.allocatedBudgetCents()
        );
    }
}
