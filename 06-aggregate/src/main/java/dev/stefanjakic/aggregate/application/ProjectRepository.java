package dev.stefanjakic.aggregate.application;

import dev.stefanjakic.aggregate.domain.Project;

import java.util.Optional;

public interface ProjectRepository {

    void insert(Project project);

    Optional<Project> findById(String projectId);

    void save(Project project);
}
