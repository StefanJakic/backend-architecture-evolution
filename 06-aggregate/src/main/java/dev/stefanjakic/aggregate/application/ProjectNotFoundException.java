package dev.stefanjakic.aggregate.application;

public class ProjectNotFoundException extends RuntimeException {

    public ProjectNotFoundException(String projectId) {
        super("Project not found: " + projectId);
    }
}
