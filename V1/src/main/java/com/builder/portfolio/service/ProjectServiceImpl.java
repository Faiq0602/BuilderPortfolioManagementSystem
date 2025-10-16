package com.builder.portfolio.service;

import com.builder.portfolio.dao.ProjectDAO;
import com.builder.portfolio.dao.ProjectDAOImpl;
import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.util.BudgetUtil;
import com.builder.portfolio.util.StatusConstants;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Business logic for project management.
// The service layer keeps validation and derived calculations away from the controllers.

public class ProjectServiceImpl implements ProjectService {
    private static final Logger LOGGER = Logger.getLogger(ProjectServiceImpl.class.getName());

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
        LOGGER.log(Level.FINE, "Creating project {0}", project.getName());
        projectDAO.addProject(project);
    }

    @Override
    public void updateProject(Project project) {
        LOGGER.log(Level.FINE, "Updating project id {0}", project.getId());
        projectDAO.updateProject(project);
    }

    @Override
    public void deleteProject(int projectId, int builderId) {
        LOGGER.log(Level.FINE, "Deleting project id {0} for builder {1}", new Object[]{projectId, builderId});
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
        LOGGER.log(Level.FINE, "Built budget report for project id {0}", project.getId());
        return report;
    }
}
