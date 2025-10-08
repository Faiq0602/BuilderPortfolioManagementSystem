package com.builder.portfolio.controller;

import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.ProjectService;

import java.util.List;
import java.util.Scanner;

public class ClientController {
    private final ProjectService projectService;
    private final int clientId;

    public ClientController(ProjectService projectService, int clientId) {
        this.projectService = projectService;
        this.clientId = clientId;
    }

    public void showMenu() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- Client Menu ---");
            System.out.println("1. View My Projects");
            System.out.println("2. Logout");
            System.out.print("Choose an option: ");
            int choice = Integer.parseInt(scanner.nextLine());

            switch (choice) {
                case 1 -> viewProjects();
                case 2 -> exit = true;
                default -> System.out.println("Invalid option.");
            }
        }
    }

    private void viewProjects() {
        List<Project> projects = projectService.listProjectsByClient(clientId);
        if (projects.isEmpty()) {
            System.out.println("No projects assigned.");
            return;
        }

        System.out.println("--- My Projects ---");
        for (Project project : projects) {
            System.out.println(project.getId() + ": " + project.getName() + " | " + project.getStatus());
        }
    }
}
