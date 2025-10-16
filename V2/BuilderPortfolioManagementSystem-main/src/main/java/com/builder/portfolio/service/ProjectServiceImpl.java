package com.builder.portfolio.service;

import com.builder.portfolio.dao.DocumentDAO;
import com.builder.portfolio.dao.DocumentDAOImpl;
import com.builder.portfolio.dao.ProjectDAO;
import com.builder.portfolio.dao.ProjectDAOImpl;
import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Document;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.model.ProjectSummary;
import com.builder.portfolio.util.BackgroundTaskManager;
import com.builder.portfolio.util.BudgetUtil;
import com.builder.portfolio.util.LockRegistry;
import com.builder.portfolio.util.ProjectCache;
import com.builder.portfolio.util.StatusConstants;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Business logic for project management.
// The service layer keeps validation and derived calculations away from the controllers.

public class ProjectServiceImpl implements ProjectService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectServiceImpl.class);

    private final ProjectDAO projectDAO;
    private final DocumentDAO documentDAO;
    private final LockRegistry lockRegistry;
    private final ProjectCache projectCache;

    public ProjectServiceImpl() {
        this(new ProjectDAOImpl(), new DocumentDAOImpl());
    }

    public ProjectServiceImpl(ProjectDAO projectDAO) {
        this(projectDAO, new DocumentDAOImpl());
    }

    public ProjectServiceImpl(ProjectDAO projectDAO, DocumentDAO documentDAO) {
        this.projectDAO = Objects.requireNonNull(projectDAO, "projectDAO");
        this.documentDAO = Objects.requireNonNull(documentDAO, "documentDAO");
        this.lockRegistry = LockRegistry.getInstance();
        this.projectCache = ProjectCache.getInstance();
        // Keep the shared pools warmed so async report jobs and demos do not spin up threads on demand.
        BackgroundTaskManager.getInstance(); // ensure pools are initialised for downstream async operations
    }

    @Override
    public void addProject(Project project) {
        Objects.requireNonNull(project, "project");
        if (project.getStatus() == null || project.getStatus().isEmpty()) {
            project.setStatus(StatusConstants.STATUS_UPCOMING);
        }
        project.setVersion(Math.max(0, project.getVersion()));
        LOGGER.info("Creating project {}", project.getName());
        projectDAO.addProject(project);
        projectCache.update(project);
    }

    @Override
    public void updateProject(Project project) {
        Objects.requireNonNull(project, "project");
        LOGGER.info("Updating project id {} without optimistic guard", project.getId());
        projectDAO.updateProject(project);
        projectCache.update(project);
    }

    @Override
    public void deleteProject(int projectId, int builderId) {
        LOGGER.info("Deleting project id {} for builder {}", projectId, builderId);
        projectDAO.deleteProject(projectId, builderId);
        projectCache.evict(projectId);
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
        return lockRegistry.withProjectRead(projectId, () -> projectDAO.findById(projectId));
    }

    @Override
    public BudgetReport buildBudgetReport(Project project) {
        BudgetReport report = new BudgetReport();
        report.setPlanned(project.getBudgetPlanned());
        report.setActual(project.getBudgetUsed());
        report.setVariance(BudgetUtil.calculateVariance(project));
        report.setHealth(BudgetUtil.determineBudgetHealth(project));
        LOGGER.debug("Built budget report for project id {}", project.getId());
        return report;
    }

    @Override
    public Project updateProjectStatus(long projectId, String newStatus, long expectedVersion) {
        Objects.requireNonNull(newStatus, "newStatus");
        return lockRegistry.withProjectWrite(projectId, () -> {
            long start = System.nanoTime();
            Project project = requireProjectForUpdate(projectId);
            verifyVersion(project, expectedVersion);
            project.setStatus(newStatus);
            if (!projectDAO.conditionalUpdateProject(project, expectedVersion)) {
                throw new ConcurrentModificationException("Project version mismatch for status update");
            }
            projectCache.update(project);
            LOGGER.info("Status update for project {} completed in {} ms (version {})",
                    projectId, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), project.getVersion());
            return project;
        });
    }

    @Override
    public Project updateProjectBudget(long projectId, double delta, long expectedVersion) {
        return lockRegistry.withProjectWrite(projectId, () -> {
            long start = System.nanoTime();
            Project project = requireProjectForUpdate(projectId);
            verifyVersion(project, expectedVersion);
            double newBudget = project.getBudgetUsed() + delta;
            project.setBudgetUsed(newBudget);
            if (!projectDAO.conditionalUpdateProject(project, expectedVersion)) {
                throw new ConcurrentModificationException("Project version mismatch for budget update");
            }
            projectCache.update(project);
            LOGGER.info("Budget update for project {} delta {} new total {} in {} ms (version {})",
                    projectId, delta, newBudget,
                    TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start), project.getVersion());
            return project;
        });
    }

    @Override
    public void uploadDocument(long projectId, Document document) {
        Objects.requireNonNull(document, "document");
        lockRegistry.withProjectWrite(projectId, () -> requireProjectForUpdate(projectId));
        document.setProjectId((int) projectId);
        long start = System.nanoTime();
        documentDAO.addDocument(document);
        LOGGER.info("Document {} uploaded for project {} by user {} in {} ms",
                document.getDocumentName(), projectId, document.getUploadedBy(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start));
    }

    @Override
    public ProjectSummary getProjectSummary(long projectId) {
        return projectCache.getOrCompute(projectId, id -> lockRegistry.withProjectRead(id,
                () -> {
                    Project project = projectDAO.findById((int) id.longValue());
                    if (project == null) {
                        throw new IllegalArgumentException("Project " + id + " not found");
                    }
                    return toSummary(project);
                }));
    }

    private Project requireProjectForUpdate(long projectId) {
        Project project = projectDAO.findByIdForUpdate(projectId);
        if (project == null) {
            throw new IllegalArgumentException("Project " + projectId + " not found");
        }
        return project;
    }

    private void verifyVersion(Project project, long expectedVersion) {
        if (project.getVersion() != expectedVersion) {
            throw new ConcurrentModificationException(
                    "Expected version " + expectedVersion + " but found " + project.getVersion());
        }
    }

    private ProjectSummary toSummary(Project project) {
        return ProjectSummary.builder()
                .projectId(project.getId())
                .name(project.getName())
                .status(project.getStatus())
                .plannedBudget(project.getBudgetPlanned())
                .usedBudget(project.getBudgetUsed())
                .version(project.getVersion())
                .build();
    }
}
