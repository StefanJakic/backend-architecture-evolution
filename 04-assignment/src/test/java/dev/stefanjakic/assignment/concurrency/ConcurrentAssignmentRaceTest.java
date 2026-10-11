package dev.stefanjakic.assignment.concurrency;

import dev.stefanjakic.assignment.application.Assignment;
import dev.stefanjakic.assignment.application.AssignmentRepository;
import dev.stefanjakic.assignment.application.AssignmentService;
import dev.stefanjakic.assignment.domain.Job;
import dev.stefanjakic.assignment.domain.Worker;
import dev.stefanjakic.assignment.infrastructure.persistence.PostgresAssignmentRepository;
import dev.stefanjakic.assignment.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ConcurrentAssignmentRaceTest.RaceTestConfiguration.class)
class ConcurrentAssignmentRaceTest extends PostgresIntegrationTest {

    @Autowired
    private AssignmentService service;

    @Autowired
    private CoordinatedAssignmentRepository coordinatedRepository;

    @Autowired
    private PostgresAssignmentRepository postgresRepository;

    @Test
    void lockedSelectionGivesConcurrentWorkersDifferentJobs()
        throws Exception {

        String suffix = UUID.randomUUID().toString();

        Job firstJob = new Job(
            "job-1-" + suffix,
            "JAVA",
            100
        );

        Job secondJob = new Job(
            "job-2-" + suffix,
            "JAVA",
            90
        );

        postgresRepository.insert(firstJob);
        postgresRepository.insert(secondJob);

        Worker firstWorker = new Worker(
            "worker-a-" + suffix,
            "JAVA"
        );

        Worker secondWorker = new Worker(
            "worker-b-" + suffix,
            "JAVA"
        );

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            var firstAttempt = pool.submit(
                () -> service.claimNext(firstWorker)
            );

            var secondAttempt = pool.submit(
                () -> service.claimNext(secondWorker)
            );

            assertTrue(
                coordinatedRepository.awaitBothLockedSelections(
                    Duration.ofSeconds(5)
                ),
                "Both schedulers should select and lock different jobs"
            );

            coordinatedRepository.releaseSelections();

            Assignment first =
                firstAttempt.get(5, TimeUnit.SECONDS);

            Assignment second =
                secondAttempt.get(5, TimeUnit.SECONDS);

            assertNotEquals(first.jobId(), second.jobId());

            assertEquals(
                Set.of(firstJob.id(), secondJob.id()),
                Set.of(first.jobId(), second.jobId())
            );

            Map<String, String> expectedClaimants = Map.of(
                first.jobId(), first.workerId(),
                second.jobId(), second.workerId()
            );

            assertEquals(
                expectedClaimants.get(firstJob.id()),
                postgresRepository.findClaimedBy(firstJob.id())
                    .orElseThrow()
            );

            assertEquals(
                expectedClaimants.get(secondJob.id()),
                postgresRepository.findClaimedBy(secondJob.id())
                    .orElseThrow()
            );
        } finally {
            coordinatedRepository.releaseSelections();
            pool.shutdownNow();
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RaceTestConfiguration {

        @Bean
        @Primary
        CoordinatedAssignmentRepository coordinatedAssignmentRepository(
            PostgresAssignmentRepository delegate
        ) {
            return new CoordinatedAssignmentRepository(delegate);
        }
    }

    static final class CoordinatedAssignmentRepository
        implements AssignmentRepository {

        private final AssignmentRepository delegate;

        private final CountDownLatch bothLockedSelections =
            new CountDownLatch(2);

        private final CountDownLatch releaseSelections =
            new CountDownLatch(1);

        CoordinatedAssignmentRepository(
            AssignmentRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public Optional<Job> findNextPendingForUpdate(String capability) {
            Optional<Job> job =
                delegate.findNextPendingForUpdate(capability);

            bothLockedSelections.countDown();

            try {
                if (!releaseSelections.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release locked assignment selections"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating assignment locks",
                    exception
                );
            }

            return job;
        }

        @Override
        public void insert(Job job) {
            delegate.insert(job);
        }

        @Override
        public void claim(
            String jobId,
            String workerId
        ) {
            delegate.claim(jobId, workerId);
        }

        @Override
        public Optional<String> findClaimedBy(String jobId) {
            return delegate.findClaimedBy(jobId);
        }

        boolean awaitBothLockedSelections(Duration timeout)
            throws InterruptedException {

            return bothLockedSelections.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        void releaseSelections() {
            releaseSelections.countDown();
        }
    }
}
