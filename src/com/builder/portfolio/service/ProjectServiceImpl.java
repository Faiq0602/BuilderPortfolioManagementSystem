package com.builder.portfolio.service;

import com.builder.portfolio.dao.ProjectDAO;
import com.builder.portfolio.dao.ProjectDAOImpl;
import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.util.BudgetUtil;
import com.builder.portfolio.util.StatusConstants;
import java.util.List;

public class ProjectServiceImpl implements ProjectService {
    private final ProjectDAO projectDAO;

    public ProjectServiceImpl() {
        this.projectDAO = new ProjectDAOImpl();
    }

    public ProjectServiceImpl(ProjectDAO projectDAO) {
        this.projectDAO = projectDAO;
    }

    @Override
    public void addProject(Project project) {
        if (project.getStatus() == null || project.getStatus().isEmpty()) {
            project.setStatus(StatusConstants.STATUS_UPCOMING);
        }
        projectDAO.addProject(project);
    }

    @Override
    public void updateProject(Project project) {
        projectDAO.updateProject(project);
    }

    @Override
    public void deleteProject(int projectId, int builderId) {
        projectDAO.deleteProject(projectId, builderId);
    }

    @Override
    public List<Project> listProjectsByBuilder(int builderId) {
        return projectDAO.findProjectsByBuilder(builderId);
    }

    @Override
    public List<Project> listProjectsByClient(int clientId) {
        return projectDAO.findProjectsByClient(clientId);
    }

    @Override
    public List<Project> listAllProjects() {
        return projectDAO.findAllProjects();
    }

    @Override
    public Project getProject(int projectId) {
        return projectDAO.findById(projectId);
    }

    @Override
    public BudgetReport buildBudgetReport(Project project) {
        BudgetReport report = new BudgetReport();
        report.setPlanned(project.getBudgetPlanned());
        report.setActual(project.getBudgetUsed());
        report.setVariance(BudgetUtil.calculateVariance(project));
        report.setHealth(BudgetUtil.determineBudgetHealth(project));
        return report;
    }
}
