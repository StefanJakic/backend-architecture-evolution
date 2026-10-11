package dev.stefanjakic.assignment.application;

import dev.stefanjakic.assignment.domain.Job;
import dev.stefanjakic.assignment.domain.Worker;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignmentService {

    private final AssignmentRepository repository;

    public AssignmentService(AssignmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Assignment claimNext(Worker worker) {
        Job selected = repository.findNextPendingForUpdate(
            worker.capability()
        ).orElseThrow(
            () -> new NoAssignableJobException(worker.capability())
        );

        repository.claim(
            selected.id(),
            worker.id()
        );

        return new Assignment(
            selected.id(),
            worker.id()
        );
    }
}
