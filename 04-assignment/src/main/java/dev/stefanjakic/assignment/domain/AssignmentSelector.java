package dev.stefanjakic.assignment.domain;

import java.util.Collection;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

public final class AssignmentSelector {

    private static final Comparator<Job> ORDER =
        Comparator.comparingInt(Job::priority)
            .reversed()
            .thenComparing(Job::id);

    public Optional<Job> selectNext(
        Worker worker,
        Collection<Job> jobs
    ) {
        Objects.requireNonNull(worker, "worker must not be null");
        Objects.requireNonNull(jobs, "jobs must not be null");

        return jobs.stream()
            .filter(Objects::nonNull)
            .filter(job -> isEligible(worker, job))
            .sorted(ORDER)
            .findFirst();
    }

    private boolean isEligible(
        Worker worker,
        Job job
    ) {
        return worker.capability().equals(job.requiredCapability());
    }
}
