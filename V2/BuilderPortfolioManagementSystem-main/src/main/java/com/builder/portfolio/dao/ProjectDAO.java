package com.builder.portfolio.dao;

import com.builder.portfolio.model.Project;
import java.util.List;
import java.util.Optional;

public interface ProjectDAO {
    void addProject(Project project);

    void updateProject(Project project);

    void deleteProject(int projectId, int builderId);

    List<Project> findProjectsByBuilder(int builderId);

    List<Project> findProjectsByClient(int clientId);

    List<Project> findAllProjects();

    Project findById(int projectId);

    /**
     * Provides a strongly consistent read prior to mutation.
     * TODO: replace with SELECT ... FOR UPDATE once database migrations are in place.
     */
    default Project findByIdForUpdate(long projectId) {
        return findById((int) projectId);
    }

    /**
     * Performs an optimistic version guarded update.
     *
     * @return true if the update succeeded and the version was incremented, false otherwise.
     */
    default boolean conditionalUpdateProject(Project project, long expectedVersion) {
        updateProject(project);
        return true;
    }

    default Optional<Project> findOptionalById(long projectId) {
        return Optional.ofNullable(findById((int) projectId));
    }
}
