package com.builder.portfolio.service;

import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Document;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.model.ProjectSummary;
import java.util.List;

public interface ProjectService {
    void addProject(Project project);

    void updateProject(Project project);

    void deleteProject(int projectId, int builderId);

    List<Project> listProjectsByBuilder(int builderId);

    List<Project> listProjectsByClient(int clientId);

    List<Project> listAllProjects();

    Project getProject(int projectId);

    BudgetReport buildBudgetReport(Project project);

    Project updateProjectStatus(long projectId, String newStatus, long expectedVersion);

    Project updateProjectBudget(long projectId, double delta, long expectedVersion);

    void uploadDocument(long projectId, Document document);

    ProjectSummary getProjectSummary(long projectId);
}
