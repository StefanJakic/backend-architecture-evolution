package dev.stefanjakic.aggregate.application;

public class OptimisticAggregateConflictException
    extends RuntimeException {

    public OptimisticAggregateConflictException(
        String projectId,
        long expectedVersion
    ) {
        super(
            "Project was modified concurrently: "
                + projectId
                + " expected version "
                + expectedVersion
        );
    }
}
