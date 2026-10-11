package dev.stefanjakic.assignment.application;

public record Assignment(
    String jobId,
    String workerId
) {

    public Assignment {
        if (jobId == null || jobId.isBlank()) {
            throw new IllegalArgumentException(
                "jobId must not be blank"
            );
        }

        if (workerId == null || workerId.isBlank()) {
            throw new IllegalArgumentException(
                "workerId must not be blank"
            );
        }
    }
}
