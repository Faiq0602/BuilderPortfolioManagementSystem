package com.builder.portfolio.controller;

import com.builder.portfolio.model.BudgetReport;
import com.builder.portfolio.model.Document;
import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.DocumentService;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.util.GanttChartUtil;
import com.builder.portfolio.util.StatusConstants;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class BuilderController {
    private final ProjectService projectService;
    private final DocumentService documentService;
    private final int builderId;

    public BuilderController(ProjectService projectService, DocumentService documentService, int builderId) {
        this.projectService = projectService;
        this.documentService = documentService;
        this.builderId = builderId;
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);


        boolean exit = false;
        while (!exit) {
            System.out.println("\n--- Builder Menu ---");
            System.out.println("1. Add Project");
            System.out.println("2. Update Project");
            System.out.println("3. Delete Project");
            System.out.println("4. View My Projects");
            System.out.println("5. Add Document Metadata");
            System.out.println("6. View Budget Report");
            System.out.println("7. Logout");
            System.out.print("Choose an option: ");

            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> addProject(scanner);
                case 2 -> updateProject(scanner);
                case 3 -> deleteProject(scanner);
                case 4 -> viewProjects();
                case 5 -> addDocumentMetadata(scanner);
                case 6 -> viewBudgetReport(scanner);
                case 7 -> exit = true;
                default -> System.out.println("Invalid option.");
            }
        }

    }

    private void addProject(Scanner scanner) {
        Project project = new Project();
        System.out.print("Project name: ");
        project.setName(scanner.nextLine());
        System.out.print("Description: ");
        project.setDescription(scanner.nextLine());
        System.out.print("Status (UPCOMING/IN_PROGRESS/COMPLETED): ");
        project.setStatus(scanner.nextLine());
        project.setBuilderId(builderId);

        System.out.print("Client ID: ");
        project.setClientId(Integer.parseInt(scanner.nextLine()));
        System.out.print("Planned budget: ");
        project.setBudgetPlanned(Double.parseDouble(scanner.nextLine()));
        System.out.print("Budget used: ");
        project.setBudgetUsed(Double.parseDouble(scanner.nextLine()));

        project.setStartDate(readDate(scanner, "Start date (YYYY-MM-DD or blank): "));
        project.setEndDate(readDate(scanner, "End date (YYYY-MM-DD or blank): "));

        projectService.addProject(project);
        System.out.println("Project added.");
    }

    private void updateProject(Scanner scanner) {
        System.out.print("Enter project ID: ");
        int projectId = Integer.parseInt(scanner.nextLine());
        Project existing = projectService.getProject(projectId);
        if (existing == null || existing.getBuilderId() != builderId) {
            System.out.println("Project not found or access denied.");
            return;
        }

        System.out.print("Project name (" + existing.getName() + "): ");
        String name = scanner.nextLine();
        if (!name.isEmpty()) existing.setName(name);

        System.out.print("Description (" + existing.getDescription() + "): ");
        String description = scanner.nextLine();
        if (!description.isEmpty()) existing.setDescription(description);

        System.out.print("Status (" + existing.getStatus() + "): ");
        String status = scanner.nextLine();
        if (!status.isEmpty()) existing.setStatus(status);

        System.out.print("Client ID (" + existing.getClientId() + "): ");
        String clientInput = scanner.nextLine();
        if (!clientInput.isEmpty()) existing.setClientId(Integer.parseInt(clientInput));

        System.out.print("Planned budget (" + existing.getBudgetPlanned() + "): ");
        String plannedInput = scanner.nextLine();
        if (!plannedInput.isEmpty()) existing.setBudgetPlanned(Double.parseDouble(plannedInput));

        System.out.print("Budget used (" + existing.getBudgetUsed() + "): ");
        String usedInput = scanner.nextLine();
        if (!usedInput.isEmpty()) existing.setBudgetUsed(Double.parseDouble(usedInput));

        LocalDate startDate = readDate(scanner, "Start date (" + formatDate(existing.getStartDate()) + "): ");
        LocalDate endDate = readDate(scanner, "End date (" + formatDate(existing.getEndDate()) + "): ");
        if (startDate != null) existing.setStartDate(startDate);
        if (endDate != null) existing.setEndDate(endDate);

        existing.setBuilderId(builderId);
        projectService.updateProject(existing);
        System.out.println("Project updated.");
    }

    private void deleteProject(Scanner scanner) {
        System.out.print("Enter project ID to delete: ");
        int projectId = Integer.parseInt(scanner.nextLine());
        projectService.deleteProject(projectId, builderId);
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

    private void addDocumentMetadata(Scanner scanner) {
        System.out.print("Project ID: ");
        int projectId = Integer.parseInt(scanner.nextLine());
        Project project = projectService.getProject(projectId);
        if (project == null || project.getBuilderId() != builderId) {
            System.out.println("Project not found or access denied.");
            return;
        }

        Document document = new Document();
        document.setProjectId(projectId);
        System.out.print("Document name: ");
        document.setDocumentName(scanner.nextLine());
        System.out.print("Document type: ");
        document.setDocumentType(scanner.nextLine());
        document.setUploadedBy(builderId);
        document.setUploadDate(readDate(scanner, "Upload date (YYYY-MM-DD or blank): "));

        documentService.addDocument(document);
        System.out.println("Document metadata stored.");
    }

    private void viewBudgetReport(Scanner scanner) {
        System.out.print("Project ID: ");
        int projectId = Integer.parseInt(scanner.nextLine());
        Project project = projectService.getProject(projectId);
        if (project == null || project.getBuilderId() != builderId) {
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

    private LocalDate readDate(Scanner scanner, String prompt) {
        System.out.print(prompt);
        String input = scanner.nextLine();
        if (input.isEmpty()) return null;
        try {
            return LocalDate.parse(input);
        } catch (DateTimeParseException ex) {
            System.out.println("Invalid date format. Ignored.");
            return null;
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
}
