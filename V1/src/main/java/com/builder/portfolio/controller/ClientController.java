package com.builder.portfolio.controller;

import com.builder.portfolio.model.Project;
import com.builder.portfolio.service.ProjectService;
import com.builder.portfolio.util.ConsoleInput;

import java.awt.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

// Menu shown to clients, allowing them to see their assigned projects.
// Using ConsoleInput keeps the input handling consistent with the other controllers.

public class ClientController {
    private static final Logger LOGGER = Logger.getLogger(ClientController.class.getName());

    private final ProjectService projectService;
    private final int clientId;

    public ClientController(ProjectService projectService, int clientId) {
        this.projectService = projectService;
        this.clientId = clientId;
    }

    public void showMenu() {
        boolean exit = false;

        while (!exit) {
            System.out.println("\n--- Client Menu ---");
            System.out.println("1. View My Projects");
            System.out.println("2. Logout");
            int choice = ConsoleInput.readInt("Choose an option: ");

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
            LOGGER.log(Level.INFO, "Client {0} has no projects to display", clientId);
            System.out.println("No projects assigned.");
            return;
        }

        System.out.println("--- My Projects ---");
        for (Project project : projects) {
            System.out.println(project.getId() + ": " + project.getName() + " | " + project.getStatus());
        }
    }
}
