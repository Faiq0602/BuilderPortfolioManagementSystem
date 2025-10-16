package com.builder.portfolio.controller;

import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Document;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.DocumentService;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.util.BackgroundTaskManager;
import com.builder.portfolio.util.ConsoleInput;
import com.builder.portfolio.util.GanttChartUtil;
import com.builder.portfolio.util.StatusConstants;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.ConcurrentModificationException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

// Handles actions available to builders such as managing projects and uploading document metadata.
// Menu interactions are delegated to {@link ConsoleInput} to keep the user experience consistent.

public class BuilderController {
    private static final Logger LOGGER = Logger.getLogger(BuilderController.class.getName());
    private static final double BUDGET_DELTA_EPSILON = 0.0001;

    private final ProjectService projectService;
    private final DocumentService documentService;
    private final int builderId;

    public BuilderController(ProjectService projectService, DocumentService documentService, int builderId) {
        this.projectService = projectService;
        this.documentService = documentService;
        this.builderId = builderId;
    }

    public void showMenu() {
        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- Builder Menu ---");
            System.out.println("1. Add Project");
            System.out.println("2. Update Project");
            System.out.println("3. Delete Project");
            System.out.println("4. View My Projects");
            System.out.println("5. Add Document Metadata");
            System.out.println("6. View Document Metadata");
            System.out.println("7. View Budget Report");
            System.out.println("8. Run Concurrency Demo");
            System.out.println("9. Logout");

            int choice = ConsoleInput.readInt("Choose an option: ");

            switch (choice) {
                case 1 -> addProject();
                case 2 -> updateProject();
                case 3 -> deleteProject();
                case 4 -> viewProjects();
                case 5 -> addDocumentMetadata();
                case 6 -> viewDocumentMetadata();
                case 7 -> viewBudgetReport();
                case 8 -> runConcurrencyDemo();
                case 9 -> exit = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void addProject() {
        Project project = new Project();
        project.setName(ConsoleInput.readLine("Project name: "));
        project.setDescription(ConsoleInput.readLine("Description: "));
        project.setStatus(ConsoleInput.readLine("Status (UPCOMING/IN_PROGRESS/COMPLETED): "));
        project.setBuilderId(builderId);

        project.setClientId(ConsoleInput.readInt("Client ID: "));
        project.setBudgetPlanned(ConsoleInput.readDouble("Planned budget: "));
        project.setBudgetUsed(ConsoleInput.readDouble("Budget used: "));

        project.setStartDate(readDate("Start date (YYYY-MM-DD or blank): "));
        project.setEndDate(readDate("End date (YYYY-MM-DD or blank): "));

        projectService.addProject(project);
        LOGGER.log(Level.INFO, "Builder {0} added project {1}", new Object[]{builderId, project.getName()});
        System.out.println("Project added.");
    }

    private void updateProject() {
        int projectId = ConsoleInput.readInt("Enter project ID: ");
        Project existing = projectService.getProject(projectId);
        if (existing == null || existing.getBuilderId() != builderId) {
            LOGGER.log(Level.WARNING, "Builder {0} attempted to update unauthorized project {1}",
                    new Object[]{builderId, projectId});
            System.out.println("Project not found or access denied.");
            return;
        }

        // Capture the original state so we can route only the mutated fields through the right service calls.
        String originalName = existing.getName();
        String originalDescription = existing.getDescription();
        String originalStatus = existing.getStatus();
        int originalClientId = existing.getClientId();
        double originalBudgetPlanned = existing.getBudgetPlanned();
        double originalBudgetUsed = existing.getBudgetUsed();
        LocalDate originalStartDate = existing.getStartDate();
        LocalDate originalEndDate = existing.getEndDate();
        long versionSnapshot = existing.getVersion();

        String name = ConsoleInput.readLine("Project name (" + existing.getName() + "): ");
        if (!name.isEmpty()) existing.setName(name);

        String description = ConsoleInput.readLine("Description (" + existing.getDescription() + "): ");
        if (!description.isEmpty()) existing.setDescription(description);

        String status = ConsoleInput.readLine("Status (" + existing.getStatus() + "): ");
        if (!status.isEmpty()) existing.setStatus(status);

        String clientInput = ConsoleInput.readLine("Client ID (" + existing.getClientId() + "): ");
        if (!clientInput.isEmpty()) {
            try {
                existing.setClientId(Integer.parseInt(clientInput));
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid client id entered: {0}", clientInput);
                System.out.println("Client ID ignored due to invalid number.");
            }
        }

        String plannedInput = ConsoleInput.readLine("Planned budget (" + existing.getBudgetPlanned() + "): ");
        if (!plannedInput.isEmpty()) {
            try {
                existing.setBudgetPlanned(Double.parseDouble(plannedInput));
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid planned budget: {0}", plannedInput);
                System.out.println("Planned budget ignored due to invalid number.");
            }
        }

        String usedInput = ConsoleInput.readLine("Budget used (" + existing.getBudgetUsed() + "): ");
        if (!usedInput.isEmpty()) {
            try {
                existing.setBudgetUsed(Double.parseDouble(usedInput));
            } catch (NumberFormatException ex) {
                LOGGER.log(Level.WARNING, "Invalid budget used: {0}", usedInput);
                System.out.println("Budget used ignored due to invalid number.");
            }
        }

        LocalDate startDate = readDate("Start date (" + formatDate(existing.getStartDate()) + "): ");
        LocalDate endDate = readDate("End date (" + formatDate(existing.getEndDate()) + "): ");
        if (startDate != null) existing.setStartDate(startDate);
        if (endDate != null) existing.setEndDate(endDate);

        existing.setBuilderId(builderId);

        long latestVersion = versionSnapshot;
        // Status and budget are guarded by locks + optimistic versioning; delegate to those APIs first.
        try {
            if (!Objects.equals(originalStatus, existing.getStatus())) {
                Project updated = projectService.updateProjectStatus(projectId, existing.getStatus(), latestVersion);
                latestVersion = updated.getVersion();
                existing.setVersion(latestVersion);
            }

            if (Math.abs(originalBudgetUsed - existing.getBudgetUsed()) > BUDGET_DELTA_EPSILON) {
                double delta = existing.getBudgetUsed() - originalBudgetUsed;
                Project updated = projectService.updateProjectBudget(projectId, delta, latestVersion);
                latestVersion = updated.getVersion();
                existing.setVersion(latestVersion);
                existing.setBudgetUsed(updated.getBudgetUsed());
            }
        } catch (ConcurrentModificationException ex) {
            LOGGER.log(Level.WARNING, "Concurrent update detected for project {0}", new Object[]{projectId});
            System.out.println("Project was updated by someone else. Please reopen the update menu and try again.");
            return;
        }

        boolean otherFieldsChanged =
                !Objects.equals(originalName, existing.getName())
                        || !Objects.equals(originalDescription, existing.getDescription())
                        || originalClientId != existing.getClientId()
                        || Math.abs(originalBudgetPlanned - existing.getBudgetPlanned()) > BUDGET_DELTA_EPSILON
                        || !Objects.equals(originalStartDate, existing.getStartDate())
                        || !Objects.equals(originalEndDate, existing.getEndDate());

        if (otherFieldsChanged) {
            existing.setVersion(latestVersion);
            projectService.updateProject(existing);
        }

        LOGGER.log(Level.INFO, "Builder {0} updated project {1}", new Object[]{builderId, projectId});
        System.out.println("Project updated.");
    }

    private void deleteProject() {
        int projectId = ConsoleInput.readInt("Enter project ID to delete: ");
        projectService.deleteProject(projectId, builderId);
        LOGGER.log(Level.INFO, "Builder {0} requested deletion for project {1}",
                new Object[]{builderId, projectId});
        System.out.println("Project removed if it belonged to you.");
    }

    private void viewProjects() {
        List<Project> projects = projectService.listProjectsByBuilder(builderId);
        if (projects.isEmpty()) {
            System.out.println("No projects yet.");
            return;
        }
        for (Project project : projects) {
            printProject(project);
        }
    }

    private void addDocumentMetadata() {
        int projectId = ConsoleInput.readInt("Project ID: ");
        Project project = projectService.getProject(projectId);
        if (project == null || project.getBuilderId() != builderId) {
            LOGGER.log(Level.WARNING, "Builder {0} attempted to add document to unauthorized project {1}",
                    new Object[]{builderId, projectId});
            System.out.println("Project not found or access denied.");
            return;
        }

        Document document = new Document();
        document.setProjectId(projectId);
        document.setDocumentName(ConsoleInput.readLine("Document name: "));
        document.setDocumentType(ConsoleInput.readLine("Document type: "));
        document.setUploadedBy(builderId);
        document.setUploadDate(readDate("Upload date (YYYY-MM-DD or blank): "));

        // Push through the project service so the write lock and timing instrumentation stay in play.
        projectService.uploadDocument(projectId, document);
        LOGGER.log(Level.INFO, "Builder {0} stored metadata for document {1}",
                new Object[]{builderId, document.getDocumentName()});
        System.out.println("Document metadata stored.");
    }

    private void viewDocumentMetadata() {
        int projectId = ConsoleInput.readInt("Project ID: ");
        Project project = projectService.getProject(projectId);
        if (project == null || project.getBuilderId() != builderId) {
            LOGGER.log(Level.WARNING, "Builder {0} attempted to view documents for unauthorized project {1}",
                    new Object[]{builderId, projectId});
            System.out.println("Project not found or access denied.");
            return;
        }

        List<Document> documents = documentService.listDocumentsByProject(projectId);
        if (documents.isEmpty()) {
            System.out.println("No document metadata recorded for this project.");
            return;
        }

        LOGGER.log(Level.INFO, "Builder {0} viewed documents for project {1}",
                new Object[]{builderId, projectId});
        for (Document document : documents) {
            printDocument(document);
        }
    }

    private void runConcurrencyDemo() {
        System.out.println("--- Concurrency Demo ---");
        System.out.println("Queuing background tasks... watch thread names to verify parallel work.");

        BackgroundTaskManager taskManager = BackgroundTaskManager.getInstance();
        CompletableFuture<Void> cacheWarmup = taskManager.runAsync(() -> simulateWork("Cache warm-up", 1200));
        CompletableFuture<Void> varianceSweep = taskManager.runAsync(() -> simulateWork("Budget variance sweep", 900));
        CompletableFuture<Void> documentSync = taskManager.runAsync(() -> simulateWork("Document sync heartbeat", 1500));

        CompletableFuture.allOf(cacheWarmup, varianceSweep, documentSync).join();
        System.out.println("All background tasks finished. Concurrency systems are operational.");
    }

    private void viewBudgetReport() {
        int projectId = ConsoleInput.readInt("Project ID: ");
        Project project = projectService.getProject(projectId);
        if (project == null || project.getBuilderId() != builderId) {
            LOGGER.log(Level.WARNING, "Builder {0} attempted to view budget for unauthorized project {1}",
                    new Object[]{builderId, projectId});
            System.out.println("Project not found or access denied.");
            return;
        }

        BudgetReport report = projectService.buildBudgetReport(project);
        System.out.println("--- Budget Report ---");
        System.out.println("Planned: " + report.getPlanned());
        System.out.println("Actual: " + report.getActual());
        System.out.println("Difference: " + report.getVariance());
        System.out.println("Health: " + report.getHealth());

        if (StatusConstants.STATUS_IN_PROGRESS.equals(project.getStatus()) ||
                StatusConstants.STATUS_COMPLETED.equals(project.getStatus())) {
            GanttChartUtil.printSimpleGantt(project);
        }
    }

    private LocalDate readDate(String prompt) {
        while (true) {
            String input = ConsoleInput.readLine(prompt);
            if (input == null || input.isBlank()) {
                return null;
            }
            try {
                return LocalDate.parse(input.trim());
            } catch (DateTimeParseException ex) {
                LOGGER.log(Level.WARNING, "Invalid date format entered: {0}", input);
                System.out.println("Invalid date format. Please try again or leave blank.");
            }
        }
    }

    private String formatDate(LocalDate date) {

        return date == null ? "none" : date.toString();

    }

    private void printProject(Project project) {
        System.out.println("------------------------------");
        System.out.println("ID: " + project.getId());
        System.out.println("Name: " + project.getName());
        System.out.println("Description: " + project.getDescription());
        System.out.println("Status: " + project.getStatus());
        System.out.println("Client ID: " + project.getClientId());
        System.out.println("Planned Budget: " + project.getBudgetPlanned());
        System.out.println("Budget Used: " + project.getBudgetUsed());
        System.out.println("Start Date: " + formatDate(project.getStartDate()));
        System.out.println("End Date: " + formatDate(project.getEndDate()));
    }

    private void printDocument(Document document) {
        System.out.println("------------------------------");
        System.out.println("Document ID: " + document.getId());
        System.out.println("Name: " + document.getDocumentName());
        System.out.println("Type: " + document.getDocumentType());
        System.out.println("Uploaded By (User ID): " + document.getUploadedBy());
        System.out.println("Upload Date: " + formatDate(document.getUploadDate()));
    }

    private void simulateWork(String taskName, long durationMs) {
        String threadName = Thread.currentThread().getName();
        System.out.println("[" + threadName + "] " + taskName + " started.");
        try {
            Thread.sleep(durationMs);
            System.out.println("[" + threadName + "] " + taskName + " completed in ~" + durationMs + " ms.");
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            System.out.println("[" + threadName + "] " + taskName + " interrupted.");
        }
    }
}
