package dev.stefanjakic.aggregate.application;

import dev.stefanjakic.aggregate.domain.Project;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectBudgetService {

    private final ProjectRepository repository;

    public ProjectBudgetService(ProjectRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void allocate(
        String projectId,
        String allocationId,
        long amountCents
    ) {
        Project project = repository.findById(projectId)
            .orElseThrow(
                () -> new ProjectNotFoundException(projectId)
            );

        project.addAllocation(
            allocationId,
            amountCents
        );

        repository.save(project);
    }
}
