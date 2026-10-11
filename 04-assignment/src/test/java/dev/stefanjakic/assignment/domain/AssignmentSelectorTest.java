package dev.stefanjakic.assignment.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssignmentSelectorTest {

    private final AssignmentSelector selector = new AssignmentSelector();

    @Test
    void selectsHighestPriorityEligibleJob() {
        Worker worker = new Worker("worker-1", "JAVA");

        Job low = new Job("job-2", "JAVA", 10);
        Job high = new Job("job-1", "JAVA", 100);

        Job selected = selector.selectNext(
            worker,
            List.of(low, high)
        ).orElseThrow();

        assertEquals(high, selected);
    }

    @Test
    void ignoresHigherPriorityIneligibleJob() {
        Worker worker = new Worker("worker-1", "JAVA");

        Job eligible = new Job("job-2", "JAVA", 10);
        Job ineligible = new Job("job-1", "PYTHON", 100);

        Job selected = selector.selectNext(
            worker,
            List.of(ineligible, eligible)
        ).orElseThrow();

        assertEquals(eligible, selected);
    }

    @Test
    void usesJobIdAsStableTieBreaker() {
        Worker worker = new Worker("worker-1", "JAVA");

        Job second = new Job("job-2", "JAVA", 50);
        Job first = new Job("job-1", "JAVA", 50);

        Job selected = selector.selectNext(
            worker,
            List.of(second, first)
        ).orElseThrow();

        assertEquals(first, selected);
    }

    @Test
    void returnsEmptyWhenNoJobIsEligible() {
        Worker worker = new Worker("worker-1", "JAVA");

        var selected = selector.selectNext(
            worker,
            List.of(
                new Job("job-1", "PYTHON", 100),
                new Job("job-2", "GO", 50)
            )
        );

        assertTrue(selected.isEmpty());
    }
}
