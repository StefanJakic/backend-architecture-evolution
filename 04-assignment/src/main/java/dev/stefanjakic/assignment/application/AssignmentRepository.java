package dev.stefanjakic.assignment.application;

import dev.stefanjakic.assignment.domain.Job;

import java.util.Optional;

public interface AssignmentRepository {

    Optional<Job> findNextPending(String capability);

    void insert(Job job);

    void claim(
        String jobId,
        String workerId
    );

    Optional<String> findClaimedBy(String jobId);
}
