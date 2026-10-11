package dev.stefanjakic.assignment.concurrency;

import dev.stefanjakic.assignment.application.Assignment;
import dev.stefanjakic.assignment.application.AssignmentRepository;
import dev.stefanjakic.assignment.application.AssignmentService;
import dev.stefanjakic.assignment.application.NoAssignableJobException;
import dev.stefanjakic.assignment.domain.Job;
import dev.stefanjakic.assignment.domain.Worker;
import dev.stefanjakic.assignment.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class MultiWorkerAssignmentTest extends PostgresIntegrationTest {

    private static final int JOB_COUNT = 10;
    private static final int WORKER_COUNT = 20;

    @Autowired
    private AssignmentService service;

    @Autowired
    private AssignmentRepository repository;

    @Test
    void manyWorkersClaimEachPendingJobAtMostOnce()
        throws Exception {

        String suffix = UUID.randomUUID().toString();

        Set<String> expectedJobIds = new HashSet<>();

        for (int i = 0; i < JOB_COUNT; i++) {
            String jobId = "job-" + i + "-" + suffix;

            expectedJobIds.add(jobId);

            repository.insert(
                new Job(
                    jobId,
                    "JAVA",
                    100 - i
                )
            );
        }

        CountDownLatch ready = new CountDownLatch(WORKER_COUNT);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService pool =
            Executors.newFixedThreadPool(WORKER_COUNT);

        try {
            List<Future<AttemptResult>> futures =
                new ArrayList<>();

            for (int i = 0; i < WORKER_COUNT; i++) {
                Worker worker = new Worker(
                    "worker-" + i + "-" + suffix,
                    "JAVA"
                );

                futures.add(
                    pool.submit(
                        () -> claim(worker, ready, start)
                    )
                );
            }

            assertTrue(
                ready.await(5, TimeUnit.SECONDS),
                "All workers should be ready before the common start"
            );

            start.countDown();

            List<AttemptResult> results = new ArrayList<>();

            for (Future<AttemptResult> future : futures) {
                results.add(
                    future.get(15, TimeUnit.SECONDS)
                );
            }

            List<Assignment> successfulAssignments =
                results.stream()
                    .filter(AttemptResult::success)
                    .map(AttemptResult::assignment)
                    .toList();

            long emptyQueueResults = results.stream()
                .filter(result -> !result.success())
                .count();

            Set<String> claimedJobIds =
                successfulAssignments.stream()
                    .map(Assignment::jobId)
                    .collect(java.util.stream.Collectors.toSet());

            assertEquals(
                JOB_COUNT,
                successfulAssignments.size()
            );

            assertEquals(
                JOB_COUNT,
                claimedJobIds.size(),
                "Every successful worker must own a different job"
            );

            assertEquals(expectedJobIds, claimedJobIds);

            assertEquals(
                WORKER_COUNT - JOB_COUNT,
                emptyQueueResults
            );

            for (Assignment assignment : successfulAssignments) {
                assertEquals(
                    assignment.workerId(),
                    repository.findClaimedBy(assignment.jobId())
                        .orElseThrow()
                );
            }
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private AttemptResult claim(
        Worker worker,
        CountDownLatch ready,
        CountDownLatch start
    ) {
        ready.countDown();

        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                    "Timed out waiting for common start"
                );
            }

            return AttemptResult.success(
                service.claimNext(worker)
            );
        } catch (NoAssignableJobException exception) {
            return AttemptResult.noJob();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(
                "Interrupted while waiting for common start",
                exception
            );
        }
    }

    private record AttemptResult(
        Assignment assignment
    ) {

        static AttemptResult success(Assignment assignment) {
            return new AttemptResult(assignment);
        }

        static AttemptResult noJob() {
            return new AttemptResult(null);
        }

        boolean success() {
            return assignment != null;
        }
    }
}
