package dev.stefanjakic.aggregate.persistence;

import dev.stefanjakic.aggregate.application.ProjectBudgetService;
import dev.stefanjakic.aggregate.application.ProjectRepository;
import dev.stefanjakic.aggregate.domain.Project;
import dev.stefanjakic.aggregate.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProjectPersistenceIntegrationTest
    extends PostgresIntegrationTest {

    @Autowired
    private ProjectRepository repository;

    @Autowired
    private ProjectBudgetService service;

    @Test
    void roundTripsRootAndChildrenAsOneAggregate() {
        String projectId =
            "project-" + UUID.randomUUID();

        Project project = new Project(
            projectId,
            10_000
        );

        project.addAllocation(
            "allocation-1",
            2_000
        );

        project.addAllocation(
            "allocation-2",
            3_000
        );

        repository.insert(project);

        Project restored = repository.findById(projectId)
            .orElseThrow();

        assertEquals(
            project.id(),
            restored.id()
        );

        assertEquals(
            project.totalBudgetCents(),
            restored.totalBudgetCents()
        );

        assertEquals(
            5_000,
            restored.allocatedBudgetCents()
        );

        assertEquals(
            5_000,
            restored.remainingBudgetCents()
        );

        assertEquals(
            2,
            restored.allocations().size()
        );
    }

    @Test
    void serviceLoadsMutatesAndSavesThroughAggregateRoot() {
        String projectId =
            "project-" + UUID.randomUUID();

        Project project = new Project(
            projectId,
            10_000
        );

        project.addAllocation(
            "allocation-1",
            2_000
        );

        repository.insert(project);

        service.allocate(
            projectId,
            "allocation-2",
            3_000
        );

        Project restored = repository.findById(projectId)
            .orElseThrow();

        assertEquals(
            5_000,
            restored.allocatedBudgetCents()
        );

        assertEquals(
            5_000,
            restored.remainingBudgetCents()
        );

        assertEquals(
            2,
            restored.allocations().size()
        );
    }

    @Test
    void rejectedMutationDoesNotPersistPartialChildState() {
        String projectId =
            "project-" + UUID.randomUUID();

        Project project = new Project(
            projectId,
            10_000
        );

        project.addAllocation(
            "allocation-1",
            8_000
        );

        repository.insert(project);

        assertThrows(
            IllegalStateException.class,
            () -> service.allocate(
                projectId,
                "allocation-2",
                3_000
            )
        );

        Project restored = repository.findById(projectId)
            .orElseThrow();

        assertEquals(
            8_000,
            restored.allocatedBudgetCents()
        );

        assertEquals(
            2_000,
            restored.remainingBudgetCents()
        );

        assertEquals(
            1,
            restored.allocations().size()
        );
    }
}
