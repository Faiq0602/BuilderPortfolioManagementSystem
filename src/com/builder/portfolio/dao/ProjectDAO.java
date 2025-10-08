package com.builder.portfolio.dao;

import com.builder.portfolio.model.Project;
import java.util.List;

public interface ProjectDAO {
    void addProject(Project project);

    void updateProject(Project project);

    void deleteProject(int projectId, int builderId);

    List<Project> findProjectsByBuilder(int builderId);

    List<Project> findProjectsByClient(int clientId);

    List<Project> findAllProjects();

    Project findById(int projectId);
}
