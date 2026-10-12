package dev.stefanjakic.aggregate.concurrency;

import dev.stefanjakic.aggregate.application.OptimisticAggregateConflictException;
import dev.stefanjakic.aggregate.application.ProjectBudgetService;
import dev.stefanjakic.aggregate.application.ProjectRepository;
import dev.stefanjakic.aggregate.domain.Project;
import dev.stefanjakic.aggregate.infrastructure.persistence.PostgresProjectRepository;
import dev.stefanjakic.aggregate.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ConcurrentAggregateLostUpdateTest.RaceTestConfiguration.class)
class ConcurrentAggregateLostUpdateTest
    extends PostgresIntegrationTest {

    @Autowired
    private ProjectBudgetService service;

    @Autowired
    private CoordinatedProjectRepository coordinatedRepository;

    @Autowired
    private PostgresProjectRepository postgresRepository;

    @Test
    void optimisticVersionRejectsOneStaleAggregateSnapshot()
        throws Exception {

        String suffix = UUID.randomUUID().toString();
        String projectId = "project-" + suffix;
        String initialId = "allocation-initial-" + suffix;
        String firstId = "allocation-a-" + suffix;
        String secondId = "allocation-b-" + suffix;

        Project project = new Project(
            projectId,
            10_000
        );

        project.addAllocation(
            initialId,
            2_000
        );

        postgresRepository.insert(project);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            var firstAttempt = pool.submit(
                () -> allocate(
                    projectId,
                    firstId,
                    3_000
                )
            );

            var secondAttempt = pool.submit(
                () -> allocate(
                    projectId,
                    secondId,
                    4_000
                )
            );

            assertTrue(
                coordinatedRepository.awaitBothLoads(
                    Duration.ofSeconds(5)
                ),
                "Both transactions should load the same aggregate version before either save"
            );

            coordinatedRepository.releaseLoads();

            AttemptResult first =
                firstAttempt.get(10, TimeUnit.SECONDS);

            AttemptResult second =
                secondAttempt.get(10, TimeUnit.SECONDS);

            List<AttemptResult> results =
                List.of(first, second);

            assertEquals(
                1,
                results.stream()
                    .filter(AttemptResult::success)
                    .count()
            );

            assertEquals(
                1,
                results.stream()
                    .filter(result -> !result.success())
                    .count()
            );

            String winnerId = results.stream()
                .filter(AttemptResult::success)
                .map(AttemptResult::allocationId)
                .findFirst()
                .orElseThrow();

            String loserId = results.stream()
                .filter(result -> !result.success())
                .map(AttemptResult::allocationId)
                .findFirst()
                .orElseThrow();

            Project finalProject = postgresRepository
                .findById(projectId)
                .orElseThrow();

            Set<String> finalIds = finalProject.allocations()
                .stream()
                .map(allocation -> allocation.id())
                .collect(java.util.stream.Collectors.toSet());

            assertEquals(
                Set.of(initialId, winnerId),
                finalIds
            );

            assertTrue(
                !finalIds.contains(loserId),
                "The stale transaction must not persist any child snapshot"
            );

            assertEquals(
                1,
                finalProject.version()
            );
        } finally {
            coordinatedRepository.releaseLoads();
            pool.shutdownNow();
        }
    }

    private AttemptResult allocate(
        String projectId,
        String allocationId,
        long amountCents
    ) {
        try {
            service.allocate(
                projectId,
                allocationId,
                amountCents
            );

            return AttemptResult.success(allocationId);
        } catch (OptimisticAggregateConflictException exception) {
            return AttemptResult.conflict(allocationId);
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RaceTestConfiguration {

        @Bean
        @Primary
        CoordinatedProjectRepository coordinatedProjectRepository(
            PostgresProjectRepository delegate
        ) {
            return new CoordinatedProjectRepository(delegate);
        }
    }

    static final class CoordinatedProjectRepository
        implements ProjectRepository {

        private final ProjectRepository delegate;

        private final CountDownLatch bothLoadsCompleted =
            new CountDownLatch(2);

        private final CountDownLatch releaseLoads =
            new CountDownLatch(1);

        CoordinatedProjectRepository(
            ProjectRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public void insert(Project project) {
            delegate.insert(project);
        }

        @Override
        public Optional<Project> findById(String projectId) {
            Optional<Project> project =
                delegate.findById(projectId);

            bothLoadsCompleted.countDown();

            try {
                if (!releaseLoads.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release aggregate loads"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating aggregate race",
                    exception
                );
            }

            return project;
        }

        @Override
        public void save(Project project) {
            delegate.save(project);
        }

        boolean awaitBothLoads(Duration timeout)
            throws InterruptedException {

            return bothLoadsCompleted.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        void releaseLoads() {
            releaseLoads.countDown();
        }
    }

    private record AttemptResult(
        String allocationId,
        boolean success
    ) {

        static AttemptResult success(String allocationId) {
            return new AttemptResult(
                allocationId,
                true
            );
        }

        static AttemptResult conflict(String allocationId) {
            return new AttemptResult(
                allocationId,
                false
            );
        }
    }
}
